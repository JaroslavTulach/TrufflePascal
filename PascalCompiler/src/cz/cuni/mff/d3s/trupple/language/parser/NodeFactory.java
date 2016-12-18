package cz.cuni.mff.d3s.trupple.language.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.oracle.truffle.api.frame.FrameSlot;

import cz.cuni.mff.d3s.trupple.language.customtypes.ICustomType;
import cz.cuni.mff.d3s.trupple.language.nodes.BlockNode;
import cz.cuni.mff.d3s.trupple.language.nodes.ExpressionNode;
import cz.cuni.mff.d3s.trupple.language.nodes.NopNode;
import cz.cuni.mff.d3s.trupple.language.nodes.PascalRootNode;
import cz.cuni.mff.d3s.trupple.language.nodes.StatementNode;
import cz.cuni.mff.d3s.trupple.language.nodes.arithmetic.AddNodeGen;
import cz.cuni.mff.d3s.trupple.language.nodes.arithmetic.DivideIntegerNodeGen;
import cz.cuni.mff.d3s.trupple.language.nodes.arithmetic.DivideNodeGen;
import cz.cuni.mff.d3s.trupple.language.nodes.arithmetic.ModuloNodeGen;
import cz.cuni.mff.d3s.trupple.language.nodes.arithmetic.MultiplyNodeGen;
import cz.cuni.mff.d3s.trupple.language.nodes.arithmetic.NegationNodeGen;
import cz.cuni.mff.d3s.trupple.language.nodes.arithmetic.SubstractNodeGen;
import cz.cuni.mff.d3s.trupple.language.nodes.builtin.RandomBuiltinNode;
import cz.cuni.mff.d3s.trupple.language.nodes.builtin.RandomizeBuiltinNode;
import cz.cuni.mff.d3s.trupple.language.nodes.builtin.ReadlnBuiltinNode;
import cz.cuni.mff.d3s.trupple.language.nodes.call.InvokeNodeGen;
import cz.cuni.mff.d3s.trupple.language.nodes.control.BreakNode;
import cz.cuni.mff.d3s.trupple.language.nodes.control.CaseNode;
import cz.cuni.mff.d3s.trupple.language.nodes.control.ForNode;
import cz.cuni.mff.d3s.trupple.language.nodes.control.IfNode;
import cz.cuni.mff.d3s.trupple.language.nodes.control.RepeatNode;
import cz.cuni.mff.d3s.trupple.language.nodes.control.WhileNode;
import cz.cuni.mff.d3s.trupple.language.nodes.function.FunctionBodyNode;
import cz.cuni.mff.d3s.trupple.language.nodes.function.FunctionBodyNodeGen;
import cz.cuni.mff.d3s.trupple.language.nodes.function.ProcedureBodyNode;
import cz.cuni.mff.d3s.trupple.language.nodes.literals.CharLiteralNode;
import cz.cuni.mff.d3s.trupple.language.nodes.literals.DoubleLiteralNode;
import cz.cuni.mff.d3s.trupple.language.nodes.literals.FunctionLiteralNode;
import cz.cuni.mff.d3s.trupple.language.nodes.literals.LogicLiteralNode;
import cz.cuni.mff.d3s.trupple.language.nodes.literals.LongLiteralNode;
import cz.cuni.mff.d3s.trupple.language.nodes.literals.StringLiteralNode;
import cz.cuni.mff.d3s.trupple.language.nodes.logic.AndNodeGen;
import cz.cuni.mff.d3s.trupple.language.nodes.logic.EqualsNodeGen;
import cz.cuni.mff.d3s.trupple.language.nodes.logic.LessThanNodeGen;
import cz.cuni.mff.d3s.trupple.language.nodes.logic.LessThanOrEqualNodeGen;
import cz.cuni.mff.d3s.trupple.language.nodes.logic.NotNodeGen;
import cz.cuni.mff.d3s.trupple.language.nodes.logic.OrNodeGen;
import cz.cuni.mff.d3s.trupple.language.nodes.variables.ArrayIndexAssignmentNode;
import cz.cuni.mff.d3s.trupple.language.nodes.variables.AssignmentNodeGen;
import cz.cuni.mff.d3s.trupple.language.nodes.variables.ReadArrayIndexNodeGen;
import cz.cuni.mff.d3s.trupple.language.nodes.variables.ReadVariableNodeGen;
import cz.cuni.mff.d3s.trupple.language.parser.exceptions.LexicalException;
import cz.cuni.mff.d3s.trupple.language.parser.identifierstable.types.OrdinalDescriptor;
import cz.cuni.mff.d3s.trupple.language.parser.identifierstable.types.TypeDescriptor;
import cz.cuni.mff.d3s.trupple.language.parser.identifierstable.types.UnknownDescriptor;
import cz.cuni.mff.d3s.trupple.language.runtime.PascalContext;
import cz.cuni.mff.d3s.trupple.language.runtime.PascalSubroutineRegistry;

