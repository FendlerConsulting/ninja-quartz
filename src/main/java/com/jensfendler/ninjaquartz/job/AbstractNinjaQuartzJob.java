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

import com.jensfendler.ninjaquartz.annotations.QuartzSchedule;

/**
 * @author Jens Fendler
 *
 */
public abstract class AbstractNinjaQuartzJob implements Job {

    protected static final Logger LOG = LoggerFactory.getLogger(NinjaQuartzTask.class);

    /**
     * The key name to use in the {@link JobDataMap} of a {@link JobDetail} when
     * setting task wrapping the scheduled method invocation.
     */
    public static final String JOB_TASK_KEY = "nqTask";

    /**
     * The key name of the boolean property of the job's {@link JobDataMap}
     * indicating if this job should be removed from the scheduler after the
     * calling of the scheduled method has resulted in an exception (caught in a
     * {@link InvocationTargetException}.
     * 
     * The default for this property is <code>false</code> (i.e. the job will be
     * kept after an InvocationTargetException is thrown).
     */
    public static final String JOB_REMOVE_ON_RUNTIME_ERROR = "removeJobAfterInvocationTargetException";

    /**
     * The key name of a boolean property of the job's {@link JobDataMap},
     * indicating if this job should be kept scheduled, despite <em>any</em>
     * exceptions being thrown in the process of executing it.
     * 
     * A <code>true</code> value of this property will also override the
     * {@link #JOB_REMOVE_ON_RUNTIME_ERROR} property's setting.
     * 
     * The default for this property is <code>false</code>.
     */
    public static final String JOB_FORCE_KEEP = "forceKeepingOfJob";

    public AbstractNinjaQuartzJob() {
    }

    /**
     * @see org.quartz.Job#execute(org.quartz.JobExecutionContext)
     */
    public void execute(JobExecutionContext context) throws JobExecutionException {
        // get NinjaQuartz settings from the context
        boolean forceKeepJob = context.getMergedJobDataMap().containsKey(JOB_FORCE_KEEP)
                ? context.getMergedJobDataMap().getBooleanValue(JOB_FORCE_KEEP) : QuartzSchedule.DEFAULT_FORCE_KEEP;
        boolean removeOnInvocationTargetException = context.getMergedJobDataMap()
                .containsKey(JOB_REMOVE_ON_RUNTIME_ERROR)
                        ? context.getMergedJobDataMap().getBooleanValue(JOB_REMOVE_ON_RUNTIME_ERROR)
                        : QuartzSchedule.DEFAULT_REMOVE_ON_ERROR;

        NinjaQuartzTask task = (NinjaQuartzTask) context.getJobDetail().getJobDataMap().get(JOB_TASK_KEY);
        if (task == null) {
            LOG.error(
                    "JobTask object for task {} is null. Nothing to do in this Quartz Job, so it will be removed from the schedule.");
            if (!forceKeepJob) {
                removeSelf("NULL-TASK", context);
            }
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
            if (!forceKeepJob) {
                removeSelf(taskName, context);
            }

        } catch (IllegalArgumentException e) {
            LOG.error("Illegal argument exception while trying to execute task " + taskName
                    + ". Your scheduled method should not require any parameters!", e);
            if (!forceKeepJob) {
                removeSelf(taskName, context);
            }

        } catch (InvocationTargetException e) {
            // check if we should ignore this exception
            if (forceKeepJob || !removeOnInvocationTargetException) {
                // if we ignore it, only log a brief exception message in WARN
                // level and do not remove the job from the scheduler
                LOG.warn("Ignoring InvocationTargetException during execution of {}: {}", taskName, e.getMessage());
            } else {
                // we should not ignore this. log the full exception at ERROR
                // level and remove the job.
                LOG.error(
                        "Removing scheduled job after InvocationTargetException during execution of " + taskName + ".",
                        e);
                removeSelf(taskName, context);
            }
        } catch (Throwable t) {
            // fallback for any other problem in the scheduled method
            LOG.error("Exception during execution of quartz task " + taskName + ".", t);
            if (!forceKeepJob) {
                removeSelf(taskName, context);
            }
        }
    }

    /**
     * @param context
     */
    private void removeSelf(String taskName, JobExecutionContext context) {
        JobKey key = context.getJobDetail().getKey();
        LOG.error(
                "Removing task {} ({}) from scheduler to avoid repeated errors. Use 'forceKeep' parameter to keep this job always.",
                taskName, key.getName());
        try {
            context.getScheduler().deleteJob(key);
        } catch (SchedulerException e) {
            LOG.error(
                    "Failed to cancel quartz task {} (job {}) with null JobTask. You are likely to see this message again.",
                    taskName, key);
        }
    }

}
