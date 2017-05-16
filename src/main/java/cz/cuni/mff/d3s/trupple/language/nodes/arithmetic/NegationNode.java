package cz.cuni.mff.d3s.trupple.language.nodes.arithmetic;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeInfo;

@NodeInfo(shortName = "neg")
public abstract class NegationNode extends UnaryNode {;

    @Specialization
    int neg(int argument) {
        return -argument;
    }

	@Specialization
	long neg(long argument) {
		return -argument;
	}

	@Specialization
	double neg(double argument) {
		return -argument;
	}

}
