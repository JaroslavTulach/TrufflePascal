package cz.cuni.mff.d3s.trupple.language.nodes.builtin.units.dos;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import cz.cuni.mff.d3s.trupple.language.runtime.customvalues.Reference;
import cz.cuni.mff.d3s.trupple.language.nodes.ExpressionNode;
import cz.cuni.mff.d3s.trupple.language.nodes.statement.StatementNode;

import java.time.LocalDateTime;

/**
 * Official specification:
 * GetTime returns the system's time. Hour is a on a 24-hour time scale. sec100 is in hundredth of a second.
 *
 * Differences:
 * None.
 */
@NodeChildren({ @NodeChild(type = ExpressionNode.class), @NodeChild(type = ExpressionNode.class),
        @NodeChild(type = ExpressionNode.class), @NodeChild(type = ExpressionNode.class) })
public abstract class GetTimeNode extends StatementNode {

    @Specialization
    void getTime(Reference hourReference, Reference minuteReference, Reference secondReference, Reference sec100Reference) {
        LocalDateTime now = LocalDateTime.now();

        this.setLongValue(hourReference, now.getHour());
        this.setLongValue(minuteReference, now.getMinute());
        this.setLongValue(secondReference, now.getSecond());
        this.setLongValue(sec100Reference, now.getNano() / 1000 / 1000 / 10);  // by specification, Sunday shall be 0, not 7 in Pascal
    }

    private void setLongValue(Reference reference, long value) {
        reference.getFromFrame().setLong(reference.getFrameSlot(), value);
    }

}