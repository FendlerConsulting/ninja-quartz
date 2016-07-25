Quartz Scheduler integration for the Ninja Web Framework.
=========================================================
This project provides a module to integrate the [Quartz Scheduler](https://quartz-scheduler.org/) with the [Ninja Framework](https://github.com/ninjaframework/ninja).


Basic Usage:
------------

- Add the dependency to your application's `pom.xml`:

```xml

    <dependency>
        <groupId>com.jensfendler</groupId>
        <artifactId>ninja-quartz</artifactId>
        <version>0.0.7</version>
    </dependency>

```

- Install an instance of the `NinjaQuartzModule` in your Ninja application's `conf.Modules class`:

```java

install( new NinjaQuartzModule() );

```

- Add `@QuartzSchedule` annotations the the methods you would like to schedule with Quartz. 
  For the time being, only `CronTrigger`s are used (the standard trigger's behaviour is already available in Ninja's default scheduler).
  To set a schedule, use the required String parameter `cronSchedule` in the annotation.
  
  The `cronSchedule` string may contain either a directly specified cron scheduler value (as understood by Quartz), *or* a property key
  name from Ninja's `application.conf`, which must then contain a cron scheduler value. The later case is useful if you want to have
  some external configuration control over the schedules used, and prefer not to hardcode them into your application.
  

Example:

```java

@Singleton
public class MySchedules {

    @QuartzSchedule(cronSchedule = "0/10 * * * * ?")
    public void myFirstScheduledMethod() {
        // do your thing
    }


    // Use a cron schedule as specified in Ninja's application.conf property 'schedule.secondMethod' 
    @QuartzSchedule(cronSchedule = "schedule.secondMethod")
    public void mySecondScheduledMethod(JobExecutionContext context) {
        // do your thing
        // you can access the Quartz job's JobExecutionContext here as well
    }

    @QuartzSchedule(cronSchedule = "schedule.secondMethod")
    public void myThirdScheduledMethod(NinjaProperties ninjaProperties) {
    	// ninjaProperties is injected via guice
		if ( ninjaProperties.isDev() ) {
			// do something
		}
    }
}

```

Since version 0.0.7 Ninja-Quartz supports Guice-injection for parameters of your scheduled methods. I.e. you can use any types which the guice injector knows how to provide. In addition, Ninja-Quartz also allows you to use a parameter of type `JobExecutionContext`, allowing you to access the Quartz provided context at runtime.


- Bind the classes containing your annotated methods using `bind(YourClassWithScheduledMethods.class)` in `conf.Module`.

- Enjoy :-)


Annotation Parameters and Job Configuration
-------------------------------------------
The `@QuartzSchedule` annotation has a number of parameters which can be used to fine-tune the job and its scheduling (on a per method basis), and to use some advanced features of Quartz.

- `cronSchedule`: This is the only required parameter without a default value. It must contain a String in Quartz' Cron Scheduler syntax, specifying the schedule to use, *or* the name of a property in Ninja's `application.conf`, which must then contain a string in Quartz' Cron Scheduler syntax. 

The following other parameters are available, all of which have sensible defaults:

- `jobName` (String): the name to use for the job wrapping your scheduled method. By default, Ninja-Quartz creates a job name based on a constant prefix plus your method name.
- `jobGroup` (String): the name of the group to put the job in. By default, Ninja-Quartz creates the group name from a constant prefix plus the name of the class containing the scheduled method.
- `jobDescription` (String): a short description of the job. This is optional, and is currently only used in some selected log messages.
- `jobRecovery` (boolean): specifies if the job should recover (i.e re-executed after a Quartz "recovery" or "fail-over" situation). Defaults to true.
- `jobDurability` (boolean): specifies if the job should be duarable (i.e. remain stored after it orphaned and no more Triggers reference it). Defaults to false.