public class NodeFactory {

	private Parser parser;
	private LexicalScope lexicalScope;

	private Map<String, Unit> units = new HashMap<>();
	private Unit currentUnit = null;

	public NodeFactory(Parser parser) {
		this.parser = parser;
	}

	void startPascal() {
		assert this.lexicalScope == null;
		this.lexicalScope = new LexicalScope(null, "main");
	}

	void registerVariables(List<String> identifiers, Token variableType) {
		String typeName = variableType.val.toLowerCase();

		for (String identifier : identifiers) {
			try {
				lexicalScope.registerLocalVariable(identifier, typeName);
			} catch (LexicalException e) {
				parser.SemErr(e.getMessage());
			}
		}
	}

    void registerVariables(List<String> identifiers, TypeDescriptor typeDescriptor) {
        for (String identifier : identifiers) {
            try {
                lexicalScope.registerLocalVariable(identifier, typeDescriptor);
            } catch (LexicalException e) {
                parser.SemErr(e.getMessage());
            }
        }
    }

    OrdinalDescriptor createSimpleOrdinalDescriptor(final int lowerBound, final int upperBound) {
        try {
            return lexicalScope.createRangeDescriptor(lowerBound, upperBound);
        } catch (LexicalException e){
            parser.SemErr(e.getMessage());
            return lexicalScope.createImplicitRangeDescriptor();
        }
    }

    OrdinalDescriptor createSimpleOrdinalDescriptorFromTypename(Token typeNameToken) {
        String identifier = this.getIdentifierFromToken(typeNameToken);
        try {
            return lexicalScope.createRangeDescriptorFromTypename(identifier);
        } catch (LexicalException e){
            parser.SemErr(e.getMessage());
            return lexicalScope.createImplicitRangeDescriptor();
        }
    }

    TypeDescriptor registerEnum(List<String> enumIdentifiers) {
        try {
            return lexicalScope.registerEnum(enumIdentifiers);
        } catch (LexicalException e) {
            parser.SemErr(e.getMessage());
            return UnknownDescriptor.SINGLETON;
        }
    }

	void registerArrayVariable(List<String> identifiers, List<OrdinalDescriptor> ordinalDimensions, Token returnTypeToken) {
		for(String identifier : identifiers) {
            try {
                lexicalScope.registerLocalArrayVariable(identifier, ordinalDimensions, returnTypeToken.val.toLowerCase());
            } catch (LexicalException e) {
                parser.SemErr(e.getMessage());
            }
		}
	}

    void registerIntegerConstant(Token identifierToken, Token valueToken) {
        try {
            long value = this.createLongFromToken(valueToken);
            String identifier = this.getIdentifierFromToken(identifierToken);
            this.lexicalScope.registerLongConstant(identifier, value);
        } catch (LexicalException e) {
            parser.SemErr(e.getMessage());
        }
    }

