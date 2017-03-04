
package cz.cuni.mff.d3s.trupple.language.parser.wirth;

import com.oracle.truffle.api.nodes.RootNode;
import cz.cuni.mff.d3s.trupple.language.nodes.*;
import cz.cuni.mff.d3s.trupple.language.parser.*;
import cz.cuni.mff.d3s.trupple.language.parser.identifierstable.types.OrdinalDescriptor;
import cz.cuni.mff.d3s.trupple.language.parser.identifierstable.types.TypeDescriptor;
import com.oracle.truffle.api.source.Source;
import java.util.ArrayList;
import java.util.List;

public class Parser implements IParser {
	public static final int _EOF = 0;
	public static final int _identifier = 1;
	public static final int _stringLiteral = 2;
	public static final int _integerLiteral = 3;
	public static final int _doubleLiteral = 4;
	public static final int maxT = 59;

	static final boolean _T = true;
	static final boolean _x = false;
	static final int minErrDist = 2;

	public Token t;    // last recognized token
	public Token la;   // lookahead token
	int errDist = minErrDist;
	
	public Scanner scanner;
	public Errors errors;
	private final NodeFactory factory;
    public PascalRootNode mainNode;

	

	public Parser() {
		this.factory = new NodeFactory(this);
		errors = new Errors();
	}

	void SynErr (int n) {
		if (errDist >= minErrDist) errors.SynErr(la.line, la.col, n);
		errDist = 0;
	}

	public void SemErr (String msg) {
		if (errDist >= minErrDist) errors.SemErr(t.line, t.col, msg);
		errDist = 0;
	}
	
	void Get () {
		for (;;) {
			t = la;
			la = scanner.Scan();
			if (la.kind <= maxT) {
				++errDist;
				break;
			}

			la = t;
		}
	}
	
	void Expect (int n) {
		if (la.kind==n) Get(); else { SynErr(n); }
	}
	
	boolean StartOf (int s) {
		return set[s][la.kind];
	}
	
	void ExpectWeak (int n, int follow) {
		if (la.kind == n) Get();
		else {
			SynErr(n);
			while (!StartOf(follow)) Get();
		}
	}
	
	boolean WeakSeparator (int n, int syFol, int repFol) {
		int kind = la.kind;
		if (kind == n) { Get(); return true; }
		else if (StartOf(repFol)) return false;
		else {
			SynErr(n);
			while (!(set[syFol][kind] || set[repFol][kind] || set[0][kind])) {
				Get();
				kind = la.kind;
			}
			return StartOf(syFol);
		}
	}
	
	void Pascal() {
		Program();
		Declarations();
		MainFunction();
	}

	void Program() {
		Expect(5);
		Expect(1);
		factory.startPascal(t); 
		Expect(6);
	}

	void Declarations() {
		if (la.kind == 21) {
			ConstantDefinitions();
		}
		if (la.kind == 7) {
			TypeDefinitions();
		}
		if (la.kind == 22) {
			VariableDeclarations();
		}
		while (la.kind == 24 || la.kind == 26) {
			Subroutine();
		}
	}

	void MainFunction() {
		StatementNode blockNode = Block();
		mainNode = factory.finishMainFunction(blockNode); 
		Expect(27);
	}

	void ConstantDefinitions() {
		Expect(21);
		ConstantDefinition();
		Expect(6);
		while (la.kind == 1) {
			ConstantDefinition();
			Expect(6);
		}
	}

	void TypeDefinitions() {
		Expect(7);
		TypeDefinition();
		Expect(6);
		while (la.kind == 1) {
			TypeDefinition();
			Expect(6);
		}
	}

	void VariableDeclarations() {
		Expect(22);
		VariableLineDeclaration();
		Expect(6);
		while (la.kind == 1) {
			VariableLineDeclaration();
			Expect(6);
		}
	}

	void Subroutine() {
		if (la.kind == 24) {
			Procedure();
			Expect(6);
		} else if (la.kind == 26) {
			Function();
			Expect(6);
		} else SynErr(60);
	}

	void TypeDefinition() {
		Expect(1);
		Token identifier = t; 
		Expect(8);
		TypeDescriptor typeDescriptor = Type();
		factory.registerNewType(identifier, typeDescriptor); 
	}

