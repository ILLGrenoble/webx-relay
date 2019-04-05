package eu.ill.webx.relay.response;

public abstract class RelayResponse {

    private String type;

    public RelayResponse(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
