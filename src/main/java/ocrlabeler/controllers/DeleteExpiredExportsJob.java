package ocrlabeler.controllers;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import io.github.cdimascio.dotenv.Dotenv;

public class DeleteExpiredExportsJob implements Job {
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private static final URI EXPORT_URI;

    static {
        Dotenv dotenv = Utils.DOTENV;
        String craftHost = dotenv.get("EXPORT_HOST");
        String craftPort = dotenv.get("EXPORT_PORT");
        String craftApi = new StringBuilder("http://").append(craftHost).append(':').append(craftPort)
                .append("/api/delete-expired").toString();
        try {
            EXPORT_URI = new URI(craftApi);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to build CRAFT URI", e);
        }
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        HttpRequest request = HttpRequest.newBuilder().uri(EXPORT_URI).POST(BodyPublishers.noBody()).build();
        try {
            HTTP_CLIENT.send(request, BodyHandlers.discarding());
        } catch (IOException e) {
            e.printStackTrace();
            throw new JobExecutionException(e);
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new JobExecutionException(e);
        }
    }
}