package ocrlabeler.controllers;

import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import io.github.cdimascio.dotenv.Dotenv;
import ocrlabeler.models.Image;
import ocrlabeler.models.ImageStatus;
import ocrlabeler.models.TextRegion;

public class CraftJob implements Job {
    private static final DatabaseInstance DB = DatabaseInstance.getInstance();
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private static final URI CRAFT_URI;

    static {
        Dotenv dotenv = Utils.DOTENV;
        String craftHost = dotenv.get("CRAFT_HOST");
        String craftPort = dotenv.get("CRAFT_PORT");
        String craftApi = new StringBuilder("http://").append(craftHost).append(':').append(craftPort)
                .append("/query_box").toString();
        try {
            CRAFT_URI = new URI(craftApi);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to build CRAFT URI", e);
        }
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            Image[] unprocessedImages = DB.getUnprocessedImages();
            for (Image item : unprocessedImages) {
                Thread itemThread = new Thread(() -> {
                    try {
                        DB.setImageStatus(item, ImageStatus.Processing);
                        Image imageWithRegions = handleImage(item);
                        if (imageWithRegions != null) {
                            DB.updateRegions(imageWithRegions);
                        } else {
                            DB.setImageStatus(item, ImageStatus.NotProcessed);
                        }
                    } catch (SQLException e) {
                        System.err.println("Failed to run UPDATE SQL query");
                        e.printStackTrace(System.err);
                    }
                });
                itemThread.start();
            }
        } catch (SQLException e) {
            throw new JobExecutionException("Failed to run SELECT SQL query", e);
        }
    }

    private Image handleImage(Image image) {
        Path imagePath = Path.of(Utils.joinPath(Utils.UPLOAD_DIRECTORY, image.getImageUrl()));
        HttpRequest request;
        try {
            request = HttpRequest.newBuilder().uri(CRAFT_URI).header("Content-Type", "image/jpeg")
                    .POST(BodyPublishers.ofFile(imagePath)).build();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
        JsonElement json;
        try {
            HttpResponse<String> response = HTTP_CLIENT.send(request, BodyHandlers.ofString());
            json = JsonParser.parseString(response.body());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        JsonElement regionJsonArray = json.getAsJsonObject().get("regions");
        List<TextRegion> regionList = new ArrayList<>();
        for (JsonElement item : regionJsonArray.getAsJsonArray()) {
            regionList.add(TextRegion.parseFromJson(item));
        }
        TextRegion[] regions = regionList.toArray(new TextRegion[0]);
        return new Image(image.getImageId(), image.getImageUrl(), image.getUploadedBy(), regions);
    }
}
