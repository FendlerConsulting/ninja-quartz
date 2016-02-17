Quartz Scheduler integration for the Ninja Web Framework.
=========================================================
This project provides a module to integrate the [Quartz Scheduler](https://quartz-scheduler.org/) with the [Ninja Framework](https://github.com/ninjaframework/ninja).


Basic Usage:
------------

- Clone the repository and install locally using `mvn clean compile install` (currently, this repository is not pushed to Maven Central - but might be in the future) 

- Add the dependency to your application's pom.xml:

```xml

    <dependency>
        <groupId>com.jensfendler</groupId>
        <artifactId>ninja-quartz</artifactId>
        <version>0.0.1-SNAPSHOT</version>
    </dependency>

```

- Install the NinjaQuartzModule in your Ninja application's conf.Modules class:

```java

install( new NinjaQuartzModule() );

```

- Add `QuartzSchedule` annotations the the methods you would like to schedule with Quartz. 
  For the time being, only `CronTrigger`s are used (the standard trigger's behavious is already available in Ninja's default scheduler).
  To set a schedule, add the required String parameter `cronSchedule` to the annotation.

- Bind the classes containing your annotated methods using `bind(YourClassWithScheduledMethods.class)` in `conf.Module`.

- Enjoy :-)


Annotation Parameters and Job Configuration
-------------------------------------------
The `@QuartzSchedule` annotation has a number of parameters which can be used to fine-tune the job and its scheduling (on a per method basis), and to use some advanced features of Quartz.

- `cronSchedule`: This is the only required parameter without a default value. It must contain a String in Cron syntax, specifying the schedule to use.

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

Please have a look at the `NinjaQuartzUtil` interface for available methods.


Known Issues
------------
- Using `@Transactional` and `@QuartzSchedule` annotations together:

If you annotate the same method as transactional, and quartz-scheduled, you will notice the scheduler will never run the method. This appears to be a Guice related problem (a similar effect has been reported (here)[https://github.com/ninjaframework/ninja/issues/417] with the standard Ninja scheduler).
To overcome this issue until the root cause is resolved, you should split your scheduled method into two parts: the top-level one with the `@QuartzSchedule` annotation, which can then call another method (in the same or in another class) which has the `@Transactional` annotation. So far, this seems to be working fine.

## License

Copyright (C) 2016 Fendler Consulting cc.
This work is licensed under the Apache License, Version 2.0. See LICENSE for details.
