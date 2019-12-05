package eu.ill.webx.transport.instruction;

import static eu.ill.webx.transport.instruction.InstructionType.KEYBOARD;

public class KeyboardInstruction extends Instruction {

    private Integer key;

    private Boolean pressed;

    KeyboardInstruction() {
        super(KEYBOARD);
    }

    public Integer getKey() {
        return key;
    }

    public void setKey(Integer key) {
        this.key = key;
    }

    public Boolean getPressed() {
        return pressed;
    }

    public void setPressed(Boolean pressed) {
        this.pressed = pressed;
    }
}
