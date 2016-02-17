Quartz Scheduler integration for the Ninja Web Framework.
=========================================================
This project provides a module to integrate the [Quartz Scheduler](https://quartz-scheduler.org/) with the [Ninja Framework](https://github.com/ninjaframework/ninja).


## Usage:

- Clone the repository and install locally using `mvn clean compile install` (currently, this repository is not pushed to Maven Central - but might be in the future) 

- Add the dependency to your application's pom.xml:

    &lt;dependency&gt;
        &lt;groupId&gt;com.jensfendler&lt;/groupId&gt;
        &lt;artifactId&gt;ninja-quartz&lt;/artifactId&gt;
        &lt;version&gt;0.0.1-SNAPSHOT&lt;/version&gt;
    &lt;/dependency&gt;

- Install the NinjaQuartzModule in your Ninja application's conf.Modules class:

```java
install( NinjaQuartzModule.class );
```

- Add `QuartzSchedule` annotations the the methods you would like to schedule with Quartz. 
  For the time being, only `CronTrigger`s are used (the standard trigger's behavious is already available in Ninja's default scheduler).
  To set a schedule, add the required String parameter `cronSchedule` to the annotation.

- Bind the classes containing your annotated methods using `bind(YourClassWithScheduledMethods.class)` in `conf.Module`.

- Enjoy :-)


### Accessing Schedulers, Triggers and Jobs

You can inject `NinjaQuartzUtil` into your controllers, DAOs, etc. to get access to all running `Scheduler`s, `Trigger`s and `JobDetail`s.

Example:

```java

@Inject
NinjaQuartzUtils ninjaQuartz;

public Result numberOfJobs() {
	return Results.text().render("We have " + ninjaQuartz.getAllJobDetails().size() + " scheduled jobs." );
}

```   

Please have a look at the `NinjaQuartzUtil` interface for available methods.


## License

Copyright (C) 2016 Fendler Consulting cc.
This work is licensed under the Apache License, Version 2.0. See LICENSE for details.
