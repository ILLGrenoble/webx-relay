package eu.ill.webx.transport.instruction;


import static eu.ill.webx.transport.instruction.InstructionType.IMAGE;

public class ImageInstruction extends Instruction {

    private Integer windowId;

    ImageInstruction() {
        super(IMAGE);
    }

    public Integer getWindowId() {
        return windowId;
    }

    public void setWindowId(Integer windowId) {
        this.windowId = windowId;
    }
}
