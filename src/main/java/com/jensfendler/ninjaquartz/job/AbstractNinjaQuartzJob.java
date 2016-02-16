/*
 * Copyright 2016 Fendler Consulting cc.
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
public abstract class AbstractNinjaQuartzJob implements Job {

    protected static final Logger LOG = LoggerFactory.getLogger(NonConcurrentNinjaQuartzJob.class);

    /**
     * The key name to use in the {@link JobDataMap} of a {@link JobDetail} when
     * setting task wrapping the scheduled method invocation.
     */
    public static final String JOB_TASK_KEY = "nqTask";

    /**
     * 
     */
    public AbstractNinjaQuartzJob() {
    }

    /**
     * @see org.quartz.Job#execute(org.quartz.JobExecutionContext)
     */
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        NinjaQuartzTask task = (NinjaQuartzTask) context.getJobDetail().getJobDataMap().get(JOB_TASK_KEY);
        if (task == null) {
            LOG.error(
                    "JobTask object for task {} is null. Nothing to do in this Quartz Job, so it will be removed from the schedule.");
            removeSelf("NULL-TASK", context);
            return;
        }

        // ensure we have a task name to use.
        String taskName = task.getTaskName();
        if (taskName == null) {
            taskName = task.toString();
        }

        try {

            LOG.debug("Executing Ninja Quartz task {}{}.", taskName, context.getJobDetail().getDescription() != null
                    ? " (" + context.getJobDetail().getDescription() : ")");

            // invokd the scheduled method
            task.execute(context);

            LOG.debug("Ninja Quartz task{} execution finished. Next fire time will be: {}", taskName,
                    context.getNextFireTime().toString());

        } catch (IllegalAccessException e) {
            LOG.error("Illegal access exception while trying to execute task " + taskName + ".", e);
            removeSelf(taskName, context);

        } catch (IllegalArgumentException e) {
            LOG.error("Illegal argument exception while trying to execute task " + taskName
                    + ". Your scheduled method should not require any parameters!", e);
            removeSelf(taskName, context);

        } catch (InvocationTargetException e) {
            LOG.error("Invocation target exception while trying to execute task " + taskName + ".", e);
            removeSelf(taskName, context);

        } catch (Throwable t) {
            // fallback for any other problem in the scheduled method
            LOG.error("Exception during execution of quartz task " + taskName + ".", t);
            // TODO use annotation parameter to determine if we should cancel
            // ourselves here in the event of any thrown exception
            removeSelf(taskName, context);

        }
    }

    /**
     * @param context
     */
    private void removeSelf(String taskName, JobExecutionContext context) {
        JobKey key = context.getJobDetail().getKey();
        LOG.error("Removing task {} ({}) from scheduler to avoid repeated errors.", taskName, key.getName());
        try {
            context.getScheduler().deleteJob(key);
        } catch (SchedulerException e) {
            LOG.error(
                    "Failed to cancel quartz task {} (job {}) with null JobTask. You are likely to see this message again.",
                    taskName, key);
        }
    }

}
