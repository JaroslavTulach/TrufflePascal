package cz.cuni.mff.d3s.trupple.parser.identifierstable.types.compound;

import com.oracle.truffle.api.frame.FrameSlotKind;
import cz.cuni.mff.d3s.trupple.parser.identifierstable.types.complex.OrdinalDescriptor;
import cz.cuni.mff.d3s.trupple.parser.identifierstable.types.TypeDescriptor;
import cz.cuni.mff.d3s.trupple.parser.identifierstable.types.primitive.*;

public class ArrayDescriptor implements TypeDescriptor {

    private final OrdinalDescriptor dimension;
    private final TypeDescriptor valuesDescriptor;

    public ArrayDescriptor(OrdinalDescriptor dimension, TypeDescriptor valuesDescriptor) {
        this.dimension = dimension;
        this.valuesDescriptor = valuesDescriptor;
    }

    @Override
    public FrameSlotKind getSlotKind() {
        return FrameSlotKind.Object;
    }

    @Override
    public Object getDefaultValue() {
        if (valuesDescriptor == IntDescriptor.getInstance()) {
            return new int[this.dimension.getSize()];
        } if (valuesDescriptor == LongDescriptor.getInstance()) {
            return new long[this.dimension.getSize()];
        } else if (valuesDescriptor == RealDescriptor.getInstance()) {
            return new double[this.dimension.getSize()];
        } else if (valuesDescriptor == CharDescriptor.getInstance()) {
            return new char[this.dimension.getSize()];
        } else if (valuesDescriptor == BooleanDescriptor.getInstance()) {
            return new boolean[this.dimension.getSize()];
        } else {
            Object[] data = new Object[dimension.getSize()];
            for (int i = 0; i < data.length; ++i) {
                data[i] = valuesDescriptor.getDefaultValue();
            }
            return data;
        }
    }

    public int getOffset() {
        return this.dimension.getFirstIndex();
    }

    public TypeDescriptor getValuesDescriptor() {
        return this.valuesDescriptor;
    }

    @Override
    public boolean convertibleTo(TypeDescriptor type) {
        return false;
    }

}
