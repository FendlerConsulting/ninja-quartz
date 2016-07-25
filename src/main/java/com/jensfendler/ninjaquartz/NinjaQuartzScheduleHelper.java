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
import org.quartz.ObjectAlreadyExistsException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.TriggerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.jensfendler.ninjaquartz.annotations.QuartzSchedule;
import com.jensfendler.ninjaquartz.job.NinjaQuartzTask;
import com.jensfendler.ninjaquartz.job.AbstractNinjaQuartzJob;
import com.jensfendler.ninjaquartz.job.AbstractNinjaQuartzTaskImpl;
import com.jensfendler.ninjaquartz.job.ConcurrentNinjaQuartzJob;
import com.jensfendler.ninjaquartz.job.ConcurrentStatefulNinjaQuartzJob;
import com.jensfendler.ninjaquartz.job.NonConcurrentNinjaQuartzJob;
import com.jensfendler.ninjaquartz.job.NonConcurrentStatefulNinjaQuartzJob;

import ninja.utils.NinjaProperties;

/**
 * @author Jens Fendler
 *
 */
@Singleton
public class NinjaQuartzScheduleHelper {

    protected static final Logger logger = LoggerFactory.getLogger(NinjaQuartzModule.class);

    /**
     * The datetime format to use for the {@link QuartzSchedule} annotation's
     * {@link QuartzSchedule#triggerStartAt()} and
     * {@link QuartzSchedule#triggerEndAt()} parameters.
     */
    public static final String TRIGGER_DATETIME_FORMAT = "yyyyMMddHHmmSS";

    /**
     * Name prefix for {@link CronTrigger}s.
     */
    protected static final String CRON_TRIGGER_NAME_PREFIX = "nqCT-";

    /**
     * Name prefix for {@link CronTrigger} groups.
     */
    protected static final String CRON_TRIGGER_GROUP_PREFIX = "nqCTG-";

    /**
     * Name prefix for {@link JobDetail}s.
     */
    protected static final String JOB_NAME_PREFIX = "nqJ-";

    /**
     * Name prefix for {@link JobDetail} groups.
     */
    protected static final String JOB_GROUP_PREFIX = "nqJG-";

    /**
     * The key name of the property in application.conf which may contain the
     * file name of the quartz.properties file to use for initialising the
     * Quartz library.
     */
    protected static final String CONF_KEY_QUARTZ_PROPERTIES = "quartz.properties";

    @Inject
    protected Provider<SchedulerFactory> schedulerFactoryProvider;

    @Inject
    protected NinjaProperties ninjaProperties;

    @Inject
    protected Injector injector;

    /**
     * If false, {@link #initialise()} will be called exactly once to read a
     * user-provided quartz.properties file.
     */
    protected static boolean initialised;

    /**
     * Instantiate the helper class, and require initialisation.
     */
    public NinjaQuartzScheduleHelper() {
        initialised = false;
    }

    /**
     * Initialise the Quartz library before its first use
     */
    private void initialise() {
        String quartzPropertiesFileName = ninjaProperties.get(CONF_KEY_QUARTZ_PROPERTIES);
        if (quartzPropertiesFileName != null) {
            logger.info("Initialising Quartz library using {}", quartzPropertiesFileName);
            System.setProperty("org.quartz.properties", quartzPropertiesFileName);
        } else {
            // initialise with default values
            logger.info(
                    "Initialising Quartz library with default properties. Set '{}' in application.conf for custom properties.",
                    CONF_KEY_QUARTZ_PROPERTIES);
            System.setProperty("org.quartz.scheduler.instanceName", "NinjaQuartz");
        }

        initialised = true;
    }

