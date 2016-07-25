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
package ninja.app.conf;

import com.google.inject.Singleton;
import com.jensfendler.ninjaquartz.NinjaQuartzModule;

import ninja.app.controllers.Application;
import ninja.app.modules.TestSchedules;
import ninja.conf.FrameworkModule;

/**
 * @author Jens Fendler
 *
 */
@Singleton
public class Module extends FrameworkModule {

    /**
     * @see com.google.inject.AbstractModule#configure()
     */
    @Override
    public void configure() {
        install(new NinjaQuartzModule());
        Application.LOG.info("NinjaQuartzModule() has been installed.");

        bind(TestSchedules.class);
        Application.LOG.info("Scheduled methods in TestSchedules.class have been set up.");
    }

}
