package eu.ill.webx.relay.response;


public class RelayImageResponse extends RelayResponse {

    private String base64ImageData;

    public RelayImageResponse(long commandId) {
        super("Image", commandId);
    }

    public RelayImageResponse(long commandId, String base64ImageData) {
        super("Image", commandId);
        this.base64ImageData = base64ImageData;
    }


    public String getBase64ImageData() {
        return base64ImageData;
    }

    public void setBase64ImageData(String base64ImageData) {
        this.base64ImageData = base64ImageData;
    }
}
