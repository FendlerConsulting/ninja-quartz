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
package ninja.app.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import ninja.Result;
import ninja.Results;
import ninja.app.modules.Counter;
import ninja.app.modules.TestSchedules;

/**
 * @author Jens Fendler
 *
 */
@Singleton
public class Application {

    public static final Logger LOG = LoggerFactory.getLogger(Application.class);

    @Inject
    protected Counter counter;

    /**
     * @return
     */
    public Result index() {
        LOG.info("index() controller running.");
        return Results.ok().text().renderRaw("Index page".getBytes());
    }

    /**
     * @return
     */
    public Result schedules() {
        LOG.info("schedules() controller running.");

        String[] keys = new String[] { TestSchedules.SCHEDULE_TEST_1, TestSchedules.SCHEDULE_TEST_2,
                TestSchedules.SCHEDULE_TEST_3, TestSchedules.SCHEDULE_TEST_4, TestSchedules.SCHEDULE_TEST_5 };

        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < keys.length; i++) {
            Integer value = counter.getValue(keys[i]);
            sb.append(value);
            if (i < (keys.length - 1)) {
                sb.append(",");
            }
        }

        String response = sb.toString();
        LOG.info("Counter values: {}", response);
        return Results.ok().text().renderRaw(response.getBytes());
    }
}
