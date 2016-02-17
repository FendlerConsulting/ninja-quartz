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

import org.quartz.JobExecutionContext;

import com.jensfendler.ninjaquartz.annotations.QuartzSchedule;

/**
 * A {@link NinjaQuartzTask} wraps the invocation of a scheduled method (i.e. a
 * method annotated with {@link QuartzSchedule}).
 * 
 * @author Jens Fendler
 *
 */
public interface NinjaQuartzTask {

    /**
     * @param context
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     */
    public void execute(JobExecutionContext context)
            throws IllegalAccessException, IllegalArgumentException, InvocationTargetException;

    /**
     * @return the name of the wrapped task
     */
    public String getTaskName();

}
