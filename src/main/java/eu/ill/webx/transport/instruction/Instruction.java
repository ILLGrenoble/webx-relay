package eu.ill.webx.transport.instruction;

public abstract class Instruction {

    private long id;
    private int type;

    public Instruction() {
    }

    public Instruction(int type) {
        this.type = type;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }

}