	TypeDescriptor  Type() {
		TypeDescriptor  typeDescriptor;
		typeDescriptor = null; 
		if (la.kind == 1) {
			Get();
			typeDescriptor = factory.getTypeDescriptor(t); 
		} else if (la.kind == 16) {
			typeDescriptor = EnumDefinition();
		} else if (la.kind == 11 || la.kind == 12) {
			List<OrdinalDescriptor> ordinalDimensions  = ArrayDefinition();
			while (continuesArray()) {
				Expect(9);
				List<OrdinalDescriptor> additionalDimensions  = ArrayDefinition();
				ordinalDimensions.addAll(additionalDimensions); 
			}
			Expect(9);
			Expect(1);
			typeDescriptor = factory.createArray(ordinalDimensions, t); 
		} else if (la.kind == 10) {
			Get();
			Expect(9);
			OrdinalDescriptor ordinal = Ordinal();
			typeDescriptor = factory.createSetType(ordinal); 
		} else SynErr(61);
		return typeDescriptor;
	}

	TypeDescriptor  EnumDefinition() {
		TypeDescriptor  typeDescriptor;
		List<String> enumIdentifiers = new ArrayList<>(); 
		Expect(16);
		Expect(1);
		enumIdentifiers.add(factory.getIdentifierFromToken(t)); 
		while (la.kind == 14) {
			Get();
			Expect(1);
			enumIdentifiers.add(factory.getIdentifierFromToken(t)); 
		}
		Expect(17);
		typeDescriptor = factory.registerEnum(enumIdentifiers); 
		return typeDescriptor;
	}

	List<OrdinalDescriptor>  ArrayDefinition() {
		List<OrdinalDescriptor>  ordinalDimensions;
		if (la.kind == 11) {
			Get();
		}
		Expect(12);
		ordinalDimensions = new ArrayList<>(); 
		Expect(13);
		OrdinalDescriptor ordinalDescriptor = null; 
		ordinalDescriptor = Ordinal();
		ordinalDimensions.add(ordinalDescriptor); 
		while (la.kind == 14) {
			Get();
			ordinalDescriptor = Ordinal();
			ordinalDimensions.add(ordinalDescriptor); 
		}
		Expect(15);
		return ordinalDimensions;
	}

	OrdinalDescriptor  Ordinal() {
		OrdinalDescriptor  ordinal;
		ordinal = null; 
		if (la.kind == 3 || la.kind == 19 || la.kind == 20) {
			int lowerBound, upperBound; 
			lowerBound = SignedIntegerLiteral();
			Expect(18);
			upperBound = SignedIntegerLiteral();
			ordinal = factory.createSimpleOrdinalDescriptor(lowerBound, upperBound); 
		} else if (StartOf(1)) {
			TypeDescriptor typeDescriptor = Type();
			ordinal = factory.castTypeToOrdinalType(typeDescriptor); 
		} else SynErr(62);
		return ordinal;
	}

	int  SignedIntegerLiteral() {
		int  value;
		value = 0; 
		if (la.kind == 19) {
			Get();
			value = SignedIntegerLiteral();
		} else if (la.kind == 20) {
			Get();
			value = SignedIntegerLiteral();
			value = -value; 
		} else if (la.kind == 3) {
			Get();
			value = Integer.parseInt(t.val); 
		} else SynErr(63);
		return value;
	}

	void ConstantDefinition() {
		Expect(1);
		Token identifier = t; 
		Expect(8);
		if (StartOf(2)) {
			NumericConstant(identifier);
		} else if (la.kind == 2) {
			String value = StringLiteral();
			factory.registerStringOrCharConstant(identifier, value); 
		} else if (la.kind == 1 || la.kind == 19 || la.kind == 20) {
			IdentifierConstant(identifier);
		} else SynErr(64);
	}

	void NumericConstant(Token identifier) {
		if (la.kind == 19 || la.kind == 20) {
			if (la.kind == 19) {
				Get();
			} else {
				Get();
			}
			Token sign = t; 
			if (la.kind == 3) {
				Get();
				factory.registerSignedIntegerConstant(identifier, sign, t); 
			} else if (la.kind == 4) {
				Get();
				factory.registerSignedRealConstant(identifier, sign, t); 
			} else SynErr(65);
		} else if (la.kind == 3 || la.kind == 4) {
			if (la.kind == 3) {
				Get();
				factory.registerIntegerConstant(identifier, t); 
			} else {
				Get();
				factory.registerRealConstant(identifier, t); 
			}
		} else SynErr(66);
	}

