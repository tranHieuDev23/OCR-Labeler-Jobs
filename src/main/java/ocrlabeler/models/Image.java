package ocrlabeler.models;

public class Image {
    private String imageId;
    private String imageUrl;
    private String uploadedBy;
    private TextRegion[] region;

    public Image(String imageId, String imageUrl, String uploadedBy, TextRegion[] region) {
        this.imageId = imageId;
        this.imageUrl = imageUrl;
        this.uploadedBy = uploadedBy;
        this.region = region;
    }

    public String getImageId() {
        return imageId;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getUploadedBy() {
        return uploadedBy;
    }

    public TextRegion[] getRegion() {
        return region;
    }

    public void setImageId(String imageId) {
        this.imageId = imageId;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void setUploadedBy(String uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public void setRegion(TextRegion[] regions) {
        this.region = regions;
    }
}
