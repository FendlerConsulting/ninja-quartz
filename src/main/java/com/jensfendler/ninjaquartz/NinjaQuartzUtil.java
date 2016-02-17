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

import java.util.Collection;

import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;

/**
 * Utility methods which allow users to interact with Quartz objects (i.e.
 * Schedulers, Triggers, and JobDetails).
 * 
 * To use these methods, inject {@link NinjaQuartzUtil} into your class e.g.
 * like this:
 * 
 * <code>
 * &#64;Inject
 * NinjaQuartzUtil quartzUtil;
 * </code>
 * 
 * @author Jens Fendler
 *
 */
public interface NinjaQuartzUtil {

    /**
     * @return a {@link Collection} of all available {@link Scheduler}s.
     * @throws SchedulerException
     *             as thrown by the Scheduler
     */
    public Collection<Scheduler> getAllSchedulers() throws SchedulerException;

    /**
     * @param schedulerName
     *            the name of the scheduler to get
     * @return the {@link Scheduler} with the given name.
     * @throws SchedulerException
     *             as thrown by the Scheduler
     */
    public Scheduler getSchedulerByName(String schedulerName) throws SchedulerException;

    /**
     * Gets all active {@link Trigger}s of a scheduled {@link Job} (looking at
     * all active {@link Scheduler}s), with the given jobName and jobGroup.
     * 
     * @param jobName
     *            the name of the job
     * @param jobGroup
     *            the group name of the job
     * @return a {@link Collection} of {@link Trigger}s for the given job name,
     *         which may be empty if no {@link Trigger} was found for the given
     *         job name/group.
     * @throws SchedulerException
     *             as thrown by the Scheduler
     */
    public Collection<Trigger> getTriggersOfJob(String jobName, String jobGroup) throws SchedulerException;

    /**
     * @return a {@link Collection} of all scheduled {@link JobDetail}s from all
     *         {@link Scheduler}s.
     * @throws SchedulerException
     *             as thrown by the Scheduler
     */
    public Collection<JobDetail> getAllJobDetails() throws SchedulerException;

    /**
     * Returns the {@link JobDetail} for the job with the given name and group,
     * from any {@link Scheduler}.
     * 
     * @param jobName
     *            the name of the job
     * @param jobGroup
     *            the group name of the job
     * @return the {@link JobDetail}, or null if not found.
     * @throws SchedulerException
     *             as thrown by the Scheduler
     */
    public JobDetail getJobDetail(String jobName, String jobGroup) throws SchedulerException;
}
