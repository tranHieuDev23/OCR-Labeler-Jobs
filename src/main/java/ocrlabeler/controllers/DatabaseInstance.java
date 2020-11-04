package ocrlabeler.controllers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import io.github.cdimascio.dotenv.Dotenv;
import ocrlabeler.models.Image;
import ocrlabeler.models.ImageStatus;
import ocrlabeler.models.TextRegion;

public class DatabaseInstance {
    private final String DUMP_JWT_QUERY = "DELETE FROM public.\"BlacklistedJwts\"\n"
            + "WHERE \"BlacklistedJwts\".exp < EXTRACT(epoch FROM (NOW() - INTERVAL '30 DAY'));";
    private final String SELECT_UNPROCESSED_IMAGES_QUERY = "SELECT \"imageId\", \"imageUrl\", \"uploadedBy\" FROM public.\"Images\"\n"
            + "WHERE \"Images\".status = 'NotProcessed' ORDER BY \"Images\".\"uploadedDate\" DESC LIMIT ?;";
    private final String RESET_PROCESSING_IMAGES_QUERY = "UPDATE public.\"Images\" SET status = 'NotProcessed'\n"
            + "WHERE \"Images\".status = 'Processing'";
    private final String ADD_REGION_QUERY = "INSERT INTO public.\"TextRegions\"(\n"
            + "\"regionId\", \"imageId\", region, label, status, \"uploadedBy\", \"labeledBy\", \"verifiedBy\")\n"
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?);";
    private final String UPDATE_IMAGE_STATUS_QUERY = "UPDATE public.\"Images\" SET status = ?\n"
            + "WHERE \"Images\".\"imageId\" = ?";

    private final Connection conn;
    private final int craftProcessLimit;

    private DatabaseInstance() {
        Dotenv dotenv = Utils.DOTENV;
        String databaseHost = dotenv.get("POSTGRES_HOST");
        String databasePort = dotenv.get("POSTGRES_PORT");
        String databaseDB = dotenv.get("POSTGRES_DB");
        String databaseUrl = createDatabaseUrl(databaseHost, databasePort, databaseDB);
        String databaseUser = dotenv.get("POSTGRES_USER");
        String databasePassword = dotenv.get("POSTGRES_PASSWORD");
        Properties props = new Properties();
        props.setProperty("user", databaseUser);
        props.setProperty("password", databasePassword);
        try {
            conn = DriverManager.getConnection(databaseUrl, props);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to connect to database", e);
        }
        craftProcessLimit = Integer.parseInt(dotenv.get("JOBS_CRAFT_PROCESS_LIMIT"));
    }

    private String createDatabaseUrl(String databaseHost, String databasePort, String databaseDB) {
        return new StringBuilder("jdbc:postgresql://").append(databaseHost).append(':').append(databasePort).append('/')
                .append(databaseDB).toString();
    }

    private static final DatabaseInstance INSTANCE = new DatabaseInstance();

    public static final synchronized DatabaseInstance getInstance() {
        return INSTANCE;
    }

    public void dumpJwt() throws SQLException {
        Statement st = conn.createStatement();
        st.execute(DUMP_JWT_QUERY);
    }

    public void resetProcessingImage() throws SQLException {
        Statement st = conn.createStatement();
        st.execute(RESET_PROCESSING_IMAGES_QUERY);
    }

    public Image[] getUnprocessedImages() throws SQLException {
        PreparedStatement st = conn.prepareStatement(SELECT_UNPROCESSED_IMAGES_QUERY);
        st.setInt(1, craftProcessLimit);
        ResultSet rs = st.executeQuery();
        if (!rs.next()) {
            return new Image[] {};
        }
        List<Image> results = new ArrayList<>();
        if (!rs.isAfterLast()) {
            do {
                String imageId = rs.getString("imageId");
                String imageUrl = rs.getString("imageUrl");
                String uploaedBy = rs.getString("uploadedBy");
                results.add(new Image(imageId, imageUrl, uploaedBy, null));
            } while (rs.next());
        }
        return results.toArray(new Image[0]);
    }

    public void setImageStatus(Image image, ImageStatus status) throws SQLException {
        PreparedStatement imageSt = conn.prepareStatement(UPDATE_IMAGE_STATUS_QUERY);
        imageSt.setString(1, status.name());
        imageSt.setString(2, image.getImageId());
        imageSt.execute();
    }

    public void updateRegions(Image image) throws SQLException {
        if (image.getRegion().length > 0) {
            PreparedStatement regionSt = conn.prepareStatement(ADD_REGION_QUERY);
            for (TextRegion region : image.getRegion()) {
                regionSt.setString(1, UUID.randomUUID().toString());
                regionSt.setString(2, image.getImageId());
                regionSt.setString(3, region.toString());
                regionSt.setString(4, null);
                regionSt.setString(5, "NotLabeled");
                regionSt.setString(6, image.getUploadedBy());
                regionSt.setString(7, null);
                regionSt.setString(8, null);
                regionSt.addBatch();
            }
            regionSt.executeBatch();
        }
        setImageStatus(image, ImageStatus.Processed);
    }
}