    void registerSignedIntegerConstant(Token identifierToken, Token sign, Token valueToken) {
        try {
            long value = this.createLongFromToken(valueToken);
            value = (sign.val.equals("-"))? -value : value;
            String identifier = this.getIdentifierFromToken(identifierToken);
            this.lexicalScope.registerLongConstant(identifier, value);
        } catch (LexicalException e) {
            parser.SemErr(e.getMessage());
        }
    }

    void registerRealConstant(Token identifierToken, Token valueToken) {
        try {
            double value = Double.parseDouble(valueToken.val);
            String identifier = this.getIdentifierFromToken(identifierToken);
            this.lexicalScope.registerRealConstant(identifier, value);
        } catch (LexicalException e) {
            parser.SemErr(e.getMessage());
        }
    }

    void registerConstantFromIdentifier(Token identifierToken, Token valueIdentifierToken) {
        String identifier = this.getIdentifierFromToken(identifierToken);
        String identifierValue = this.getIdentifierFromToken(valueIdentifierToken);
        try {
            this.lexicalScope.registerConstantFromConstant(identifier, identifierValue);
        } catch (LexicalException e) {
            parser.SemErr(e.getMessage());
        }
    }

    void registerSignedConstantFromIdentifier(Token identifierToken, Token sign, Token valueIdentifierToken) {
        String identifier = this.getIdentifierFromToken(identifierToken);
        String identifierValue = this.getIdentifierFromToken(valueIdentifierToken);
        try {
            if (sign.val.equals("-")) {
                this.lexicalScope.registerConstantFromConstant(identifier, identifierValue);
            } else {
                this.lexicalScope.registerConstantFromNegatedConstant(identifier, identifierValue);
            }
        } catch (LexicalException e) {
            parser.SemErr(e.getMessage());
        }
    }

    void registerSignedRealConstant(Token identifierToken, Token sign, Token valueToken) {
        try {
            double value = Double.parseDouble(valueToken.val);
            value = (sign.val.equals("-"))? -value : value;
            String identifier = this.getIdentifierFromToken(identifierToken);
            this.lexicalScope.registerRealConstant(identifier, value);
        } catch (LexicalException e) {
            parser.SemErr(e.getMessage());
        }
    }

    void registerBooleanConstant(Token identifierToken, boolean value) {
        try {
            String identifier = this.getIdentifierFromToken(identifierToken);
            this.lexicalScope.registerBooleanConstant(identifier, value);
        } catch (LexicalException e) {
            parser.SemErr(e.getMessage());
        }
    }

    void registerStringOrCharConstant(Token identifierToken, String value) {
        if(value.length() == 1) {
            registerCharConstant(identifierToken, value.charAt(0));
        } else {
            registerStringConstant(identifierToken, value);
        }
    }

    private void registerCharConstant(Token identifierToken, char value) {
        String identifier = this.getIdentifierFromToken(identifierToken);
        try {
            this.lexicalScope.registerCharConstant(identifier, value);
        } catch (LexicalException e) {
            parser.SemErr(e.getMessage());
        }
    }

    private void registerStringConstant(Token identifierToken, String value) {
        String identifier = this.getIdentifierFromToken(identifierToken);
        try {
            this.lexicalScope.registerStringConstant(identifier, value);
        } catch (LexicalException e) {
            parser.SemErr(e.getMessage());
        }
    }

	void startProcedure(Token identifierToken, List<FormalParameter> formalParameters) {
        String identifier = this.getIdentifierFromToken(identifierToken);
        try {
            lexicalScope.registerProcedureInterface(identifier, formalParameters);
            lexicalScope = new LexicalScope(lexicalScope, identifier);
            addParameterIdentifiersToLexicalScope(formalParameters);
        } catch (LexicalException e) {
            parser.SemErr(e.getMessage());
        }
	}

