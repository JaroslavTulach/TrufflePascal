package cz.cuni.mff.d3s.trupple.language.nodes.variables.write;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import cz.cuni.mff.d3s.trupple.language.nodes.ExpressionNode;
import cz.cuni.mff.d3s.trupple.language.nodes.statement.StatementNode;
import cz.cuni.mff.d3s.trupple.language.runtime.customvalues.PointerValue;
import cz.cuni.mff.d3s.trupple.language.runtime.customvalues.RecordValue;

@NodeChildren({
        @NodeChild(value = "recordNode", type = ExpressionNode.class),
        @NodeChild(value = "valueNode", type = ExpressionNode.class)
})
public abstract class AssignToRecordField extends StatementNode {

    private final String identifier;

    protected AssignToRecordField(String identifier) {
        this.identifier = identifier;
    }

    @Specialization
    void assignInt(RecordValue record, int value) {
        record.getFrame().setInt(record.getSlot(this.identifier), value);
    }

    @Specialization
    void assignLong(RecordValue record, long value) {
        record.getFrame().setLong(record.getSlot(this.identifier), value);
    }

    @Specialization
    void assignDouble(RecordValue record, double value) {
        record.getFrame().setDouble(record.getSlot(this.identifier), value);
    }

    @Specialization
    void assignChar(RecordValue record, char value) {
        record.getFrame().setByte(record.getSlot(this.identifier), (byte) value);
    }

    @Specialization
    void assignBoolean(RecordValue record, boolean value) {
        record.getFrame().setBoolean(record.getSlot(this.identifier), value);
    }

    @Specialization
    void assignPointer(RecordValue record, PointerValue pointer) {
        PointerValue recordPointer = (PointerValue) record.getFrame().getValue(record.getSlot(this.identifier));
        recordPointer.setHeapSlot(pointer.getHeapSlot());
    }

    @Specialization
    void assignGeneric(RecordValue record, Object value) {
        record.getFrame().setObject(record.getSlot(this.identifier), value);
    }

}