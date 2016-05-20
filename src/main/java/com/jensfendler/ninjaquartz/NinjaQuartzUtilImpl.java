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
import java.util.HashSet;
import java.util.Set;

import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.Trigger;
import org.quartz.impl.matchers.GroupMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * @author Jens Fendler
 *
 */
public class NinjaQuartzUtilImpl implements NinjaQuartzUtil {

    protected static final Logger LOG = LoggerFactory.getLogger(NinjaQuartzModule.class);

    /**
     * The {@link SchedulerFactory} provider as injected to the constructor.
     */
    protected Provider<SchedulerFactory> schedulerFactoryProvider;

    /**
     * @param schedulerFactoryProvider
     *            the {@link SchedulerFactory} provider to be injected.
     */
    @Inject
    public NinjaQuartzUtilImpl(Provider<SchedulerFactory> schedulerFactoryProvider) {
        this.schedulerFactoryProvider = schedulerFactoryProvider;
    }

    /**
     * @see com.jensfendler.ninjaquartz.NinjaQuartzUtil#getAllSchedulers()
     */
    public Collection<Scheduler> getAllSchedulers() throws SchedulerException {
        return schedulerFactoryProvider.get().getAllSchedulers();
    }

    /**
     * @see com.jensfendler.ninjaquartz.NinjaQuartzUtil#getSchedulerByName(java.lang.String)
     */
    public Scheduler getSchedulerByName(String schedulerName) throws SchedulerException {
        return schedulerFactoryProvider.get().getScheduler(schedulerName);
    }

    /**
     * @see com.jensfendler.ninjaquartz.NinjaQuartzUtil#getTriggersOfJob(java.lang.String,
     *      java.lang.String)
     */
    public Collection<Trigger> getTriggersOfJob(String jobName, String jobGroup) throws SchedulerException {
        JobKey jk = JobKey.jobKey(jobName, jobGroup);
        Set<Trigger> allTriggers = new HashSet<Trigger>();
        for (Scheduler scheduler : getAllSchedulers()) {
            try {
                allTriggers.addAll(scheduler.getTriggersOfJob(jk));
            } catch (SchedulerException se) {
                // try to skip this scheduler with a simple error message. If
                // other parts of this method throw a SchedulerException, throw
                // it out.
                LOG.error(
                        "Exception while looking for trigger of job " + jobName + "." + jobGroup + " scheduler "
                                + scheduler.getSchedulerName() + ". Skipping scheduler.",
                        jobName, jobGroup, scheduler.getSchedulerName());
            }
        }
        return allTriggers;
    }

    /**
     * @see com.jensfendler.ninjaquartz.NinjaQuartzUtil#getAllJobDetails()
     */
    public Collection<JobDetail> getAllJobDetails() throws SchedulerException {
        Set<JobDetail> allJobDetails = new HashSet<JobDetail>();
        for (Scheduler scheduler : getAllSchedulers()) {
            for (JobKey jk : scheduler.getJobKeys(GroupMatcher.anyJobGroup())) {
                allJobDetails.add(scheduler.getJobDetail(jk));
            }
        }
        return allJobDetails;
    }

    /**
     * @see com.jensfendler.ninjaquartz.NinjaQuartzUtil#getJobDetail(java.lang.String,
     *      java.lang.String)
     */
    public JobDetail getJobDetail(String jobName, String jobGroup) throws SchedulerException {
        for (Scheduler scheduler : getAllSchedulers()) {
            for (JobKey jk : scheduler.getJobKeys(GroupMatcher.jobGroupEquals(jobGroup))) {
                if (jk.getName().equals(jobName)) {
                    return scheduler.getJobDetail(jk);
                }
            }
        }
        return null;
    }

}
