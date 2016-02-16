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
package com.jensfendler.ninjaquartz.provider;

import org.quartz.SchedulerFactory;
import org.slf4j.Logger;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import ninja.utils.NinjaProperties;

/**
 * @author Jens Fendler <jf@jensfendler.com>
 *
 */
@Singleton
public class QuartzSchedulerFactoryProvider implements Provider<SchedulerFactory> {

	private NinjaProperties ninjaProperties;

	private Logger logger;

	private SchedulerFactory schedulerFactory;

	/**
	 * 
	 */
	@Inject
	public QuartzSchedulerFactoryProvider(NinjaProperties ninjaProperties, Logger logger) {
		this.ninjaProperties = ninjaProperties;
		this.logger = logger;
		logger.info("{} instantiated.", getClass().getName());
	}

	/**
	 * @see com.google.inject.Provider#get()
	 */
	@Override
	public SchedulerFactory get() {
		logger.info("{} called to get SchedulerFactory.", getClass().getName());
		if (schedulerFactory == null) {
			loadSchedulerFactory();
		}
		return schedulerFactory;
	}

	private void loadSchedulerFactory() {
		String sfClassName = ninjaProperties.getWithDefault("quartz.schedulerFactory", "org.quartz.impl.StdSchedulerFactory");
		logger.info("Using Quartz SchedulerFactory from {}.", sfClassName);
		try {
			Class<?> sfClass = Class.forName(sfClassName);
			schedulerFactory = (SchedulerFactory) sfClass.newInstance();
		} catch (ClassNotFoundException e) {
			throw new RuntimeException("Quartz SchedulerFactory class '" + sfClassName + "' not found.", e);
		} catch (InstantiationException e) {
			throw new RuntimeException("Cannot instantiate Quartz SchedulerFactory class '" + sfClassName + "'.", e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException("Illegal access exception while trying to instantiate Quartz SchedulerFactory class '" + sfClassName + "'.",
					e);
		}
	}

}
