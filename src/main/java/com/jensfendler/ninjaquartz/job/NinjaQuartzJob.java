/*
 * Copyright 2015 Fendler Consulting cc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jensfendler.ninjaquartz.job;

import java.lang.reflect.InvocationTargetException;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Jens Fendler <jf@jensfendler.com>
 *
 */
public class NinjaQuartzJob implements Job {

    public static final Logger LOG = LoggerFactory.getLogger(NinjaQuartzJob.class);

    /**
     * The key name to use in the {@link JobDataMap} of a {@link JobDetail} when
     * setting task wrapping the scheduled method invocation.
     */
    public static final String JOB_TASK_KEY = "nqJobTask";

    /**
     * 
     */
    public NinjaQuartzJob() {
    }

    /**
     * @see org.quartz.Job#execute(org.quartz.JobExecutionContext)
     */
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        NinjaQuartzTask jobTask = (NinjaQuartzTask) context.getJobDetail().getJobDataMap().get(JOB_TASK_KEY);
        if (jobTask == null) {
            LOG.error("JobTask is null. Nothing to do in this Quartz Job, so it will be removed from the schedule.");
            removeSelf(context);
            return;
        }

        try {
            LOG.debug("Executing Ninja Quartz task{}.", context.getJobDetail().getDescription() != null
                    ? " " + context.getJobDetail().getDescription() : "");
            jobTask.execute(context);
            LOG.debug("Ninja Quartz task{} execution finished. Next fire time will be: {}",
                    context.getJobDetail().getDescription() != null ? " " + context.getJobDetail().getDescription()
                            : "",
                    context.getNextFireTime().toString());
        } catch (IllegalAccessException e) {
            LOG.error("Illegal access exception while trying to execute JobTask.", e);
            removeSelf(context);
        } catch (IllegalArgumentException e) {
            LOG.error(
                    "Illegal argument exception while trying to execute JobTask. Your scheduled method should not require any parameters!",
                    e);
            removeSelf(context);
        } catch (InvocationTargetException e) {
            LOG.error("Invocation target exception while trying to execute JobTask.", e);
            removeSelf(context);
        } catch (Throwable t) {
            // fallback for any other problem in the scheduled method
            LOG.error("Exception during Quartz Job execution.", t);
            // TODO use annotation parameter to determine if we should cancel
            // ourselves here in the event of any thrown exception
            removeSelf(context);
        }
    }

    /**
     * @param context
     */
    private void removeSelf(JobExecutionContext context) {
        JobKey key = context.getJobDetail().getKey();
        LOG.error("Removing job {} from scheduler to avoid repeated errors.", key.getName());
        try {
            context.getScheduler().deleteJob(key);
        } catch (SchedulerException e) {
            LOG.error("Failed to cancel quartz job {} with null JobTask. You are likely to see this message again.");
        }
    }

}
