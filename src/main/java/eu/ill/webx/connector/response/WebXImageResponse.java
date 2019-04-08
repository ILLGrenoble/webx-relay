package eu.ill.webx.connector.response;

public class WebXImageResponse extends WebXResponse {

    private String base64ImageData;

    public WebXImageResponse() {
    }

    public WebXImageResponse(String base64ImageData) {
        this.base64ImageData = base64ImageData;
    }


    public String getBase64ImageData() {
        return base64ImageData;
    }

    public void setBase64ImageData(String base64ImageData) {
        this.base64ImageData = base64ImageData;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("WebXImageResponse{");
        sb.append("base64ImageData='").append(base64ImageData).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