	String  StringLiteral() {
		String  value;
		Expect(2);
		value = factory.createStringFromToken(t); 
		return value;
	}

	void IdentifierConstant(Token identifier) {
		if (la.kind == 19 || la.kind == 20) {
			if (la.kind == 19) {
				Get();
			} else {
				Get();
			}
			Token sign = t; 
			Expect(1);
			factory.registerSignedConstantFromIdentifier(identifier, sign, t); 
		} else if (la.kind == 1) {
			Get();
			factory.registerConstantFromIdentifier(identifier, t); 
		} else SynErr(67);
	}

	void VariableLineDeclaration() {
		List<String> identifiers = new ArrayList<>(); 
		Expect(1);
		identifiers.add(factory.getIdentifierFromToken(t)); 
		while (la.kind == 14) {
			Get();
			Expect(1);
			identifiers.add(factory.getIdentifierFromToken(t)); 
		}
		Expect(23);
		TypeDescriptor typeDescriptor = Type();
		factory.registerVariables(identifiers, typeDescriptor); 
	}

	void Procedure() {
		Expect(24);
		Expect(1);
		Token identifierToken = t; 
		List<FormalParameter> formalParameters = new ArrayList<>(); 
		if (la.kind == 16) {
			formalParameters = FormalParameterList();
		}
		Expect(6);
		if (la.kind == 25) {
			Get();
			factory.forwardProcedure(identifierToken, formalParameters); 
		} else if (StartOf(3)) {
			factory.startProcedureImplementation(identifierToken, formalParameters); 
			Declarations();
			StatementNode bodyNode = Block();
			factory.finishProcedureImplementation(bodyNode); 
		} else SynErr(68);
	}

	void Function() {
		Expect(26);
		Expect(1);
		Token identifierToken = t; 
		List<FormalParameter> formalParameters = new ArrayList<>(); 
		if (la.kind == 16) {
			formalParameters = FormalParameterList();
		}
		Expect(23);
		Expect(1);
		Token returnTypeToken = t; 
		Expect(6);
		if (la.kind == 25) {
			Get();
			factory.forwardFunction(identifierToken, formalParameters, returnTypeToken); 
		} else if (StartOf(3)) {
			factory.startFunctionImplementation(identifierToken, formalParameters, returnTypeToken); 
			Declarations();
			StatementNode bodyNode = Block();
			factory.finishFunctionImplementation(bodyNode); 
		} else SynErr(69);
	}

	List<FormalParameter>  FormalParameterList() {
		List<FormalParameter>  formalParameters;
		Expect(16);
		formalParameters = new ArrayList<>(); 
		List<FormalParameter> newParameters = new ArrayList<>(); 
		newParameters = FormalParameter();
		factory.appendFormalParameter(newParameters, formalParameters); 
		while (la.kind == 6) {
			Get();
			newParameters = FormalParameter();
			factory.appendFormalParameter(newParameters, formalParameters); 
		}
		Expect(17);
		return formalParameters;
	}

	StatementNode  Block() {
		StatementNode  blockNode;
		Expect(28);
		List<StatementNode> bodyNodes = new ArrayList<>(); 
		if (StartOf(4)) {
			StatementSequence(bodyNodes);
		}
		Expect(29);
		blockNode = factory.createBlockNode(bodyNodes); 
		return blockNode;
	}

	List<FormalParameter>  FormalParameter() {
		List<FormalParameter>  formalParameter;
		List<String> identifiers = new ArrayList<>(); 
		Expect(1);
		identifiers.add(factory.getIdentifierFromToken(t)); 
		while (la.kind == 14) {
			Get();
			Expect(1);
			identifiers.add(factory.getIdentifierFromToken(t)); 
		}
		Expect(23);
		Expect(1);
		formalParameter = factory.createFormalParametersList(identifiers, factory.getTypeNameFromToken(t), false); 
		return formalParameter;
	}

	void StatementSequence(List<StatementNode> body ) {
		StatementNode statement = Statement();
		body.add(statement); 
		while (la.kind == 6) {
			Get();
			statement = Statement();
			body.add(statement); 
		}
	}

