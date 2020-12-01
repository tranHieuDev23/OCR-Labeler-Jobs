package ocrlabeler.controllers;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.stream.Collectors;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import org.apache.commons.codec.Charsets;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import io.github.cdimascio.dotenv.Dotenv;
import ocrlabeler.models.Image;
import ocrlabeler.models.ImageStatus;
import ocrlabeler.models.TextRegion;

public class SuggestorJob implements Job {
    private static final DatabaseInstance DB = DatabaseInstance.getInstance();
    private static final HttpClient HTTP_CLIENT = HttpClients.createDefault();
    private static final URI SUGGESTOR_URI;
    private static final int REGION_LIMIT;

    static {
        Dotenv dotenv = Utils.DOTENV;
        String craftHost = dotenv.get("SUGGESTOR_HOST", "localhost");
        String craftPort = dotenv.get("SUGGESTOR_PORT", "7000");
        String craftApi = new StringBuilder("http://").append(craftHost).append(':').append(craftPort).append("/query")
                .toString();
        try {
            SUGGESTOR_URI = new URI(craftApi);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to build CRAFT URI", e);
        }
        REGION_LIMIT = Integer.parseInt(dotenv.get("JOBS_SUGGESTOR_REGION_LIMIT"));
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            Image[] unprocessedImages = DB.getUnprocessedImages(ImageStatus.PrePublished, 1);
            for (Image item : unprocessedImages) {
                Thread itemThread = new Thread(() -> {
                    try {
                        handleImage(item);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
                itemThread.start();
            }
        } catch (SQLException e) {
            throw new JobExecutionException("Failed to run SELECT SQL query", e);
        }
    }

    private void handleImage(Image image) throws SQLException, ClientProtocolException, IOException {
        TextRegion[] regions = DB.getRegionsOfImage(image, REGION_LIMIT);
        if (regions.length == 0) {
            DB.setImageStatus(image, ImageStatus.Published);
            return;
        }
        HttpPost request = new HttpPost(SUGGESTOR_URI);
        request.setEntity(requestBody(image, regions));
        HttpResponse response = HTTP_CLIENT.execute(request);
        String jsonStr = parseResponse(response);
        JsonElement json = JsonParser.parseString(jsonStr);
        JsonArray regionJsonArray = json.getAsJsonObject().get("result").getAsJsonArray();
        for (int i = 0; i < regions.length; i++) {
            DB.updateRegionSuggestion(regions[i].getRegionId(), regionJsonArray.get(i).getAsString());
        }
    }

    private HttpEntity requestBody(Image image, TextRegion[] regions) {
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.addTextBody("regionJson", getRegionJson(regions));
        builder.addBinaryBody("file", new File(Utils.joinPath(Utils.UPLOAD_DIRECTORY, image.getImageUrl())));
        return builder.build();
    }

    private String getRegionJson(TextRegion[] regions) {
        StringBuilder builder = new StringBuilder();
        builder.append("{\"regions\":[");
        builder.append(Arrays.stream(regions).map((reg) -> {
            return new StringBuilder().append('[').append(Arrays.stream(reg.getVertices()).map((vert) -> {
                return new StringBuilder().append("{\"x\":").append(vert.getX()).append(",\"y\":").append(vert.getY())
                        .append("}").toString();
            }).collect(Collectors.joining(","))).append(']').toString();
        }).collect(Collectors.joining(",")));
        builder.append("]}");
        return builder.toString();
    }

    private String parseResponse(HttpResponse response) throws ParseException, IOException {
        HttpEntity entity = response.getEntity();
        Header encodingHeader = entity.getContentEncoding();
        Charset encoding = encodingHeader == null ? StandardCharsets.UTF_8
                : Charsets.toCharset(encodingHeader.getValue());
        return EntityUtils.toString(entity, encoding);
    }
}