    /**
     * Scans all methods of the given object's class for NinjaQuartz scheduler
     * annotations, and schedules these for execution using the given object.
     * 
     * @param target
     *            the target instance (of the class containing the scheduled
     *            method)
     */
    public void scheduleTarget(Object target) {
        if (!initialised) {
            initialise();
        }

        logger.debug("Scheduling target object of type {}", target.getClass().getName());

        Class<?> clazz = target.getClass();
        for (Method method : clazz.getMethods()) {
            if (method.isAnnotationPresent(QuartzSchedule.class)) {
                QuartzSchedule quartzSchedule = method.getAnnotation(QuartzSchedule.class);
                scheduleMethod(target, method, quartzSchedule);
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
            logger.info("Scheduled {}::{} with cron schedule '{}'", method.getDeclaringClass().getName(),
                    method.getName(), cronTrigger.getCronExpression());
        } catch (SchedulerException e) {
            if (e instanceof ObjectAlreadyExistsException) {
                // for some reason we're trying to schedule the same method
                // multiple times. not to worry - unless the task name/group is
                // the same
                logger.debug("Not scheduling " + method.getDeclaringClass().getName() + "." + method.getName()
                        + " twice: {}", e.getMessage());
            } else {
                logger.error("Failed to schedule " + method.getDeclaringClass().getName() + "." + method.getName(), e);
            }
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
            jobName = JOB_NAME_PREFIX + method.getName();
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
        boolean allowParallelInvocations = quartzSchedule.allowConcurrent();
        boolean persistent = quartzSchedule.persistent();

        // create the job to execute
        NinjaQuartzTask task = new AbstractNinjaQuartzTaskImpl(jobName + "/" + jobGroup) {
            @Override
            public void execute(JobExecutionContext context)
                    throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
                if (method.getParameterTypes().length == 0) {
                    // no arguments
                    method.invoke(target);

                } else {
                    // multiple arguments. try to inject parameters through
                    // guice
                    Object[] parameters = new Object[method.getParameterTypes().length];
                    int i = 0;
                    for (Class<?> type : method.getParameterTypes()) {
                        Object obj = null;
                        if (JobExecutionContext.class.isAssignableFrom(method.getParameterTypes()[i])) {
                            // support mix of JobExecutionContext and other
                            // (injected) arguments
                            obj = context;
                        } else {
                            obj = injector.getInstance(type);
                        }
                        if (obj == null) {
                            // guice did not provide an object
                            logger.warn("Using null value for parameter of type {} in call to scheduled method {}",
                                    type.getName(), method.getName());
                        }
                        parameters[i++] = obj;
                    }
                    method.invoke(target, parameters);
                }
            }
        };

        // determine the job wrapper class to use (the classes provide different
        // annotations to support the requested Quartz functionality)
        Class<? extends AbstractNinjaQuartzJob> jobClass = null;
        if (allowParallelInvocations) {
            // concurrent jobs
            if (persistent) {
                // concurrent, and persistent
                jobClass = ConcurrentStatefulNinjaQuartzJob.class;
            } else {
                // concurrent, not not persistent
                jobClass = ConcurrentNinjaQuartzJob.class;
            }
        } else {
            // non-concurrent jobs
            if (persistent) {
                // non-concurrent, and persistent
                jobClass = NonConcurrentStatefulNinjaQuartzJob.class;
            } else {
                // non-concurrent, and not persistent
                jobClass = NonConcurrentNinjaQuartzJob.class;
            }
        }

        JobBuilder jobBuilder = JobBuilder.newJob(jobClass).withIdentity(jobName, jobGroup).requestRecovery(jobRecovery)
                .storeDurably(jobDurability);
        if (jobDescription != null) {
            jobBuilder = jobBuilder.withDescription(jobDescription);
        }
        JobDetail jobDetail = jobBuilder.build();
        // let the NinjaQuartzJob know which task (wrapping our scheduled
        // method) we want to execute
        jobDetail.getJobDataMap().put(AbstractNinjaQuartzJob.JOB_TASK_KEY, task);

        // store other properties from the annotation in the job's context.
        jobDetail.getJobDataMap().put(AbstractNinjaQuartzJob.JOB_REMOVE_ON_RUNTIME_ERROR,
                quartzSchedule.removeOnError());
        jobDetail.getJobDataMap().put(AbstractNinjaQuartzJob.JOB_FORCE_KEEP, quartzSchedule.forceKeep());

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
            triggerName = CRON_TRIGGER_NAME_PREFIX + method.getName();
        }

        String triggerGroup = quartzSchedule.triggerGroup();
        if (QuartzSchedule.DEFAULT_TRIGGER_GROUP.equals(triggerGroup)) {
            // by default, use the same trigger group name for all methods
            // within the same declaring class.
            triggerGroup = CRON_TRIGGER_GROUP_PREFIX + method.getDeclaringClass().getName();
        }

        Date startAt = parseTriggerDatetime(quartzSchedule.triggerStartAt(), method);
        Date endAt = parseTriggerDatetime(quartzSchedule.triggerEndAt(), method);
        int triggerPriority = quartzSchedule.triggerPriority();
        int misfirePolicy = quartzSchedule.cronScheduleMisfirePolicy();

        // check for an application.conf key name specified instead of a
        // diretly specified schedule string
        String appConfCronSchedule = ninjaProperties.get(quartzSchedule.cronSchedule());
        // if we have a non-null value from application.conf, we use that.
        // otherwise assume a schedule was directly given in the annotation
        String cronSchedule = null;
        if (appConfCronSchedule != null) {
            cronSchedule = appConfCronSchedule;
            logger.debug("Using cronSchedule from application.conf property '{}': {}", quartzSchedule.cronSchedule(),
                    cronSchedule);
        } else {
            cronSchedule = quartzSchedule.cronSchedule();
            logger.debug("Using cronSchedule as provided in annotation: {}", cronSchedule);
        }

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
        if (!scheduler.isStarted()) {
            if (schedulerDelay == -1) {
                scheduler.start();
                logger.debug("Started new scheduler of type {}", scheduler.getClass().getName());
            } else {
                scheduler.startDelayed(schedulerDelay);
                logger.debug("Started new scheduler of type {} with delay {}", scheduler.getClass().getName(),
                        schedulerDelay);
            }
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
            // this is not necessarily an error. default values are empty.
            logger.debug(
                    "Invalid datetime format for parameter value '{}' on {}.{}. Expected format is '{}'. "
                            + "Affected Start/End constraint will NOT be used.",
                    datetime, method.getDeclaringClass().getName(), method.getName(), TRIGGER_DATETIME_FORMAT);
            return null;
        }
    }

}
