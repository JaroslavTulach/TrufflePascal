package cz.cuni.mff.d3s.trupple.language.nodes;

import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.nodes.UnexpectedResultException;

import cz.cuni.mff.d3s.trupple.language.PascalTypesGen;
import cz.cuni.mff.d3s.trupple.language.runtime.customvalues.EnumValue;
import cz.cuni.mff.d3s.trupple.language.nodes.statement.StatementNode;
import cz.cuni.mff.d3s.trupple.language.runtime.customvalues.PascalArray;
import cz.cuni.mff.d3s.trupple.parser.identifierstable.types.TypeDescriptor;
import cz.cuni.mff.d3s.trupple.parser.identifierstable.types.constant.*;
import cz.cuni.mff.d3s.trupple.parser.identifierstable.types.primitive.*;

/**
 * This is a base node for each node that shall return any value after its execution. Not all the
 * specialized execute{Type} methods are implemented because we do not really need them since we are using
 * Truffle's specializations.
 */
@NodeInfo(description = "Abstract class for all nodes that return value")
public abstract class ExpressionNode extends StatementNode {

    /**
     * Gets type of the expression. This method is mainly used for compile time type checking.
     */
    public abstract TypeDescriptor getType();

    @Override
    public void executeVoid(VirtualFrame virtualFrame) {
        executeGeneric(virtualFrame);
    }

	public abstract Object executeGeneric(VirtualFrame frame);

	public boolean executeBoolean(VirtualFrame frame) throws UnexpectedResultException {
		return PascalTypesGen.expectBoolean(executeGeneric(frame));
	}

	public long executeLong(VirtualFrame frame) throws UnexpectedResultException {
		return PascalTypesGen.expectLong(executeGeneric(frame));
	}

	public char executeChar(VirtualFrame frame) throws UnexpectedResultException {
		return PascalTypesGen.expectCharacter(executeGeneric(frame));
	}

	public EnumValue executeEnum(VirtualFrame frame) throws UnexpectedResultException {
	    return PascalTypesGen.expectEnumValue(executeGeneric(frame));
    }

    public PascalArray executePascalArray(VirtualFrame frame) throws UnexpectedResultException {
	    return PascalTypesGen.expectPascalArray(executeGeneric(frame));
    }

    protected boolean isInt() {
        return getType() == IntDescriptor.getInstance() || getType() instanceof IntConstantDescriptor;
    }

    protected boolean isLong() {
        return getType() == LongDescriptor.getInstance() || getType() instanceof LongConstantDescriptor;
    }

    protected boolean isDouble() {
        return getType() == RealDescriptor.getInstance() || getType() instanceof RealConstantDescriptor;
    }

    protected boolean isChar() {
        return getType() == CharDescriptor.getInstance() || getType() instanceof CharConstantDescriptor;
    }

    protected boolean isBoolean() {
        return getType() == BooleanDescriptor.getInstance() || getType() instanceof BooleanConstantDescriptor;
    }

    protected boolean isLongReference() {
        return this.isLong() && isReference();
    }

    protected boolean isDoubleReference() {
        return this.isDouble() && isReference();
    }

    protected boolean isCharReference() {
        return this.isChar() && isReference();
    }

    protected boolean isBooleanReference() {
        return this.isBoolean() && isReference();
    }

    protected boolean isReference() {
        return false;
    }

}
