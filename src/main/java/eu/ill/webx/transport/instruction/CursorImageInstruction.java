package eu.ill.webx.transport.instruction;

import static eu.ill.webx.transport.instruction.InstructionType.CURSOR;

public class CursorImageInstruction extends Instruction {

    private Long cursorId;

    CursorImageInstruction() {
        super(CURSOR);
    }

    public Long getCursorId() {
        return cursorId;
    }

    public void setCursorId(Long cursorId) {
        this.cursorId = cursorId;
    }
}