	void startFunction(Token identifierToken, List<FormalParameter> formalParameters, Token returnTypeToken) {
        String identifier = this.getIdentifierFromToken(identifierToken);
        String returnType = this.getIdentifierFromToken(returnTypeToken);
        try {
            lexicalScope.registerFunctionInterface(identifier, formalParameters, returnType);
            lexicalScope = new LexicalScope(lexicalScope, identifier, returnType);
            addParameterIdentifiersToLexicalScope(formalParameters);
        } catch (LexicalException e) {
            parser.SemErr(e.getMessage());
        }
    }

    void appendFormalParameter(List<FormalParameter> parameter, List<FormalParameter> params) {
        params.addAll(parameter);
    }

    List<FormalParameter> createFormalParametersList(List<String> identifiers, String typeName, boolean isOutput) {
        List<FormalParameter> paramList = new ArrayList<>();
        for (String identifier : identifiers) {
            paramList.add(new FormalParameter(identifier, typeName, isOutput));
        }

        return paramList;
    }

    void finishProcedure() {
        finishSubroutine();
    }

    void finishFunction() {
        finishSubroutine();
    }

    void finishProcedure(StatementNode bodyNode) {
        StatementNode subroutineNode = createSubroutineNode(bodyNode);
        final ProcedureBodyNode procedureBodyNode = new ProcedureBodyNode(subroutineNode);
        finishSubroutine(procedureBodyNode);
    }

    void finishFunction(StatementNode bodyNode) {
        StatementNode subroutineNode = createSubroutineNode(bodyNode);
        final FunctionBodyNode functionBodyNode = FunctionBodyNodeGen.create(subroutineNode, lexicalScope.getReturnSlot());
        finishSubroutine(functionBodyNode);
    }

    void startLoop() {
        lexicalScope.increaseLoopDepth();
    }

    StatementNode createForLoop(boolean ascending, Token variableToken, ExpressionNode startValue, ExpressionNode finalValue, StatementNode loopBody) {
        String iteratingIdentifier = this.getIdentifierFromToken(variableToken);
        FrameSlot iteratingSlot = lexicalScope.getLocalSlot(iteratingIdentifier);
        if (iteratingSlot == null) {
            parser.SemErr("Unknown identifier: " + iteratingIdentifier);
        }
        return new ForNode(ascending, iteratingSlot, startValue, finalValue, loopBody);
    }

    StatementNode createRepeatLoop(ExpressionNode condition, StatementNode loopBody) {
        return new RepeatNode(condition, loopBody);
    }

    StatementNode createWhileLoop(ExpressionNode condition, StatementNode loopBody) {
        return new WhileNode(condition, loopBody);
    }

    StatementNode createBreak() {
        // TODO: check if TurboPascal standard is set
        if (!lexicalScope.isInLoop()) {
            parser.SemErr("Break outside a loop: ");
        }
        return new BreakNode();
    }

	void finishLoop() {
        try {
            lexicalScope.decreaseLoopDepth();
        } catch (LexicalException e) {
            parser.SemErr(e.getMessage());
        }
    }

    StatementNode createIfStatement(ExpressionNode condition, StatementNode thenNode, StatementNode elseNode) {
        return new IfNode(condition, thenNode, elseNode);
    }

    CaseNode createCaseStatement(CaseStatementData data) {
        ExpressionNode[] indexes = data.indexNodes.toArray(new ExpressionNode[data.indexNodes.size()]);
        StatementNode[] statements = data.statementNodes.toArray(new StatementNode[data.statementNodes.size()]);

        return new CaseNode(data.caseExpression, indexes, statements, data.elseNode);
    }

    StatementNode createNopStatement() {
        return new NopNode();
    }

