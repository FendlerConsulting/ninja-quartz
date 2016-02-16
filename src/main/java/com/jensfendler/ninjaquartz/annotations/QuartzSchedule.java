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
package com.jensfendler.ninjaquartz.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.quartz.CronExpression;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;

import com.jensfendler.ninjaquartz.NinjaQuartzScheduleHelper;

/**
 * @author Jens Fendler <jf@jensfendler.com>
 *
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface QuartzSchedule {

    String DEFAULT_JOB_NAME = "_noJobName";

    String DEFAULT_JOB_GROUP = "_noJobGroup";

    String DEFAULT_JOB_DESCRIPTION = "_noJobDescription";

    boolean DEFAULT_JOB_RECOVERY = true;

    boolean DEFAULT_JOB_DURABILITY = false;

    String DEFAULT_TRIGGER_NAME = "_noTriggerName";

    String DEFAULT_TRIGGER_GROUP = "_noTriggerGroup";

    public int MISFIRE_POLICY_DO_NOTHING = 1;

    public int MISFIRE_POLICY_FIRE_AND_PROCEED = 2;

    public int MISFIRE_POLICY_IGNORE = 3;

    int DEFAULT_MISFIRE_POLICY = MISFIRE_POLICY_DO_NOTHING;

    int DEFAULT_SCHEDULER_DELAY = -1;

    String DEFAULT_TRIGGER_START_AT = "_noTriggerStartAt";

    String DEFAULT_TRIGGER_END_AT = "_noTriggerEndAt";

    boolean DEFAULT_ALLOW_PARALLEL_INVOCATIONS = false;

    /**
     * The group name of the trigger to use for the scheduled method.
     */
    String triggerGroup() default DEFAULT_TRIGGER_GROUP;

    /**
     * The name of the trigger to use for the scheduled method.
     */
    String triggerName() default DEFAULT_TRIGGER_NAME;

    /**
     * A datetime string in the format
     * {@link NinjaQuartzScheduleHelper#TRIGGER_DATETIME_FORMAT} indicating when
     * the trigger should end.
     * 
     * @see TriggerBuilder#endAt(java.util.Date)
     * 
     */
    String triggerEndAt() default DEFAULT_TRIGGER_END_AT;

    /**
     * The {@link #cronSchedule()} string defines the UNIX Cron-like execution
     * schedule to set for an annotated method.
     * 
     * @see CronExpression
     */
    String cronSchedule();

    /**
     * The name of the {@link Job} to run for the scheduled method.
     */
    String jobName() default DEFAULT_JOB_NAME;

    /**
     * The group name of the {@link Job} to run for the scheduled method.
     */
    String jobGroup() default DEFAULT_JOB_GROUP;

    /**
     * The (optional) description of the {@link Job} to run for the scheduled
     * method.
     */
    String jobDescription() default DEFAULT_JOB_DESCRIPTION;

    /**
     * The recovery strategy for the {@link Job}.
     * 
     * @see JobBuilder#requestRecovery(boolean)
     */
    boolean jobRecovery() default DEFAULT_JOB_RECOVERY;

    /**
     * The durability strategy for the {@link Job}.
     * 
     * @see JobBuilder#storeDurably(boolean)
     */
    boolean jobDurability() default DEFAULT_JOB_DURABILITY;

    /**
     * A datetime string in the format
     * {@link NinjaQuartzScheduleHelper#TRIGGER_DATETIME_FORMAT} indicating when
     * the trigger should start.
     * 
     * @see TriggerBuilder#startAt(java.util.Date)
     */
    String triggerStartAt() default DEFAULT_TRIGGER_START_AT;

    /**
     * The priority for the {@link Trigger} to use for the scheduling.
     * 
     * @See {@link TriggerBuilder#withPriority(int)
     */
    int triggerPriority() default Trigger.DEFAULT_PRIORITY;

    /**
     * The initial delay (in seconds) before the {@link Scheduler} should start
     * running after initialisation. By default the Scheduler will start without
     * delay.
     */
    int schedulerDelay() default DEFAULT_SCHEDULER_DELAY;

    /**
     * The misfire strategy to use if the {@link CronTrigger} misfires.
     * 
     * @see CronScheduleBuilder#withMisfireHandlingInstructionDoNothing()
     * @see CronScheduleBuilder#withMisfireHandlingInstructionFireAndProceed()
     * @see CronScheduleBuilder#withMisfireHandlingInstructionIgnoreMisfires()
     */
    int cronScheduleMisfirePolicy() default DEFAULT_MISFIRE_POLICY;

    /**
     * NinjaQuartz tries to prevent multiple (running in parallel) invocations
     * of the same scheduled method in different worker threads. If such
     * parallel invocations are not a problem for you (or you need this), set
     * this property to true. In most cases, allowing this is probably a bad
     * idea.
     */
    boolean allowParallelInvocations() default DEFAULT_ALLOW_PARALLEL_INVOCATIONS;

}
