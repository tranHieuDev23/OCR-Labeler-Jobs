package ocrlabeler.models;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.google.gson.JsonElement;

public class TextRegion {
    private Coordinate[] vertices;

    public TextRegion(Coordinate[] vertices) {
        this.vertices = vertices;
    }

    public Coordinate[] getVertices() {
        return vertices;
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

    public static TextRegion parseFromJson(JsonElement json) {
        List<Coordinate> vertexList = new ArrayList<>();
        for (JsonElement item : json.getAsJsonArray()) {
            vertexList.add(Coordinate.parseFromJson(item));
        }
        return new TextRegion(vertexList.toArray(new Coordinate[0]));
    }
}
