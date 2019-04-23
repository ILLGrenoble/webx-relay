package eu.ill.webx.transport.instruction;

import static eu.ill.webx.transport.instruction.InstructionType.MOUSE;

public class MouseInstruction extends Instruction {

    private Integer x;
    private Integer y;
    private Integer buttonMask;

    public Integer getX() {
        return x;
    }

    public void setX(Integer x) {
        this.x = x;
    }

    public Integer getY() {
        return y;
    }

    public void setY(Integer y) {
        this.y = y;
    }

    public Integer getButtonMask() {
        return buttonMask;
    }

    public void setButtonMask(Integer buttonMask) {
        this.buttonMask = buttonMask;
    }

    MouseInstruction() {
        super(MOUSE);
    }
}