	StatementNode  Statement() {
		StatementNode  statement;
		statement = null; 
		switch (la.kind) {
		case 6: case 29: case 33: case 39: {
			statement = factory.createNopStatement(); 
			break;
		}
		case 41: {
			statement = IfStatement();
			break;
		}
		case 34: {
			statement = ForLoop();
			break;
		}
		case 40: {
			statement = WhileLoop();
			break;
		}
		case 38: {
			statement = RepeatLoop();
			break;
		}
		case 32: {
			statement = CaseStatement();
			break;
		}
		case 28: {
			statement = Block();
			break;
		}
		case 1: {
			statement = IdentifierBeginningStatement();
			break;
		}
		case 30: {
			Get();
			statement = factory.createRandomizeNode(); 
			break;
		}
		default: SynErr(70); break;
		}
		return statement;
	}

	StatementNode  IfStatement() {
		StatementNode  statement;
		Expect(41);
		ExpressionNode condition = Expression();
		Expect(42);
		StatementNode thenStatement = Statement();
		StatementNode elseStatement = null; 
		if (la.kind == 33) {
			Get();
			elseStatement = Statement();
		}
		statement = factory.createIfStatement(condition, thenStatement, elseStatement); 
		return statement;
	}

	StatementNode  ForLoop() {
		StatementNode  statement;
		factory.startLoop(); 
		Expect(34);
		boolean ascending = true; 
		Expect(1);
		Token variableToken = t; 
		Expect(31);
		ExpressionNode startValue = Expression();
		if (la.kind == 35) {
			Get();
			ascending = true; 
		} else if (la.kind == 36) {
			Get();
			ascending = false; 
		} else SynErr(71);
		ExpressionNode finalValue = Expression();
		Expect(37);
		StatementNode loopBody = Statement();
		statement = factory.createForLoop(ascending, variableToken, startValue, finalValue, loopBody); 
		factory.finishLoop(); 
		return statement;
	}

	StatementNode  WhileLoop() {
		StatementNode  statement;
		factory.startLoop(); 
		Expect(40);
		ExpressionNode condition = Expression();
		Expect(37);
		StatementNode loopBody = Statement();
		statement = factory.createWhileLoop(condition, loopBody); 
		factory.finishLoop(); 
		return statement;
	}

	StatementNode  RepeatLoop() {
		StatementNode  statement;
		factory.startLoop(); 
		Expect(38);
		List<StatementNode> bodyNodes = new ArrayList<>(); 
		StatementSequence(bodyNodes);
		Expect(39);
		ExpressionNode condition = Expression();
		statement = factory.createRepeatLoop(condition, factory.createBlockNode(bodyNodes)); 
		factory.finishLoop(); 
		return statement;
	}

	StatementNode  CaseStatement() {
		StatementNode  statement;
		Expect(32);
		ExpressionNode caseExpression = Expression();
		Expect(9);
		CaseStatementData caseData = CaseList();
		caseData.caseExpression = caseExpression; 
		Expect(29);
		statement = factory.createCaseStatement(caseData); 
		return statement;
	}

	StatementNode  IdentifierBeginningStatement() {
		StatementNode  statement;
		statement = null; 
		Expect(1);
		Token identifierToken = t; 
		if (StartOf(5)) {
			statement = factory.createSubroutineCall(identifierToken, new ArrayList<ExpressionNode>()); 
		} else if (la.kind == 16) {
			statement = SubroutineCall(identifierToken);
		} else if (la.kind == 13 || la.kind == 31) {
			statement = AssignmentAfterIdentifierPart(identifierToken);
		} else SynErr(72);
		return statement;
	}

	ExpressionNode  SubroutineCall(Token identifierToken) {
		ExpressionNode  expression;
		Expect(16);
		List<ExpressionNode> parameters = new ArrayList<>(); 
		if (StartOf(6)) {
			parameters = ActualParameters(identifierToken);
		}
		Expect(17);
		expression = factory.createSubroutineCall(identifierToken, parameters); 
		return expression;
	}

	StatementNode  AssignmentAfterIdentifierPart(Token identifierToken) {
		StatementNode  statement;
		List<ExpressionNode> indexNodes = null; 
		if (la.kind == 13) {
			indexNodes = ArrayIndex();
		}
		Expect(31);
		ExpressionNode value = Expression();
		statement = (indexNodes == null)? factory.createAssignment(identifierToken, value) : factory.createArrayIndexAssignment(identifierToken, indexNodes, value); 
		return statement;
	}

