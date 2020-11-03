package ocrlabeler;

import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;

import io.github.cdimascio.dotenv.Dotenv;
import ocrlabeler.controllers.CraftJob;
import ocrlabeler.controllers.DumpJwtJob;
import ocrlabeler.controllers.Utils;

/**
 * Hello world!
 *
 */
public class App {
    private static void scheduleJwtDumpJob(Scheduler scheduler) throws SchedulerException {
        JobDetail job = JobBuilder.newJob(DumpJwtJob.class).withIdentity("DumpJwtJob").build();
        Trigger trigger = TriggerBuilder.newTrigger().withIdentity("DumpJwtTrigger").startNow()
                .withSchedule(CronScheduleBuilder.dailyAtHourAndMinute(0, 0)).build();
        scheduler.scheduleJob(job, trigger);
    }

    private static void scheduleCraftJob(Scheduler scheduler, int intervalInSeconds) throws SchedulerException {
        JobDetail job = JobBuilder.newJob(CraftJob.class).withIdentity("CraftJob").build();
        Trigger trigger = TriggerBuilder.newTrigger().withIdentity("CraftTrigger").startNow()
                .withSchedule(
                        SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(intervalInSeconds).repeatForever())
                .build();
        scheduler.scheduleJob(job, trigger);
    }

    public static void main(String[] args) throws SchedulerException {
        Dotenv dotenv = Utils.DOTENV;
        final int craftInterval = Integer.parseInt(dotenv.get("JOBS_CRAFT_INTERVAL"));
        SchedulerFactory schedFact = new StdSchedulerFactory();
        Scheduler scheduler = schedFact.getScheduler();
        scheduler.start();
        scheduleJwtDumpJob(scheduler);
        scheduleCraftJob(scheduler, craftInterval);
    }
}
