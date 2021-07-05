package ocrlabeler;

import java.sql.SQLException;

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
import ocrlabeler.controllers.DatabaseInstance;
import ocrlabeler.controllers.DeleteExpiredExportsJob;
import ocrlabeler.controllers.DumpJwtJob;
import ocrlabeler.controllers.SuggestorJob;
import ocrlabeler.controllers.Utils;
import ocrlabeler.controllers.WaitFor;

/**
 * Hello world!
 *
 */
public class App {
    private static final DatabaseInstance DB = DatabaseInstance.getInstance();

    private static void waitForDB() {
        Dotenv dotenv = Utils.DOTENV;
        String dbHost = dotenv.get("POSTGRES_HOST");
        int dbPort = Integer.parseInt(dotenv.get("POSTGRES_PORT"));
        WaitFor.waitForPort(dbHost, dbPort, 3000);
    }

    private static void resetAllProcessing() {
        try {
            DB.resetProcessingImage();
        } catch (SQLException e) {
            System.err.println("Failed to reset all images with status Processing to NotProcessed. "
                    + "This may indicate a problem with PostgresSQL server! Exitting...");
            e.printStackTrace();
            System.exit(0);
        }
    }

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

    private static void scheduleSuggestorJob(Scheduler scheduler, int intervalInSeconds) throws SchedulerException {
        JobDetail job = JobBuilder.newJob(SuggestorJob.class).withIdentity("SuggestorJob").build();
        Trigger trigger = TriggerBuilder.newTrigger().withIdentity("SuggestorTrigger").startNow()
                .withSchedule(
                        SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(intervalInSeconds).repeatForever())
                .build();
        scheduler.scheduleJob(job, trigger);
    }

    private static void scheduleDeleteExpiredExportsJob(Scheduler scheduler) throws SchedulerException {
        JobDetail job = JobBuilder.newJob(DeleteExpiredExportsJob.class).withIdentity("DeleteExpiredExportsJob")
                .build();
        Trigger trigger = TriggerBuilder.newTrigger().withIdentity("DeleteExpiredExportsTrigger").startNow()
                .withSchedule(CronScheduleBuilder.dailyAtHourAndMinute(3, 0)).build();
        scheduler.scheduleJob(job, trigger);
    }

    public static void main(String[] args) throws SchedulerException {
        waitForDB();
        resetAllProcessing();
        Dotenv dotenv = Utils.DOTENV;
        final int craftInterval = Integer.parseInt(dotenv.get("JOBS_CRAFT_INTERVAL"));
        final int suggestorInterval = Integer.parseInt(dotenv.get("JOBS_SUGGESTOR_INTERVAL"));
        SchedulerFactory schedFact = new StdSchedulerFactory();
        Scheduler scheduler = schedFact.getScheduler();
        scheduler.start();
        scheduleJwtDumpJob(scheduler);
        scheduleCraftJob(scheduler, craftInterval);
        scheduleSuggestorJob(scheduler, suggestorInterval);
        scheduleDeleteExpiredExportsJob(scheduler);
    }
}