	List<ExpressionNode>  ArrayIndex() {
		List<ExpressionNode>  indexNodes;
		indexNodes = new ArrayList<>(); 
		ExpressionNode indexingNode = null; 
		Expect(13);
		indexingNode = Expression();
		indexNodes.add(indexingNode); 
		while (la.kind == 14) {
			Get();
			indexingNode = Expression();
			indexNodes.add(indexingNode); 
		}
		Expect(15);
		return indexNodes;
	}

	ExpressionNode  Expression() {
		ExpressionNode  expression;
		expression = LogicTerm();
		while (la.kind == 43) {
			Get();
			Token op = t; 
			ExpressionNode right = LogicTerm();
			expression = factory.createBinaryExpression(op, expression, right); 
		}
		return expression;
	}

	CaseStatementData  CaseList() {
		CaseStatementData  data;
		data = new CaseStatementData(); 
		ExpressionNode caseConstant = Expression();
		data.indexNodes.add(caseConstant); 
		Expect(23);
		StatementNode caseStatement = Statement();
		data.statementNodes.add(caseStatement); 
		while (!caseEnds()) {
			Expect(6);
			caseConstant = Expression();
			data.indexNodes.add(caseConstant); 
			Expect(23);
			caseStatement = Statement();
			data.statementNodes.add(caseStatement); 
		}
		if (la.kind == 6) {
			Get();
		}
		if (la.kind == 33) {
			Get();
			data.elseNode = Statement();
		}
		if (la.kind == 6) {
			Get();
		}
		return data;
	}

	ExpressionNode  LogicTerm() {
		ExpressionNode  expression;
		expression = SignedLogicFactor();
		while (la.kind == 44) {
			Get();
			Token op = t; 
			ExpressionNode right = SignedLogicFactor();
			expression = factory.createBinaryExpression(op, expression, right); 
		}
		return expression;
	}

	ExpressionNode  SignedLogicFactor() {
		ExpressionNode  expression;
		expression = null; 
		if (la.kind == 45) {
			Get();
			Token op = t; 
			ExpressionNode right = SignedLogicFactor();
			expression = factory.createUnaryExpression(op, right); 
		} else if (StartOf(7)) {
			expression = LogicFactor();
		} else SynErr(73);
		return expression;
	}

	ExpressionNode  LogicFactor() {
		ExpressionNode  expression;
		expression = Arithmetic();
		if (StartOf(8)) {
			switch (la.kind) {
			case 46: {
				Get();
				break;
			}
			case 47: {
				Get();
				break;
			}
			case 48: {
				Get();
				break;
			}
			case 49: {
				Get();
				break;
			}
			case 8: {
				Get();
				break;
			}
			case 50: {
				Get();
				break;
			}
			case 51: {
				Get();
				break;
			}
			}
			Token op = t; 
			ExpressionNode right = Arithmetic();
			expression = factory.createBinaryExpression(op, expression, right); 
		}
		return expression;
	}

	ExpressionNode  Arithmetic() {
		ExpressionNode  expression;
		expression = Term();
		while (la.kind == 19 || la.kind == 20) {
			if (la.kind == 19) {
				Get();
			} else {
				Get();
			}
			Token op = t; 
			ExpressionNode right = Term();
			expression = factory.createBinaryExpression(op, expression, right); 
		}
		return expression;
	}

	ExpressionNode  Term() {
		ExpressionNode  expression;
		expression = SignedFactor();
		while (StartOf(9)) {
			if (la.kind == 52) {
				Get();
			} else if (la.kind == 53) {
				Get();
			} else if (la.kind == 54) {
				Get();
			} else {
				Get();
			}
			Token op = t; 
			ExpressionNode right = SignedFactor();
			expression = factory.createBinaryExpression(op, expression, right); 
		}
		return expression;
	}

	ExpressionNode  SignedFactor() {
		ExpressionNode  expression;
		expression = null; 
		if (la.kind == 19 || la.kind == 20) {
			if (la.kind == 19) {
				Get();
			} else {
				Get();
			}
			Token unOp = t; 
			expression = SignedFactor();
			expression = factory.createUnaryExpression(unOp, expression); 
		} else if (StartOf(10)) {
			expression = Factor();
		} else SynErr(74);
		return expression;
	}

