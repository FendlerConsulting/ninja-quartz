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

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;

import org.quartz.JobExecutionContext;

/**
 * @author Jens Fendler <jf@jensfendler.com>
 *
 */
public abstract class AbstractNinjaQuartzTaskImpl implements NinjaQuartzTask, Serializable {

    protected String taskName;

    /**
     * 
     */
    public AbstractNinjaQuartzTaskImpl(String taskName) {
        this.taskName = taskName;
    }

    /**
     * @see com.jensfendler.ninjaquartz.job.NinjaQuartzTask#execute(org.quartz.JobExecutionContext)
     */
    @Override
    public abstract void execute(JobExecutionContext context)
            throws IllegalAccessException, IllegalArgumentException, InvocationTargetException;

    /**
     * @see com.jensfendler.ninjaquartz.job.NinjaQuartzTask#getTaskName()
     */
    @Override
    public String getTaskName() {
        return taskName;
    }

}
