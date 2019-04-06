package eu.ill.webx.relay.response;

public abstract class RelayResponse {

    private String type;
    private long commandId;

    public RelayResponse(String type) {
        this.type = type;
    }

    public RelayResponse(String type, long commandId) {
        this.type = type;
        this.commandId = commandId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public long getCommandId() {
        return commandId;
    }

    public void setCommandId(long commandId) {
        this.commandId = commandId;
    }
}
