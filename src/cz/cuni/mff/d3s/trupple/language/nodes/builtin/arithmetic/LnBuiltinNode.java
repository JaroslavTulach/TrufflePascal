package cz.cuni.mff.d3s.trupple.language.nodes.builtin.arithmetic;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeInfo;
import cz.cuni.mff.d3s.trupple.exceptions.runtime.LogarithmInvalidArgumentException;
import cz.cuni.mff.d3s.trupple.language.nodes.ExpressionNode;
import cz.cuni.mff.d3s.trupple.language.nodes.builtin.BuiltinNode;
import cz.cuni.mff.d3s.trupple.language.runtime.PascalContext;

@NodeInfo(shortName = "ln")
@NodeChild(value = "argument", type = ExpressionNode.class)
public abstract class LnBuiltinNode extends BuiltinNode {

    public LnBuiltinNode(PascalContext context) {
        super(context);
    }

    @Specialization
    double integerNaturalLogarithmValue(long value) {
        return computeLogarithm(value);
    }

    @Specialization
    double doubleNaturalLogarithmValue(double value) {
        return computeLogarithm(value);
    }

    private double computeLogarithm(double value) {
        if (value > 0) {
            return Math.log(value);
        } else {
            throw new LogarithmInvalidArgumentException(value);
        }
    }

}