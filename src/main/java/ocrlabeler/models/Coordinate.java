package ocrlabeler.models;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class Coordinate {
    private double x;
    private double y;

    public Coordinate(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public void setX(double x) {
        this.x = x;
    }

    public void setY(double y) {
        this.y = y;
    }

    public static Coordinate parseFromJson(JsonElement json) {
        JsonObject obj = json.getAsJsonObject();
        return new Coordinate(obj.get("x").getAsDouble(), obj.get("y").getAsDouble());
    }
}
