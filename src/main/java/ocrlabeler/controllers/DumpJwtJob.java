package ocrlabeler.controllers;

import java.sql.SQLException;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class DumpJwtJob implements Job {
    private static final DatabaseInstance DB = DatabaseInstance.getInstance();

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            DB.dumpJwt();
        } catch (SQLException e) {
            throw new JobExecutionException("Failed to run SQL Query to dump JWT", e);
        }
    }
}
