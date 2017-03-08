package cz.cuni.mff.d3s.trupple.language.runtime;

import java.util.HashMap;
import java.util.Map;

import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.nodes.NodeInfo;

import cz.cuni.mff.d3s.trupple.language.nodes.ExpressionNode;
import cz.cuni.mff.d3s.trupple.language.nodes.PascalRootNode;
import cz.cuni.mff.d3s.trupple.language.nodes.builtin.*;
import cz.cuni.mff.d3s.trupple.language.nodes.call.ReadAllArgumentsNode;
import cz.cuni.mff.d3s.trupple.language.nodes.call.ReadArgumentNode;
import cz.cuni.mff.d3s.trupple.language.nodes.function.ReadSubroutineArgumentNodeGen;

public class PascalSubroutineRegistry {
	private final Map<String, PascalFunction> functions = new HashMap<>();
	private final PascalContext context;

	PascalSubroutineRegistry(PascalContext context, boolean installBuiltins) {
		this.context = context;
		if(installBuiltins) {
			installBuiltins();
		}
	}

	void registerSubroutineName(String identifier) {
		functions.put(identifier, PascalFunction.createUnimplementedFunction());
	}

	public PascalFunction lookup(String identifier) {
		return functions.get(identifier);
	}

	void setFunctionRootNode(String identifier, PascalRootNode rootNode) {
        PascalFunction function = functions.get(identifier);
		assert function != null;
		
		RootCallTarget callTarget = Truffle.getRuntime().createCallTarget(rootNode);
        function.setCallTarget(callTarget);
	}

	protected void installBuiltins() {
		installBuiltinWithVariableArgumentsCount(WriteBuiltinNodeFactory.getInstance());
		installBuiltinWithVariableArgumentsCount(ReadBuiltinNodeFactory.getInstance());
        installBuiltinOneArgument(SuccBuiltinNodeFactory.getInstance());
        installBuiltinOneArgument(PredBuiltinNodeFactory.getInstance());
	}

	private void installBuiltinOneArgument(NodeFactory<? extends BuiltinNode> factory) {
	    ExpressionNode argument = new ReadArgumentNode(0);
	    BuiltinNode builtinNode = factory.createNode(argument);
	    PascalRootNode rootNode = new PascalRootNode(new FrameDescriptor(), builtinNode);
        String name = lookupNodeInfo(builtinNode.getClass()).shortName();
        this.register(name, rootNode);
    }

	@SuppressWarnings("unused")
	private void installBuiltin(NodeFactory<? extends BuiltinNode> factory) {
		int argumentCount = factory.getExecutionSignature().size();
		ExpressionNode[] argumentNodes = new ExpressionNode[argumentCount];

		for (int i = 0; i < argumentCount; i++) {
			argumentNodes[i] = new ReadArgumentNode(i);
		}

		BuiltinNode builtinBodyNode = factory.createNode(argumentNodes, context);
		PascalRootNode rootNode = new PascalRootNode(new FrameDescriptor(), builtinBodyNode);

		String name = lookupNodeInfo(builtinBodyNode.getClass()).shortName();
		this.register(name, rootNode);
	}

	protected void installBuiltinWithVariableArgumentsCount(NodeFactory<? extends BuiltinNode> factory) {
		ExpressionNode argumentsNode[] = new ExpressionNode[1];
		argumentsNode[0] = new ReadAllArgumentsNode();
		BuiltinNode bodyNode = factory.createNode(context, argumentsNode);
		PascalRootNode rootNode = new PascalRootNode(new FrameDescriptor(), bodyNode);

		String name = lookupNodeInfo(bodyNode.getClass()).shortName();
		this.register(name, rootNode);
	}
	
	
	private void register(String identifier, PascalRootNode rootNode){
		RootCallTarget callTarget = Truffle.getRuntime().createCallTarget(rootNode);
		PascalFunction pascalFunction = new PascalFunction(identifier, callTarget);

		functions.put(identifier, pascalFunction);
	}

	private static NodeInfo lookupNodeInfo(Class<?> clazz) {
		if (clazz == null) {
			return null;
		}
		NodeInfo info = clazz.getAnnotation(NodeInfo.class);
		if (info != null) {
			return info;
		} else {
			return lookupNodeInfo(clazz.getSuperclass());
		}
	}
}
