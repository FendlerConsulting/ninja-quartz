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
package com.jensfendler.ninjaquartz;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.TriggerBuilder;
import org.slf4j.Logger;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.jensfendler.ninjaquartz.annotations.QuartzSchedule;
import com.jensfendler.ninjaquartz.job.NinjaQuartzTask;
import com.jensfendler.ninjaquartz.job.NinjaQuartzJob;

/**
 * @author Jens Fendler <jf@jensfendler.com>
 *
 */
@Singleton
public class NinjaQuartzScheduleHelper {

    /**
     * The datetime format to use for the {@link QuartzSchedule} annotation's
     * {@link QuartzSchedule# triggerStartAt()} and
     * {@link QuartzSchedule# triggerEndAt()} parameters.
     */
    public static final String TRIGGER_DATETIME_FORMAT = "yyyyMMddHHmmSS";

    /**
     * Name prefix for {@link CronTrigger}s.
     */
    private static final String CRON_TRIGGER_NAME_PREFIX = "nqCT-";

    /**
     * Name prefix for {@link CronTrigger} groups.
     */
    private static final String CRON_TRIGGER_GROUP_PREFIX = "nqCTG-";

    /**
     * Name prefix for {@link JobDetail}s.
     */
    private static final String JOB_NAME_PREFIX = "nqJ-";

    /**
     * Name prefix for {@link JobDetail} groups.
     */
    private static final String JOB_GROUP_PREFIX = "nqJG-";

    @Inject
    private Logger logger;

    @Inject
    private Provider<SchedulerFactory> schedulerFactoryProvider;

    /**
     * Scans all methods of the given object's class for NinjaQuartz scheduler
     * annotations, and schedules these for execution using the given object.
     * 
     * @param method
     */
    public void scheduleTarget(Object target) {
        logger.debug("Scheduling target object of type {}", target.getClass().getName());

        if (target != null) {
            Class<?> clazz = target.getClass();
            for (Method method : clazz.getMethods()) {
                QuartzSchedule quartzSchedule = method.getAnnotation(QuartzSchedule.class);
                if (quartzSchedule != null) {
                    scheduleMethod(target, method, quartzSchedule);
                }
            }
        }
    }

    /**
     * Schedules execution of the given method using the given target instance,
     * based on the given {@link QuartzSchedule}.
     * 
     * @param target
     * @param method
     * @param quartzSchedule
     */
    private void scheduleMethod(Object target, Method method, QuartzSchedule quartzSchedule) {
        logger.debug("Scheduling method {} from class {}...", method.getName(), target.getClass().getName());

        JobDetail jobDetail = createJobDetailToSchedule(target, method, quartzSchedule);
        if (jobDetail == null) {
            logger.error("Could not create Quartz job. Not scheduling {}.{}.", method.getDeclaringClass().getName(),
                    method.getName());
            return;
        }

        CronTrigger cronTrigger = createCronTrigger(method, quartzSchedule);
        if (cronTrigger == null) {
            logger.error("Could not create Quartz trigger. Not scheduling {}.{}.", method.getDeclaringClass().getName(),
                    method.getName());
            return;
        }

        try {
            Scheduler scheduler = createScheduler(method, quartzSchedule);
            scheduler.scheduleJob(jobDetail, cronTrigger);
            logger.info("Scheduled {}.{} with execution schedule {}", method.getDeclaringClass().getName(),
                    method.getName(), quartzSchedule.cronSchedule());
        } catch (SchedulerException e) {
            logger.error("Failed to schedule " + method.getDeclaringClass().getName() + "." + method.getName(), e);
        }
    }

    /**
     * @param target
     * @param method
     * @param quartzSchedule
     * @return
     */
    private JobDetail createJobDetailToSchedule(final Object target, final Method method,
            QuartzSchedule quartzSchedule) {
        // get job parameters from the annotation

        String jobName = quartzSchedule.jobName();
        if (QuartzSchedule.DEFAULT_JOB_NAME.equals(jobName)) {
            // by default, use a unique job name for all scheduled methods
            jobName = JOB_NAME_PREFIX + method.getName() + "-" + System.currentTimeMillis();
        }

        String jobGroup = quartzSchedule.jobGroup();
        if (QuartzSchedule.DEFAULT_JOB_GROUP.equals(jobGroup)) {
            // by default, use the same group name for all scheduled methods
            // within the same declaring class
            jobGroup = JOB_GROUP_PREFIX + method.getDeclaringClass().getName();
        }

        String jobDescription = quartzSchedule.jobDescription();
        if (QuartzSchedule.DEFAULT_JOB_DESCRIPTION.equals(jobDescription)) {
            jobDescription = null;
        }

        boolean jobRecovery = quartzSchedule.jobRecovery();
        boolean jobDurability = quartzSchedule.jobDurability();

        // create the job to execute
        NinjaQuartzTask jobTask = new NinjaQuartzTask() {
            @Override
            public void execute(JobExecutionContext context)
                    throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
                if ((method.getParameterCount() == 1)
                        && (JobExecutionContext.class.isAssignableFrom(method.getParameterTypes()[0]))) {
                    // the scheduled method can receive the context
                    Object[] parameters = new Object[] { context };
                    method.invoke(target, parameters);
                } else {
                    method.invoke(target);
                }
            }
        };
        JobBuilder jobBuilder = JobBuilder.newJob(NinjaQuartzJob.class).withIdentity(jobName, jobGroup)
                .requestRecovery(jobRecovery).storeDurably(jobDurability);
        if (jobDescription != null) {
            jobBuilder = jobBuilder.withDescription(jobDescription);
        }
        JobDetail jobDetail = jobBuilder.build();
        // let the NinjaQuartzJob know which task (wrapping our scheduled
        // method) we want to execute
        jobDetail.getJobDataMap().put(NinjaQuartzJob.JOB_TASK_KEY, jobTask);

