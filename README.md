Quartz Scheduler integration for the Ninja Web Framework.
=========================================================
This project provides a module to integrate the [Quartz Scheduler](https://quartz-scheduler.org/) with the [Ninja Framework](https://github.com/ninjaframework/ninja).


## Usage:

- Clone the repository and install locally using `mvn clean compile install` (currently, this repository is not pushed to Maven Central - but might be in the future) 

- Add the dependency to your application's pom.xml:

    <dependency>
        <groupId>com.jensfendler</groupId>
        <artifactId>ninja-quartz</artifactId>
        <version>0.0.1-SNAPSHOT</version>
    </dependency>

- Install the NinjaQuartzModule in your Ninja application's conf.Modules class:

    install( NinjaQuartzModule.class );

- Add `QuartzSchedule` annotations the the methods you would like to schedule with Quartz. 
  For the time being, only `CronTrigger`s are used (the standard trigger's behavious is already available in Ninja's default scheduler).
  To set a schedule, add the required String parameter `cronSchedule` to the annotation.

- Enjoy :-)


## License

Copyright (C) 2016 Fendler Consulting cc.
This work is licensed under the Apache License, Version 2.0. See LICENSE for details.
