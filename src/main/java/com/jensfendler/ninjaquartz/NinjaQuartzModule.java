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

import org.quartz.SchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.matcher.Matchers;
import com.jensfendler.ninjaquartz.provider.QuartzSchedulerFactoryProvider;

/**
 * @author Jens Fendler
 *
 */
public class NinjaQuartzModule extends AbstractModule {

    private static final Logger logger = LoggerFactory.getLogger(NinjaQuartzModule.class);

    /**
     * @see com.google.inject.AbstractModule#configure()
     */
    @Override
    protected void configure() {
        logger.info("NinjaQuartz Module initialising.");

        // disable Quartz' checking for updates
        System.setProperty("org.terracotta.quartz.skipUpdateCheck", "true");

        bind(SchedulerFactory.class).toProvider(QuartzSchedulerFactoryProvider.class).in(Singleton.class);

        NinjaQuartzScheduleHelper scheduleHelper = new NinjaQuartzScheduleHelper();
        requestInjection(scheduleHelper);

        bindListener(Matchers.any(), new NinjaQuartzTypeListener(scheduleHelper));
        bind(NinjaQuartzScheduleHelper.class).toInstance(scheduleHelper);

        bind(NinjaQuartzUtil.class).to(NinjaQuartzUtilImpl.class);

        logger.info("NinjaQuartz Module initialisation completed.");
    }

}