- `triggerName` (String): the name to use for the trigger of the job.
- `triggerGroup` (String): the name of the trigger group to use for the job.
- `startTriggerAt` (String): a date-time value in yyyyMMddHHmmSS format, indicating when the trigger should start running. By default, the trigger will start immediately.
- `startEndAt` (String): a date-time value in yyyyMMddHHmmSS format, indicating when the trigger should end running. By default, the trigger will continue to run indefinitely.
- `triggerPriority` (int): a priority level to assign to this trigger. Defaults to the Quartz default of 5.
 
- `schedulerDelay` (int): an initial delay period (in seconds) before the scheduler starts running (and potentially executing scheduled jobs). This might be useful if you want your first scheduled invocations to occur only after the application start-up phase is completed (e.g. after 60 seconds or so). By default, the scheduler will start immediately, possibly invoking your scheduled methods before your Ninja application is fully up and running. 
- `cronScheduleMisfirePolicy` (int): the policy to use in the event of a trigger mis-firing. Can be one of the constants `QuartzSchedule.MISFIRE_POLICY_DO_NOTHING` (default), `QuartzSchedule.MISFIRE_POLICY_FIRE_AND_PROCEED`, or `QuartzSchedule.MISFIRE_POLICY_IGNORE`.

- `allowConcurrent` (boolean): specifies if multiple (parallel/concurrent) invocations of the same scheduled method should be allowed or not. Defaults to false. Unless you have a very good reason to allow concurrent executions, you should probably leave this one untouched.
- `persistent` (boolean): specifies if the job's `JobDataMap` (containing the run-time context of your job) should be kept between invocations, thereby making your jobs stateful. Defaults to false. 
- `forceKeep` (boolean): (_since 0.0.3_) if set to true, scheduled tasks will not be removed from the scheduler upon _any_ exception thrown during their execution. Defaults to false. Prior to version 0.0.3 all exceptions resulted in the task being removed. 
- `removeOnError` (boolean): (_since 0.0.3_) if set to true, scheduled tasks will be removed upon an `InvocationTargetException` (typically wrapping run-time exceptions from your method) thrown while trying to invoke the scheduled method. Defaults to false, i.e. keeping your methods scheduled as long as only "normal" exceptions are thrown from their code. Prior to version 0.0.3 _all_ exceptions resulted in the task being removed.  



Quartz Configuration through application.conf
---------------------------------------------
If you want to use a custom `SchedulerFactory`, you can set the class name of your factory through the `quartz.schedulerFactory` property in your `application.conf`.  

If you would like fine-tune other configuration options of the Quartz library via a properties file, you can do so by providing a property `quartz.properties` in your `application.conf`, pointing to your Quartz properties file to use. (_since 0.0.3_) If you do this, please note that the `SchedulerFactory` must still be configured with the `quartz.schedulerFactory` property in your `application.conf`.



Accessing Schedulers, Triggers and Jobs:
----------------------------------------
Simply inject `NinjaQuartzUtil` into your controllers, DAOs, etc., and you will have instant access to your `Scheduler`s, `Trigger`s and `JobDetail`s.

Example:

```java

@Inject
NinjaQuartzUtils ninjaQuartz;

public Result numberOfJobs() {
	return Results.text().render("We have " + ninjaQuartz.getAllJobDetails().size() + " scheduled jobs." );
}

```

Please have a look at the `NinjaQuartzUtil` interface for details of available methods.


Known Issues
------------
- Using `@Transactional` and `@QuartzSchedule` annotations together:

If you annotate the same method as transactional, and quartz-scheduled, you will notice the scheduler will never run the method. This appears to be a Guice related problem - a similar effect has been reported [here](https://github.com/ninjaframework/ninja/issues/417) when using the standard Ninja scheduler.
To overcome this issue until the root cause is resolved, you should split your scheduled method into two parts: the top-level one with the `@QuartzSchedule` annotation, which can then call another method (in the same or in another class) which has the `@Transactional` annotation. So far, this seems to be working fine.

## License

Copyright (C) 2016 Fendler Consulting cc.
This work is licensed under the Apache License, Version 2.0. See LICENSE for details.
