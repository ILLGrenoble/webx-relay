package eu.ill.webx.transport.instruction;

public class Instruction {

    public static int CONNECT = 1;
    public static int WINDOWS = 2;
    public static int IMAGE = 3;

    private int type;
    private long id;
    private String stringPayload;
    private long numericPayload;

    public Instruction() {
    }

    public Instruction(int type) {
        this.type = type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getNumericPayload() {
        return numericPayload;
    }

    public void setNumericPayload(long numericPayload) {
        this.numericPayload = numericPayload;
    }

    public String getStringPayload() {
        return stringPayload;
    }

    public void setStringPayload(String stringPayload) {
        this.stringPayload = stringPayload;
    }
}