    ExpressionNode createBinaryExpression(Token operator, ExpressionNode leftNode, ExpressionNode rightNode) {
        switch (operator.val.toLowerCase()) {

            // arithmetic
            case "+":
                return AddNodeGen.create(leftNode, rightNode);
            case "-":
                return SubstractNodeGen.create(leftNode, rightNode);
            case "*":
                return MultiplyNodeGen.create(leftNode, rightNode);
            case "/":
                return DivideNodeGen.create(leftNode, rightNode);
            case "div":
                return DivideIntegerNodeGen.create(leftNode, rightNode);
            case "mod":
                return ModuloNodeGen.create(leftNode, rightNode);

            // logic
            case "and":
                return AndNodeGen.create(leftNode, rightNode);
            case "or":
                return OrNodeGen.create(leftNode, rightNode);

            case "<":
                return LessThanNodeGen.create(leftNode, rightNode);
            case "<=":
                return LessThanOrEqualNodeGen.create(leftNode, rightNode);
            case ">":
                return NotNodeGen.create(LessThanOrEqualNodeGen.create(leftNode, rightNode));
            case ">=":
                return NotNodeGen.create(LessThanNodeGen.create(leftNode, rightNode));
            case "=":
                return EqualsNodeGen.create(leftNode, rightNode);
            case "<>":
                return NotNodeGen.create(EqualsNodeGen.create(leftNode, rightNode));

            default:
                parser.SemErr("Unknown binary operator: " + operator.val);
                return null;
        }
    }

    ExpressionNode createUnaryExpression(Token operator, ExpressionNode son) {
        switch (operator.val) {
            case "+":
                return son;
            case "-":
                return NegationNodeGen.create(son);
            case "not":
                return NotNodeGen.create(son);
            default:
                parser.SemErr("Unexpected unary operator: " + operator.val);
                return null;
        }
    }

    ExpressionNode createAssignment(Token identifierToken, ExpressionNode valueNode) {
        String variableIdentifier = this.getIdentifierFromToken(identifierToken);

        LexicalScope ls = this.lexicalScope;
        while (ls != null) {
            if (lexicalScope.containsLocalIdentifier(variableIdentifier)) {
                if (!lexicalScope.isVariable(variableIdentifier)) {
                    parser.SemErr("Assignment target is not a variable");
                    return null;
                } else {
                    FrameSlot frameSlot = lexicalScope.getLocalSlot(variableIdentifier);
                    return AssignmentNodeGen.create(valueNode, frameSlot);
                }
            } else {
                ls = ls.getOuterScope();
            }
        }

        parser.SemErr("Assignment target is an unknown identifier");
        return null;
    }

    ExpressionNode createExpressionFromSingleIdentifier(Token identifierToken) {
        String identifier = this.getIdentifierFromToken(identifierToken);

        LexicalScope ls = this.lexicalScope;
        while (ls != null) {
            if (ls.containsLocalIdentifier(identifier)){
                if (ls.isVariable(identifier) || ls.isConstant(identifier)) {
                    return ReadVariableNodeGen.create(ls.getLocalSlot(identifier));
                } else if (ls.isParameterlessSubroutine(identifier)) {
                    PascalContext context = ls.getContext();
                    ExpressionNode literal = new FunctionLiteralNode(context, identifier);
                    return this.createCall(literal, new ArrayList<>());
                } else {
                    parser.SemErr(identifier + " is not an expression");
                    return null;
                }
            } else {
                ls = ls.getOuterScope();
            }
        }

        parser.SemErr("Unknown identifier: " + identifier);
        return null;
    }

    ExpressionNode createCall(ExpressionNode functionLiteral, List<ExpressionNode> params) {
        return InvokeNodeGen.create(params.toArray(new ExpressionNode[params.size()]), functionLiteral);
    }

    ExpressionNode createFunctionLiteralNode(Token identifierToken) {
        String identifier = this.getIdentifierFromToken(identifierToken);

        LexicalScope ls = this.lexicalScope;
        while (ls != null){
            if (ls.containsLocalIdentifier(identifier)) {
                if (!ls.isSubroutine(identifier)) {
                    parser.SemErr(identifier + " is not a subroutine");
                    return null;
                } else {
                    return new FunctionLiteralNode(ls.getContext(), identifier);
                }
            } else {
                ls = ls.getOuterScope();
            }
        }

        parser.SemErr("Undefined subroutine: " + identifier);
        return null;
    }

