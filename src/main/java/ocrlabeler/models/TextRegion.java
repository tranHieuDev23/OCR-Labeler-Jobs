package ocrlabeler.models;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.google.gson.JsonElement;

public class TextRegion {
    private String regionId;
    private Coordinate[] vertices;

    public TextRegion(String regionId, Coordinate[] vertices) {
        this.regionId = regionId;
        this.vertices = vertices;
    }

    public String getRegionId() {
        return regionId;
    }

    public Coordinate[] getVertices() {
        return vertices;
    }

    public void setRegionId(String regionId) {
        this.regionId = regionId;
    }

    public void setVertices(Coordinate[] vertices) {
        this.vertices = vertices;
    }

    @Override
    public String toString() {
        return Arrays.stream(vertices).map((item) -> {
            return item.getX() + "," + item.getY();
        }).collect(Collectors.joining(";"));
    }

    public static TextRegion parseFromString(String s) {
        String[] parts = s.split(";");
        Coordinate[] vertices = new Coordinate[parts.length];
        for (int i = 0; i < parts.length; i++) {
            String[] values = parts[i].split(",");
            vertices[i] = new Coordinate(Double.parseDouble(values[0]), Double.parseDouble(values[1]));
        }
        return new TextRegion(null, vertices);
    }

    public static TextRegion parseFromJson(JsonElement json) {
        List<Coordinate> vertexList = new ArrayList<>();
        for (JsonElement item : json.getAsJsonArray()) {
            vertexList.add(Coordinate.parseFromJson(item));
        }
        return new TextRegion(null, vertexList.toArray(new Coordinate[0]));
    }
}