	ExpressionNode  Factor() {
		ExpressionNode  expression;
		expression = null; 
		switch (la.kind) {
		case 58: {
			expression = Random();
			break;
		}
		case 1: {
			Get();
			if (la.kind == 13 || la.kind == 16) {
				expression = MemberExpression(t);
			} else if (StartOf(11)) {
				expression = factory.createExpressionFromSingleIdentifier(t); 
			} else SynErr(75);
			break;
		}
		case 16: {
			Get();
			expression = Expression();
			Expect(17);
			break;
		}
		case 2: {
			String value = ""; 
			value = StringLiteral();
			expression = factory.createCharOrStringLiteral(value); 
			break;
		}
		case 4: {
			Get();
			expression = factory.createFloatLiteral(t); 
			break;
		}
		case 3: {
			Get();
			expression = factory.createNumericLiteral(t); 
			break;
		}
		case 56: case 57: {
			boolean val; 
			val = LogicLiteral();
			expression = factory.createLogicLiteral(val); 
			break;
		}
		case 13: {
			expression = SetConstructor();
			break;
		}
		default: SynErr(76); break;
		}
		return expression;
	}

	ExpressionNode  Random() {
		ExpressionNode  expression;
		Expect(58);
		expression = null; 
		if (StartOf(11)) {
			expression = factory.createRandomNode(); 
		} else if (la.kind == 16) {
			Get();
			if (la.kind == 17) {
				Get();
				expression = factory.createRandomNode(); 
			} else if (la.kind == 3) {
				Get();
				expression = factory.createRandomNode(t); 
				Expect(17);
			} else SynErr(77);
		} else SynErr(78);
		return expression;
	}

	ExpressionNode  MemberExpression(Token identifierName) {
		ExpressionNode  expression;
		expression = null; 
		if (la.kind == 16) {
			expression = SubroutineCall(identifierName);
		} else if (la.kind == 13) {
			List<ExpressionNode> indexNodes  = ArrayIndex();
			expression = factory.createReadArrayValue(identifierName, indexNodes); 
		} else SynErr(79);
		return expression;
	}

	boolean  LogicLiteral() {
		boolean  result;
		result = false; 
		if (la.kind == 56) {
			Get();
			result = true; 
		} else if (la.kind == 57) {
			Get();
			result = false; 
		} else SynErr(80);
		return result;
	}

	ExpressionNode  SetConstructor() {
		ExpressionNode  expression;
		expression = null; 
		Expect(13);
		if (la.kind == 15) {
			expression = factory.createSetConstructorNode(new ArrayList<>()); 
		} else if (StartOf(6)) {
			List<ExpressionNode> valueNodes = new ArrayList<ExpressionNode>(); 
			ExpressionNode valueNode = Expression();
			valueNodes.add(valueNode); 
			while (la.kind == 14) {
				Get();
				valueNode = Expression();
				valueNodes.add(valueNode); 
			}
			expression = factory.createSetConstructorNode(valueNodes); 
		} else SynErr(81);
		Expect(15);
		return expression;
	}

	List<ExpressionNode>  ActualParameters(Token subroutineToken ) {
		List<ExpressionNode>  parameters;
		parameters = new ArrayList<>(); 
		int currentParameter = 0; 
		ExpressionNode parameter = ActualParameter(currentParameter, subroutineToken);
		parameters.add(parameter); 
		while (la.kind == 14) {
			Get();
			parameter = ActualParameter(++currentParameter, subroutineToken);
			parameters.add(parameter); 
		}
		return parameters;
	}

	ExpressionNode  ActualParameter(int currentParameterIndex, Token subroutineToken) {
		ExpressionNode  parameter;
		parameter = null; 
		if (factory.shouldBeReference(subroutineToken, currentParameterIndex)) {
			Expect(1);
			parameter = factory.createReferenceNode(t); 
		} else if (StartOf(6)) {
			parameter = Expression();
		} else SynErr(82);
		return parameter;
	}



	public void Parse(Source source) {
		this.scanner = new Scanner(source.getInputStream());
		la = new Token();
		la.val = "";		
		Get();
		Pascal();
		Expect(0);

	}

