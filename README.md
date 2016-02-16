Quartz Scheduler integration for the Ninja Web Framework.
=========================================================

## Usage:

- Clone the repository and install locally using `mvn clean compile install` (currently, this repository is not pushed to Maven Central - but might be in the future) 

- Install the NinjaQuartzModule in your Ninja application's conf.Modules class:
`install( NinjaQuartzModule.class );`

- Add `QuartzSchedule` annotations the the methods you would like to schedule with Quartz. 
  For the time being, only `CronTrigger`s are used (the standard trigger's behavious is already available in Ninja's default scheduler).
  To set a schedule, add the required String parameter `cronSchedule` to the annotation.

- Enjoy :-)
