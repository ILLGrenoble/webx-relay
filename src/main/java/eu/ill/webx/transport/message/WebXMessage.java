package eu.ill.webx.transport.message;

public abstract class WebXMessage {

    private long commandId;

    public WebXMessage() {
    }

    public WebXMessage(long commandId) {
        this.commandId = commandId;
    }

    public long getCommandId() {
        return commandId;
    }

    public void setCommandId(long commandId) {
        this.commandId = commandId;
    }
}