	private static final boolean[][] set = {
		{_T,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x},
		{_x,_T,_x,_x, _x,_x,_x,_x, _x,_x,_T,_T, _T,_x,_x,_x, _T,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x},
		{_x,_x,_x,_T, _T,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_T, _T,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x},
		{_x,_x,_x,_x, _x,_x,_x,_T, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_T,_T,_x, _T,_x,_T,_x, _T,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x},
		{_x,_T,_x,_x, _x,_x,_T,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _T,_T,_T,_x, _T,_x,_T,_x, _x,_x,_T,_x, _T,_T,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x},
		{_x,_x,_x,_x, _x,_x,_T,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_T,_x,_x, _x,_T,_x,_x, _x,_x,_x,_T, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x},
		{_x,_T,_T,_T, _T,_x,_x,_x, _x,_x,_x,_x, _x,_T,_x,_x, _T,_x,_x,_T, _T,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_T,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _T,_T,_T,_x, _x},
		{_x,_T,_T,_T, _T,_x,_x,_x, _x,_x,_x,_x, _x,_T,_x,_x, _T,_x,_x,_T, _T,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _T,_T,_T,_x, _x},
		{_x,_x,_x,_x, _x,_x,_x,_x, _T,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_T,_T, _T,_T,_T,_T, _x,_x,_x,_x, _x,_x,_x,_x, _x},
		{_x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _T,_T,_T,_T, _x,_x,_x,_x, _x},
		{_x,_T,_T,_T, _T,_x,_x,_x, _x,_x,_x,_x, _x,_T,_x,_x, _T,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _x,_x,_x,_x, _T,_T,_T,_x, _x},
		{_x,_x,_x,_x, _x,_x,_T,_x, _T,_T,_x,_x, _x,_x,_T,_T, _x,_T,_x,_T, _T,_x,_x,_T, _x,_x,_x,_x, _x,_T,_x,_x, _x,_T,_x,_T, _T,_T,_x,_T, _x,_x,_T,_T, _T,_x,_T,_T, _T,_T,_T,_T, _T,_T,_T,_T, _x,_x,_x,_x, _x}

	};

	public boolean isUsingTPExtension() {
        return false;
    }
	
    public boolean hadErrors() {
        return errors.count > 0;
    }

    public RootNode getRootNode() {
        return this.mainNode;
    }

    public boolean caseEnds(){
    	if(la.val.toLowerCase().equals("end") && !t.val.toLowerCase().equals(":"))
    		return true;
    		
    	if(la.val.toLowerCase().equals("else"))
    		return true;
    		
    	else if(la.val.toLowerCase().equals(";")){
    		Token next = scanner.Peek();
    		return next.val.toLowerCase().equals("end") || next.val.toLowerCase().equals("else");
    	}
    	
    	return false;
    }
    
    public boolean isSingleIdentifier(){
    	Token next = scanner.Peek();
    	if(next.val.equals("]") && factory.containsIdentifier(la.val.toLowerCase()))
    		return true;

    	return false;
    }
    
    public boolean continuesArray(){
    	Token next = scanner.Peek();
    	if(next.val.toLowerCase().equals("array") || (next.val.toLowerCase().equals("packed")))
    		return true;

    	return false;
    }
} // end Parser

class Errors {
	public int count = 0;                                    // number of errors detected
	public java.io.PrintStream errorStream = System.err;     // error messages go to this stream
	public String errMsgFormat = "-- line {0} col {1}: {2}"; // 0=line, 1=column, 2=text
	
	protected void printMsg(int line, int column, String msg) {
		StringBuffer b = new StringBuffer(errMsgFormat);
		int pos = b.indexOf("{0}");
		if (pos >= 0) { b.delete(pos, pos+3); b.insert(pos, line); }
		pos = b.indexOf("{1}");
		if (pos >= 0) { b.delete(pos, pos+3); b.insert(pos, column); }
		pos = b.indexOf("{2}");
		if (pos >= 0) b.replace(pos, pos+3, msg);
		errorStream.println(b.toString());
	}
	