        logger.debug("Created new job {} in group: {}.", jobName, jobGroup);
        return jobDetail;
    }

    /**
     * @param quartzSchedule
     * @return
     */
    private CronTrigger createCronTrigger(Method method, QuartzSchedule quartzSchedule) {
        // get trigger parameters from the annotation

        String triggerName = quartzSchedule.triggerName();
        if (QuartzSchedule.DEFAULT_TRIGGER_NAME.equals(triggerName)) {
            // by default, use a unique trigger name for each scheduled method
            triggerName = CRON_TRIGGER_NAME_PREFIX + method.getName() + "-" + System.currentTimeMillis();
        }

        String triggerGroup = quartzSchedule.triggerGroup();
        if (QuartzSchedule.DEFAULT_TRIGGER_GROUP.equals(triggerGroup)) {
            // by default, use the same trigger group name for all methods
            // within the same declaring class.
            triggerName = CRON_TRIGGER_GROUP_PREFIX + method.getDeclaringClass().getName();
        }

        Date startAt = parseTriggerDatetime(quartzSchedule.triggerStartAt(), method);
        Date endAt = parseTriggerDatetime(quartzSchedule.triggerEndAt(), method);
        int triggerPriority = quartzSchedule.triggerPriority();
        String cronSchedule = quartzSchedule.cronSchedule();
        int misfirePolicy = quartzSchedule.cronScheduleMisfirePolicy();

        // build the cron schedule
        CronScheduleBuilder csb = null;
        try {
            csb = CronScheduleBuilder.cronScheduleNonvalidatedExpression(cronSchedule);
        } catch (ParseException e) {
            logger.error("Invalid cron schedule '" + cronSchedule + "' for method "
                    + method.getDeclaringClass().getName() + "." + method.getName(), e);
            return null;
        }
        switch (misfirePolicy) {
        case QuartzSchedule.MISFIRE_POLICY_FIRE_AND_PROCEED:
            csb = csb.withMisfireHandlingInstructionFireAndProceed();
            break;
        case QuartzSchedule.MISFIRE_POLICY_IGNORE:
            csb = csb.withMisfireHandlingInstructionIgnoreMisfires();
            break;
        default:
            csb = csb.withMisfireHandlingInstructionDoNothing();
            break;
        }

        // build the trigger
        TriggerBuilder<CronTrigger> triggerBuilder = TriggerBuilder.newTrigger().withIdentity(triggerName, triggerGroup)
                .withPriority(triggerPriority).withSchedule(csb);
        if (startAt != null) {
            triggerBuilder = triggerBuilder.startAt(startAt);
        }
        if (endAt != null) {
            triggerBuilder = triggerBuilder.endAt(endAt);
        }

        CronTrigger trigger = triggerBuilder.build();

        logger.debug("Created new cron trigger with priority {} and schedule {}", triggerPriority, cronSchedule);
        return trigger;
    }

    /**
     * @param method
     * @param quartzSchedule
     * @return
     * @throws SchedulerException
     */
    private Scheduler createScheduler(Method method, QuartzSchedule quartzSchedule) throws SchedulerException {
        int schedulerDelay = quartzSchedule.schedulerDelay();

        SchedulerFactory sf = schedulerFactoryProvider.get();
        Scheduler scheduler = sf.getScheduler();
        if (schedulerDelay == -1) {
            scheduler.start();
            logger.debug("Created new scheduler of type {}", scheduler.getClass().getName());
        } else {
            scheduler.startDelayed(schedulerDelay);
            logger.debug("Created new scheduler of type {} with initial delay {}", scheduler.getClass().getName(),
                    schedulerDelay);
        }

        return scheduler;
    }

    /**
     * @param datetime
     * @return
     */
    private Date parseTriggerDatetime(String datetime, Method method) {
        if (datetime == null) {
            return null;
        }

        Date d = null;
        SimpleDateFormat sdf = new SimpleDateFormat(TRIGGER_DATETIME_FORMAT);
        try {
            d = sdf.parse(datetime);
            return d;
        } catch (ParseException e) {
            logger.warn(
                    "Invalid datetime format for parameter value '{}' on {}.{}. Expected format is '{}'. "
                            + "Affected Start/End constraint will NOT be used.",
                    datetime, method.getDeclaringClass().getName(), method.getName(), TRIGGER_DATETIME_FORMAT);
            return null;
        }
    }

}
