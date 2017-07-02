package cz.cuni.mff.d3s.trupple.language.nodes.program.arguments;

import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.VirtualFrame;
import cz.cuni.mff.d3s.trupple.language.nodes.statement.StatementNode;
import cz.cuni.mff.d3s.trupple.language.runtime.customvalues.FileValue;
import cz.cuni.mff.d3s.trupple.language.runtime.exceptions.PascalRuntimeException;

public class FileProgramArgumentAssignmentNode extends StatementNode {

    private final FrameSlot frameSlot;
    private final int index;

    public FileProgramArgumentAssignmentNode(FrameSlot frameSlot, int index) {
        this.frameSlot = frameSlot;
        this.index = index;
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        try {
            FileValue file = (FileValue) frame.getObject(frameSlot);
            file.assignFilePath((String) frame.getArguments()[index]);
        } catch (FrameSlotTypeException e) {
            throw new PascalRuntimeException("Something went wrong");
        }
    }

}