	public void SynErr (int line, int col, int n) {
		String s;
		switch (n) {
			case 0: s = "EOF expected"; break;
			case 1: s = "identifier expected"; break;
			case 2: s = "stringLiteral expected"; break;
			case 3: s = "integerLiteral expected"; break;
			case 4: s = "doubleLiteral expected"; break;
			case 5: s = "\"program\" expected"; break;
			case 6: s = "\";\" expected"; break;
			case 7: s = "\"type\" expected"; break;
			case 8: s = "\"=\" expected"; break;
			case 9: s = "\"of\" expected"; break;
			case 10: s = "\"set\" expected"; break;
			case 11: s = "\"packed\" expected"; break;
			case 12: s = "\"array\" expected"; break;
			case 13: s = "\"[\" expected"; break;
			case 14: s = "\",\" expected"; break;
			case 15: s = "\"]\" expected"; break;
			case 16: s = "\"(\" expected"; break;
			case 17: s = "\")\" expected"; break;
			case 18: s = "\"..\" expected"; break;
			case 19: s = "\"+\" expected"; break;
			case 20: s = "\"-\" expected"; break;
			case 21: s = "\"const\" expected"; break;
			case 22: s = "\"var\" expected"; break;
			case 23: s = "\":\" expected"; break;
			case 24: s = "\"procedure\" expected"; break;
			case 25: s = "\"forward\" expected"; break;
			case 26: s = "\"function\" expected"; break;
			case 27: s = "\".\" expected"; break;
			case 28: s = "\"begin\" expected"; break;
			case 29: s = "\"end\" expected"; break;
			case 30: s = "\"randomize\" expected"; break;
			case 31: s = "\":=\" expected"; break;
			case 32: s = "\"case\" expected"; break;
			case 33: s = "\"else\" expected"; break;
			case 34: s = "\"for\" expected"; break;
			case 35: s = "\"to\" expected"; break;
			case 36: s = "\"downto\" expected"; break;
			case 37: s = "\"do\" expected"; break;
			case 38: s = "\"repeat\" expected"; break;
			case 39: s = "\"until\" expected"; break;
			case 40: s = "\"while\" expected"; break;
			case 41: s = "\"if\" expected"; break;
			case 42: s = "\"then\" expected"; break;
			case 43: s = "\"or\" expected"; break;
			case 44: s = "\"and\" expected"; break;
			case 45: s = "\"not\" expected"; break;
			case 46: s = "\">\" expected"; break;
			case 47: s = "\">=\" expected"; break;
			case 48: s = "\"<\" expected"; break;
			case 49: s = "\"<=\" expected"; break;
			case 50: s = "\"<>\" expected"; break;
			case 51: s = "\"in\" expected"; break;
			case 52: s = "\"*\" expected"; break;
			case 53: s = "\"/\" expected"; break;
			case 54: s = "\"div\" expected"; break;
			case 55: s = "\"mod\" expected"; break;
			case 56: s = "\"true\" expected"; break;
			case 57: s = "\"false\" expected"; break;
			case 58: s = "\"random\" expected"; break;
			case 59: s = "??? expected"; break;
			case 60: s = "invalid Subroutine"; break;
			case 61: s = "invalid Type"; break;
			case 62: s = "invalid Ordinal"; break;
			case 63: s = "invalid SignedIntegerLiteral"; break;
			case 64: s = "invalid ConstantDefinition"; break;
			case 65: s = "invalid NumericConstant"; break;
			case 66: s = "invalid NumericConstant"; break;
			case 67: s = "invalid IdentifierConstant"; break;
			case 68: s = "invalid Procedure"; break;
			case 69: s = "invalid Function"; break;
			case 70: s = "invalid Statement"; break;
			case 71: s = "invalid ForLoop"; break;
			case 72: s = "invalid IdentifierBeginningStatement"; break;
			case 73: s = "invalid SignedLogicFactor"; break;
			case 74: s = "invalid SignedFactor"; break;
			case 75: s = "invalid Factor"; break;
			case 76: s = "invalid Factor"; break;
			case 77: s = "invalid Random"; break;
			case 78: s = "invalid Random"; break;
			case 79: s = "invalid MemberExpression"; break;
			case 80: s = "invalid LogicLiteral"; break;
			case 81: s = "invalid SetConstructor"; break;
			case 82: s = "invalid ActualParameter"; break;
			default: s = "error " + n; break;
		}
		printMsg(line, col, s);
		count++;
	}

	public void SemErr (int line, int col, String s) {	
		printMsg(line, col, s);
		count++;
	}
	
	public void SemErr (String s) {
		errorStream.println(s);
		count++;
	}
	
	public void Warning (int line, int col, String s) {	
		printMsg(line, col, s);
	}
	
	public void Warning (String s) {
		errorStream.println(s);
	}
} // Errors


class FatalError extends RuntimeException {
	public static final long serialVersionUID = 1L;
	public FatalError(String s) { super(s); }
}
