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
package ninja.app.modules;

import org.quartz.JobExecutionContext;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.jensfendler.ninjaquartz.annotations.QuartzSchedule;

import ninja.app.controllers.Application;
import ninja.utils.NinjaProperties;

/**
 * @author Jens Fendler
 *
 */
@Singleton
public class TestSchedules {

    public static final String SCHEDULE_TEST_1 = "scheduleTest1";

    public static final String SCHEDULE_TEST_2 = "scheduleTest2";

    public static final String SCHEDULE_TEST_3 = "scheduleTest3";

    public static final String SCHEDULE_TEST_4 = "scheduleTest4";

    public static final String SCHEDULE_TEST_5 = "scheduleTest5";

    @Inject
    protected Counter counter;

    /**
     * Run every 2 seconds, no arguments
     */
    @QuartzSchedule(cronSchedule = "0/2 * * * * ?", schedulerDelay = 1, jobDescription = "Test Schedule 1", jobName = "test1")
    public void testSchedule1() {
        Integer value = counter.updateValue(SCHEDULE_TEST_1);
        Application.LOG.info("\n\n\ntestSchedule1() updated value to {}\n\n\n", value);
    }

    /**
     * Run every 5 seconds, JobExecutionContext argument
     */
    @QuartzSchedule(cronSchedule = "0/2 * * * * ?", schedulerDelay = 2, jobDescription = "Test Schedule 2", jobName = "test2")
    public void testSchedule2(JobExecutionContext context) {
        Integer value = counter.updateValue(SCHEDULE_TEST_2);
        Application.LOG.info("\n\n\ntestSchedule2() updated value to {}. Description={}\n\n\n", value,
                context.getJobDetail().getDescription());
    }

    /**
     * Run every 5 seconds (configured in application.conf), JobExecutionContext
     * argument
     * 
     * @param context
     */
    @QuartzSchedule(cronSchedule = "schedule.testSchedule3", schedulerDelay = 3, jobDescription = "Test Schedule 3", jobName = "test3")
    public void testSchedule3(JobExecutionContext context) {
        Integer value = counter.updateValue(SCHEDULE_TEST_3);
        Application.LOG.info("\n\n\ntestSchedule3() updated value to {}. Description={}\n\n\n", value,
                context.getJobDetail().getDescription());
    }

    /**
     * Run every 5 seconds (configured in application.conf), guice-injected
     * {@link NinjaProperties} argument.
     * 
     * @param context
     */
    @QuartzSchedule(cronSchedule = "0/2 * * * * ?", schedulerDelay = 4, jobDescription = "Test Schedule 4", jobName = "test4")
    public void testSchedule4(NinjaProperties ninjaProperties) {
        Integer value = counter.updateValue(SCHEDULE_TEST_4);
        Application.LOG.info("\n\n\ntestSchedule4() updated value to {}. Available ninjaProperties: {}\n\n\n", value,
                ninjaProperties.getAllCurrentNinjaProperties().size());
    }

    /**
     * Run every 5 seconds (configured in application.conf), guice-injected
     * {@link NinjaProperties} argument.
     * 
     * @param context
     */
    @QuartzSchedule(cronSchedule = "0/2 * * * * ?", schedulerDelay = 5, jobDescription = "Test Schedule 5", jobName = "test5")
    public void testSchedule5(NinjaProperties ninjaProperties, JobExecutionContext context) {
        Integer value = counter.updateValue(SCHEDULE_TEST_5);
        Application.LOG.info(
                "\n\n\ntestSchedule5() updated value to {}. Context: {}, Available ninjaProperties: {}\n\n\n", value,
                context.hashCode(), ninjaProperties.getAllCurrentNinjaProperties().size());
    }
}