    ExpressionNode createReadArrayValue(Token identifierToken, List<ExpressionNode> indexingNodes) {
        String identifier = this.getIdentifierFromToken(identifierToken);

        return ReadArrayIndexNodeGen.create(indexingNodes.toArray(new ExpressionNode[indexingNodes.size()]),
                lexicalScope.getLocalSlot(identifier));
    }

    ExpressionNode createArrayIndexAssignment(Token identifierToken, List<ExpressionNode> indexingNodes, ExpressionNode valueNode) {
        String identifier = this.getIdentifierFromToken(identifierToken);

        return new ArrayIndexAssignmentNode(lexicalScope.getLocalSlot(identifier),
                indexingNodes.toArray(new ExpressionNode[indexingNodes.size()]), valueNode);
    }

    ExpressionNode createLogicLiteral(boolean value) {
        return new LogicLiteralNode(value);
    }

    ExpressionNode createNumericLiteral(Token literalToken) {
        try {
            return new LongLiteralNode(createLongFromToken(literalToken));
        } catch (LexicalException e) {
            parser.SemErr(e.getMessage());
            return new LongLiteralNode(0);
        }
    }

    ExpressionNode createFloatLiteral(Token token) {
        double value = Float.parseFloat(token.val);
        return new DoubleLiteralNode(value);
    }

    ExpressionNode createCharOrStringLiteral(String literal) {
        return (literal.length() == 1) ? new CharLiteralNode(literal.charAt(0)) : new StringLiteralNode(literal);
    }

    StatementNode createBlockNode(List<StatementNode> bodyNodes) {
        return new BlockNode(bodyNodes.toArray(new StatementNode[bodyNodes.size()]));
    }

    // TODO: this main node can be in lexical scope instead of a parser
    PascalRootNode finishMainFunction(StatementNode blockNode) {
        StatementNode bodyNode = this.createSubroutineNode(blockNode);
        return new PascalRootNode(lexicalScope.getFrameDescriptor(), new ProcedureBodyNode(bodyNode));
    }

    String createStringFromToken(Token t) {
        String literal = t.val;
        literal = literal.substring(1, literal.length() - 1);
        literal = literal.replaceAll("''", "'");
        return literal;
    }

    private long createLongFromToken(Token token) throws LexicalException {
        try {
            return Long.parseLong(token.val);
        } catch (NumberFormatException e) {
            throw new LexicalException("Integer literal out of range");
        }
    }

	private String getIdentifierFromToken(Token identifier) {
        return identifier.val.toLowerCase();
    }

    private StatementNode createSubroutineNode(StatementNode bodyNode) {
        // TODO: syntax tree by vyzeral lepsie keby z initialization nodes je BlockNode
        List<StatementNode> subroutineNodes = new ArrayList<>();
        try {
            subroutineNodes = lexicalScope.createInitializationNodes();
        } catch (LexicalException e) {
            parser.SemErr(e.getMessage());
        }
        subroutineNodes.add(bodyNode);

        return new BlockNode(subroutineNodes.toArray(new StatementNode[lexicalScope.scopeNodes.size()]));
    }

    private void addParameterIdentifiersToLexicalScope(List<FormalParameter> parameters) {
        try {
            for (FormalParameter parameter : parameters) {
                this.lexicalScope.registerLocalVariable(parameter.identifier, parameter.type);
            }
        } catch (LexicalException e) {
            parser.SemErr(e.getMessage());
        }
    }

    private void finishSubroutine() {
        lexicalScope = lexicalScope.getOuterScope();
    }

    private void finishSubroutine(ExpressionNode subroutineBodyNode) {
        final PascalRootNode rootNode = new PascalRootNode(lexicalScope.getFrameDescriptor(), subroutineBodyNode);

        String subroutineIdentifier = lexicalScope.getName();
        lexicalScope = lexicalScope.getOuterScope();
        lexicalScope.getContext().setSubroutineRootNode(subroutineIdentifier, rootNode);
    }

