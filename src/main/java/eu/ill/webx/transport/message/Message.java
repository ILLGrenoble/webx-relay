package eu.ill.webx.transport.message;

public abstract class Message {

    private String type;
    private long commandId;

    public Message() {
    }

    public Message(long commandId) {
        this.commandId = commandId;
    }

    public long getCommandId() {
        return commandId;
    }

    public void setCommandId(long commandId) {
        this.commandId = commandId;
    }
}
