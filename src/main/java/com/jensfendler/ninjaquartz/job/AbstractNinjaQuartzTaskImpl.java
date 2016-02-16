/*
 * Copyright (c) 2014 Fendler Consulting cc.
 *
 * Proprietary material.
 *
 * All rights reserved. Terms and Conditions apply.
 * See http://www.jensfendler.com/terms-conditions/ for details.
 *
 */
package com.jensfendler.ninjaquartz.job;

import java.lang.reflect.InvocationTargetException;

import org.quartz.JobExecutionContext;

/**
 * @author Jens Fendler <jf@jensfendler.com>
 *
 */
public abstract class AbstractNinjaQuartzTaskImpl implements NinjaQuartzTask {

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