	// ------------------------------------------------------

	public StatementNode createRandomizeNode() {
		return new RandomizeBuiltinNode(lexicalScope.getContext());
	}

	public ExpressionNode createRandomNode() {
		return new RandomBuiltinNode(lexicalScope.getContext());
	}

	public ExpressionNode createRandomNode(Token numericLiteral) {
		return new RandomBuiltinNode(lexicalScope.getContext(), Long.parseLong(numericLiteral.val));
	}

	public StatementNode createReadLine() {
		return new ReadlnBuiltinNode(lexicalScope.getContext());
	}

	public StatementNode createReadLine(List<String> identifiers){
		FrameSlot[] slots = new FrameSlot[identifiers.size()];
		for(int i = 0; i < slots.length; i++) {
			String currentIdentifier = identifiers.get(i);
			slots[i] = lexicalScope.getLocalSlot(currentIdentifier);
			if(slots[i] == null) {
				parser.SemErr("UnknownDescriptor identifier: " + currentIdentifier + ".");
			}
		}

		ReadlnBuiltinNode readln = new ReadlnBuiltinNode(lexicalScope.getContext(), slots);
		return readln;
	}

	public void importUnit(Token unitToken) {
		String importingUnit = unitToken.val.toLowerCase();

		if (!units.containsKey(importingUnit)) {
			parser.SemErr("UnknownDescriptor unit. Did you forget to import it to compiler? - " + importingUnit);
			return;
		}

		Unit unit = units.get(importingUnit);

		// functions
		PascalSubroutineRegistry fRegistry = unit.getContext().getGlobalFunctionRegistry();
		lexicalScope.getContext().getGlobalFunctionRegistry().addAll(fRegistry);

		// custom types
		for(String typeIdentifier : unit.getLexicalScope().getAllCustomTypes().keySet()){
			ICustomType custom = unit.getLexicalScope().getAllCustomTypes().get(typeIdentifier);
			if(custom.isGlobal()){
				lexicalScope.registerCustomType(typeIdentifier, custom);
			}
		}
	}

    boolean containsIdentifier(String identifier) {
        return this.lexicalScope.containsLocalIdentifier(identifier);
    }

	/*****************************************************************************
	 * UNIT SECTION
	 *****************************************************************************/

	public void startUnit(Token t) {
		String unitName = t.val.toLowerCase();

		if (units.containsValue(t.val.toLowerCase())) {
			parser.SemErr("Unit with name " + unitName + " is already defined.");
			return;
		}

		currentUnit = new Unit(unitName);
		this.units.put(unitName, currentUnit);
        this.lexicalScope = currentUnit.getLexicalScope();
	}

	public void endUnit() {
		currentUnit = null;
        this.lexicalScope = null;
	}

	public void addProcedureInterface(Token name, List<FormalParameter> formalParameters) {
		if(currentUnit == null) {
			lexicalScope.getContext().getGlobalFunctionRegistry().registerSubroutineName(name.val.toLowerCase());
            this.lexicalScope = this.lexicalScope.getOuterScope();
		} else if (!currentUnit.addProcedureInterface(name.val.toLowerCase(), formalParameters)) {
			parser.SemErr("Subroutine with this name is already defined: " + name);
		}
	}

	public void addFunctionInterface(Token name, List<FormalParameter> formalParameters, String returnType) {
		if(currentUnit == null) {
			lexicalScope.getContext().getGlobalFunctionRegistry().registerSubroutineName(name.val.toLowerCase());
		} else if (!currentUnit.addFunctionInterface(name.val.toLowerCase(), formalParameters, returnType)) {
			parser.SemErr("Subroutine with this name is already defined: " + name);
		}
	}

	public void leaveUnitInterfaceSection(){
		assert currentUnit != null;
		currentUnit.leaveInterfaceSection();
	}
}
