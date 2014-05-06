package miniJava.SyntaticAnalyzer;

import miniJava.AbstractSyntaxTrees.ArrayType;
import miniJava.AbstractSyntaxTrees.AssignStmt;
import miniJava.AbstractSyntaxTrees.BaseType;
import miniJava.AbstractSyntaxTrees.BinaryExpr;
import miniJava.AbstractSyntaxTrees.BlockStmt;
import miniJava.AbstractSyntaxTrees.BooleanLiteral;
import miniJava.AbstractSyntaxTrees.CallExpr;
import miniJava.AbstractSyntaxTrees.CallStmt;
import miniJava.AbstractSyntaxTrees.ClassDecl;
import miniJava.AbstractSyntaxTrees.ClassDeclList;
import miniJava.AbstractSyntaxTrees.ClassType;
import miniJava.AbstractSyntaxTrees.ExprList;
import miniJava.AbstractSyntaxTrees.Expression;
import miniJava.AbstractSyntaxTrees.FieldDecl;
import miniJava.AbstractSyntaxTrees.FieldDeclList;
import miniJava.AbstractSyntaxTrees.IdRef;
import miniJava.AbstractSyntaxTrees.Identifier;
import miniJava.AbstractSyntaxTrees.IfStmt;
import miniJava.AbstractSyntaxTrees.IndexedRef;
import miniJava.AbstractSyntaxTrees.IntLiteral;
import miniJava.AbstractSyntaxTrees.LiteralExpr;
import miniJava.AbstractSyntaxTrees.MemberDecl;
import miniJava.AbstractSyntaxTrees.MethodDecl;
import miniJava.AbstractSyntaxTrees.MethodDeclList;
import miniJava.AbstractSyntaxTrees.NewArrayExpr;
import miniJava.AbstractSyntaxTrees.NewObjectExpr;
import miniJava.AbstractSyntaxTrees.Operator;
import miniJava.AbstractSyntaxTrees.ParameterDecl;
import miniJava.AbstractSyntaxTrees.ParameterDeclList;
import miniJava.AbstractSyntaxTrees.QualifiedRef;
import miniJava.AbstractSyntaxTrees.RefExpr;
import miniJava.AbstractSyntaxTrees.Reference;
import miniJava.AbstractSyntaxTrees.Statement;
import miniJava.AbstractSyntaxTrees.StatementList;
import miniJava.AbstractSyntaxTrees.ThisRef;
import miniJava.AbstractSyntaxTrees.Type;
import miniJava.AbstractSyntaxTrees.TypeKind;
import miniJava.AbstractSyntaxTrees.UnaryExpr;
import miniJava.AbstractSyntaxTrees.VarDecl;
import miniJava.AbstractSyntaxTrees.VarDeclStmt;
import miniJava.AbstractSyntaxTrees.WhileStmt;
import miniJava.AbstractSyntaxTrees.Package;

/**
 * The parse class; parse the source file and detect syntax error.
 * 
 * @author hxing
 * 
 */
public class Parser {

	private Scanner lexicalAnalyzer;
	private Token currentToken;
	private SourcePosition previousSourcePosition;
	private ErrorReporter errorReporter;

	/**
	 * The constructor
	 * 
	 * @param lexer
	 *            : the scanner for scanning the source file
	 * @param reporter
	 *            : the error reporter for syntax error
	 */
	public Parser(Scanner lexer, ErrorReporter reporter) {
		lexicalAnalyzer = lexer;
		setPreviousSourcePosition(new SourcePosition());
		errorReporter = reporter;
	}

	public void accept(int tokenExpected) throws SyntaxError {
		if (currentToken.kind == tokenExpected) {
			setPreviousSourcePosition(currentToken.position);
			currentToken = lexicalAnalyzer.scan();
		} else {
			errorReporter.reportError("expected here "
					+ Token.spell(tokenExpected)
					+ " at line: "+currentToken.position.startPosition);
		}
	}

	/**
	 * Accept a token and read the next one
	 */
	public void acceptIt() {
		setPreviousSourcePosition(currentToken.position);
		currentToken = lexicalAnalyzer.scan();
	}

	/*
	 * start records the position of the start of a phrase. This is defined to
	 * be the position of the first character of the first token of the phrase.
	 */
	void start(SourcePosition position) {
		position.startPosition = currentToken.position.startPosition;
	}

	/*
	 * finish records the position of the end of a phrase. This is defined to be
	 * the position of the last character of the last token of the phrase.
	 */

	void finish(SourcePosition position) {
		position.finishPosition = previousSourcePosition.finishPosition;
	}

	/**
	 * Begin parse the program
	 * 
	 * @throws SyntaxError
	 */
	public Package parseProgram() throws SyntaxError {

		ClassDeclList classes = new ClassDeclList();
		currentToken = lexicalAnalyzer.scan();
		SourcePosition pack = new SourcePosition();
		start(pack);
		while (currentToken.kind == Token.CLASS) {
			SourcePosition classStart = new SourcePosition();
			start(classStart);
			acceptIt();
			String className = currentToken.spelling;
			accept(Token.IDENTIFIER);
			accept(Token.LCURLY);
			FieldDeclList fields = new FieldDeclList();
			MethodDeclList methods = new MethodDeclList();
			while (currentToken.kind == Token.PRIVATE
					|| currentToken.kind == Token.PUBLIC
					|| currentToken.kind == Token.STATIC
					|| currentToken.kind == Token.INT
					|| currentToken.kind == Token.VOID
					|| currentToken.kind == Token.BOOLEAN
					|| currentToken.kind == Token.IDENTIFIER) {

				parseDeclaration(fields, methods);

			}
			accept(Token.RCURLY);
			finish(classStart);
			ClassDecl c = new ClassDecl(className, fields, methods, classStart);
			classes.add(c);
		}
		accept(Token.EOT);
		finish(pack);
		Package p = new Package(classes, pack);
		return p;
	}

	/**
	 * Parse type
	 * 
	 * @return
	 * 
	 * @throws SyntaxError
	 */
	private Type parseType() throws SyntaxError {
		Type type = null;
		switch (currentToken.kind) {
		case Token.BOOLEAN:
		case Token.VOID:
			SourcePosition basePosition = new SourcePosition();
			start(basePosition);
			TypeKind kind = (currentToken.kind == Token.BOOLEAN) ? TypeKind.BOOLEAN
					: TypeKind.VOID;
			acceptIt();
			finish(basePosition);
			type = new BaseType(kind, basePosition);
			break;
		case Token.INT:
		case Token.IDENTIFIER:
			SourcePosition basePosition2 = new SourcePosition();
			start(basePosition2);
			TypeKind kind2 = (currentToken.kind == Token.INT) ? TypeKind.INT
					: TypeKind.CLASS;
			String name = currentToken.spelling;
			SourcePosition idPosition = currentToken.position;
			acceptIt();
			finish(basePosition2);
			if (kind2 == TypeKind.INT)
				type = new BaseType(kind2, basePosition2);
			else {
				Identifier id = new Identifier(name, idPosition);
				type = new ClassType(id, basePosition2);
			}
			if (currentToken.kind == Token.LBRACKET) {
				acceptIt();
				accept(Token.RBRACKET);
				finish(basePosition2);
				type = new ArrayType(type, basePosition2);
			}
			break;
		default:
			errorReporter.reportError("cannot start a type with: "
					+ currentToken.spelling);
		}
		return type;
	}

	/**
	 * Parse declaration
	 * 
	 * @throws SyntaxError
	 */
	private void parseDeclaration(FieldDeclList fields, MethodDeclList methods)
			throws SyntaxError {
		SourcePosition start = new SourcePosition();
		start(start);
		boolean isPrivate = false;
		boolean isStatic = false;
		Type type = null;
		switch (currentToken.kind) {
		case Token.PUBLIC:
		case Token.PRIVATE:
			isPrivate = (currentToken.kind == Token.PRIVATE) ? true : false;
			acceptIt();
			if (currentToken.kind == Token.STATIC) {
				isStatic = true;
				acceptIt();
			}
			type = parseType();
			break;
		case Token.STATIC:
			isStatic = true;
			acceptIt();
			type = parseType();
			break;
		default:
			type = parseType();
		}
		String name = currentToken.spelling;
		accept(Token.IDENTIFIER);
		if (currentToken.kind == Token.LPAREN) {
			finish(start);
			MemberDecl member = new FieldDecl(isPrivate, isStatic, type, name,
					start);
			ParameterDeclList param = new ParameterDeclList();
			acceptIt();
			if (currentToken.kind == Token.RPAREN) {
				acceptIt();
			} else {
				for (ParameterDecl p : parseParameterList()){
					param.add(p);
				}
				accept(Token.RPAREN);
			}
			accept(Token.LCURLY);
			StatementList statements = new StatementList();
			while (currentToken.kind == Token.IDENTIFIER
					|| currentToken.kind == Token.THIS
					|| currentToken.kind == Token.IF
					|| currentToken.kind == Token.WHILE
					|| currentToken.kind == Token.LCURLY
					|| currentToken.kind == Token.INT
					|| currentToken.kind == Token.BOOLEAN
					|| currentToken.kind == Token.VOID) {
				Statement statement = parseStatement();
				statements.add(statement);
			}
			Expression e = null;
			if (currentToken.kind == Token.RETURN) {
				acceptIt();
				e = parseExpression();
				accept(Token.SEMICOLON);
			}
			accept(Token.RCURLY);
			finish(start);
			methods.add(new MethodDecl(member, param, statements, e, start));
		} else {
			accept(Token.SEMICOLON);
			finish(start);
			fields.add(new FieldDecl(isPrivate, isStatic, type, name, start));
		}

	}

	/**
	 * Parse parameter list
	 * 
	 * @throws SyntaxError
	 */
	private ParameterDeclList parseParameterList() throws SyntaxError {
		ParameterDeclList l = new ParameterDeclList();
		SourcePosition p = new SourcePosition();
		start(p);
		Type type = parseType();
		String name = currentToken.spelling;
		accept(Token.IDENTIFIER);
		finish(p);
		l.add(new ParameterDecl(type, name, p));
		while (currentToken.kind == Token.COMMA) {
			acceptIt();
			SourcePosition p2 = new SourcePosition();
			start(p2);
			Type type2 = parseType();
			String name2 = currentToken.spelling;
			accept(Token.IDENTIFIER);
			finish(p2);
			l.add(new ParameterDecl(type2, name2, p2));
		}
		return l;
	}

	/**
	 * Parse reference
	 * 
	 * @throws SyntaxError
	 */
	private Reference parseReference() throws SyntaxError {
		Reference ref = null;
		SourcePosition position = new SourcePosition();
		switch (currentToken.kind) {
		case Token.THIS:
			start(position);
			acceptIt();
			ref = new ThisRef(position);
			finish(position);
			break;
		case Token.IDENTIFIER:
			start(position);
			String name = currentToken.spelling;
			acceptIt();
			finish(position);
			ref = new IdRef(new Identifier(name, position), position);
			if (currentToken.kind == Token.LBRACKET) {
				acceptIt();
				Expression e = parseExpression();
				accept(Token.RBRACKET);
				finish(position);
				ref = new IndexedRef(ref, e, position);
			}
			break;
		default:
			errorReporter.reportError("cannot start a reference with: "
					+ currentToken.spelling);
		}
		while (currentToken.kind == Token.DOT) {
			acceptIt();
			SourcePosition idPosition = currentToken.position;
			String name = currentToken.spelling;
			accept(Token.IDENTIFIER);
			finish(position);
			ref = new QualifiedRef(ref, new Identifier(name, idPosition),
					position);
			if (currentToken.kind == Token.LBRACKET) {
				acceptIt();
				Expression e = parseExpression();
				accept(Token.RBRACKET);
				finish(position);
				ref = new IndexedRef(ref, e, position);
			}
		}
		return ref;

	}

	/* A parse reference helper method */
	private Reference parseReference(Reference prev, SourcePosition pos)
			throws SyntaxError {

		while (currentToken.kind == Token.DOT) {
			acceptIt();
			SourcePosition idPosition = currentToken.position;
			String name = currentToken.spelling;
			accept(Token.IDENTIFIER);
			finish(pos);
			prev = new QualifiedRef(prev, new Identifier(name, idPosition), pos);
			if (currentToken.kind == Token.LBRACKET) {
				acceptIt();
				Expression e = parseExpression();
				accept(Token.RBRACKET);
				finish(pos);
				prev = new IndexedRef(prev, e, pos);
			}
		}
		return prev;
	}

	/**
	 * Parse argument list
	 * 
	 * @throws SyntaxError
	 */
	private ExprList parseArgumentList() throws SyntaxError {
		ExprList list = new ExprList();

		Expression p = parseExpression();
		list.add(p);
		while (currentToken.kind == Token.COMMA) {
			acceptIt();
			Expression p2 = parseExpression();
			list.add(p2);
		}
		return list;
	}

	/**
	 * Parse statement
	 * 
	 * @throws SyntaxError
	 */
	private Statement parseStatement() throws SyntaxError {
		Statement statement = null;
		switch (currentToken.kind) {
		case Token.IF:
			SourcePosition ifPosition = new SourcePosition();
			start(ifPosition);
			acceptIt();
			accept(Token.LPAREN);
			Expression e = parseExpression();
			accept(Token.RPAREN);
			Statement then = parseStatement();
			finish(ifPosition);
			Statement other = null;
			if (currentToken.kind == Token.ELSE) {
				acceptIt();
				other = parseStatement();
				finish(ifPosition);
			}
			statement = new IfStmt(e, then, other, ifPosition);
			break;
		case Token.WHILE:
			SourcePosition whilePosition = new SourcePosition();
			start(whilePosition);
			acceptIt();
			accept(Token.LPAREN);
			Expression w = parseExpression();
			accept(Token.RPAREN);
			Statement d = parseStatement();
			finish(whilePosition);
			statement = new WhileStmt(w, d, whilePosition);
			break;
		case Token.BOOLEAN:
		case Token.VOID:
			SourcePosition varSt = new SourcePosition();
			SourcePosition typePosition = currentToken.position;
			start(varSt);
			TypeKind kind = (currentToken.kind == Token.BOOLEAN) ? TypeKind.BOOLEAN
					: TypeKind.VOID;
			Type type = new BaseType(kind, typePosition);
			acceptIt();
			String name = currentToken.spelling;
			SourcePosition varDecl = new SourcePosition(
					typePosition.startPosition,
					currentToken.position.finishPosition);
			VarDecl v = new VarDecl(type, name, varDecl);
			accept(Token.IDENTIFIER);
			accept(Token.EQUAL);
			Expression assign = parseExpression();
			accept(Token.SEMICOLON);
			finish(varSt);
			statement = new VarDeclStmt(v, assign, varSt);
			break;
		case Token.INT:
			SourcePosition varDeclStPosition = new SourcePosition();
			SourcePosition intTypePosition = currentToken.position;
			start(varDeclStPosition);
			TypeKind k = TypeKind.INT;
			Type intType = new BaseType(k, intTypePosition);
			Type arrayType = null;
			acceptIt();
			if (currentToken.kind == Token.LBRACKET) {
				acceptIt();
				SourcePosition arrayPos = new SourcePosition(
						intTypePosition.startPosition,
						currentToken.position.finishPosition);
				arrayType = new ArrayType(intType, arrayPos);
				accept(Token.RBRACKET);
			}
			SourcePosition varDec = new SourcePosition(
					intTypePosition.startPosition,
					currentToken.position.finishPosition);
			VarDecl decl = (arrayType == null) ? new VarDecl(intType,
					currentToken.spelling, varDec) : new VarDecl(arrayType,
					currentToken.spelling, varDec);
			accept(Token.IDENTIFIER);
			accept(Token.EQUAL);
			Expression result = parseExpression();
			accept(Token.SEMICOLON);
			finish(varDeclStPosition);
			statement = new VarDeclStmt(decl, result, varDeclStPosition);
			break;
		case Token.LCURLY:
			SourcePosition blockStPosition = new SourcePosition();
			StatementList stList = new StatementList();
			start(blockStPosition);
			acceptIt();
			while (currentToken.kind == Token.IDENTIFIER
					|| currentToken.kind == Token.THIS
					|| currentToken.kind == Token.IF
					|| currentToken.kind == Token.WHILE
					|| currentToken.kind == Token.LCURLY
					|| currentToken.kind == Token.INT
					|| currentToken.kind == Token.BOOLEAN
					|| currentToken.kind == Token.VOID) {
				Statement st = parseStatement();
				stList.add(st);
			}
			accept(Token.RCURLY);
			finish(blockStPosition);
			statement = new BlockStmt(stList, blockStPosition);
			break;
		case Token.IDENTIFIER:
			SourcePosition idStPosition = new SourcePosition();
			start(idStPosition);
			String typeName = currentToken.spelling;
			SourcePosition arrayPosition = currentToken.position;
			SourcePosition elementPos = currentToken.position;
			Reference r = new IdRef(new Identifier(typeName, elementPos),
					elementPos);
			Expression p = null;
			acceptIt();
			boolean hasBracket = false;
			boolean reference = false;
			if (currentToken.kind == Token.LBRACKET) {
				acceptIt();
				hasBracket = true;
				if (currentToken.kind == Token.NUM
						|| currentToken.kind == Token.THIS
						|| currentToken.kind == Token.IDENTIFIER
						|| currentToken.kind == Token.TRUE
						|| currentToken.kind == Token.FALSE
						|| currentToken.kind == Token.NEGATE
						|| currentToken.kind == Token.NOT
						|| currentToken.kind == Token.NEW
						|| currentToken.kind == Token.LPAREN
						|| currentToken.kind == Token.VOID) {

					p = parseExpression();
					reference = true;

				}
				accept(Token.RBRACKET);

				finish(arrayPosition);
				if (reference == true) {
					r = new IndexedRef(r, p, arrayPosition);
				}
				if (reference == false) {
					Type cl = new ClassType(
							new Identifier(typeName, elementPos), elementPos);
					Type arTy = new ArrayType(cl, arrayPosition);
					SourcePosition vdeclPosition = new SourcePosition(
							elementPos.startPosition,
							currentToken.position.finishPosition);
					VarDecl vd = new VarDecl(arTy, currentToken.spelling,
							vdeclPosition);
					accept(Token.IDENTIFIER);
					accept(Token.EQUAL);
					Expression value = parseExpression();
					accept(Token.SEMICOLON);
					finish(idStPosition);
					statement = new VarDeclStmt(vd, value, idStPosition);
				}
			}
			if (currentToken.kind == Token.EQUAL
					|| currentToken.kind == Token.LPAREN || reference == true
					|| currentToken.kind == Token.DOT) {
				r = parseReference(r, arrayPosition);
				if (currentToken.kind == Token.EQUAL) {
					acceptIt();
					Expression exp = parseExpression();
					accept(Token.SEMICOLON);
					finish(idStPosition);
					statement = new AssignStmt(r, exp, idStPosition);

				} else {
					ExprList exprList = new ExprList();
					accept(Token.LPAREN);
					if (currentToken.kind == Token.RPAREN)
						acceptIt();
					else {
						for (Expression args : parseArgumentList())
							exprList.add(args);
						accept(Token.RPAREN);
					}
					accept(Token.SEMICOLON);
					finish(idStPosition);
					statement = new CallStmt(r, exprList, idStPosition);
				}

			} else if (hasBracket == false) {
				Type t = new ClassType(new Identifier(typeName, elementPos),
						elementPos);
				SourcePosition varDeclPosition = elementPos;
				varDeclPosition.finishPosition = currentToken.position.finishPosition;
				String vname = currentToken.spelling;
				accept(Token.IDENTIFIER);
				VarDecl vd = new VarDecl(t, vname, varDeclPosition);
				accept(Token.EQUAL);
				Expression equal = parseExpression();
				accept(Token.SEMICOLON);
				finish(idStPosition);
				statement = new VarDeclStmt(vd, equal, idStPosition);

			}
			break;
		case Token.THIS:
			SourcePosition stPosition = new SourcePosition();
			start(stPosition);
			Reference ref = parseReference();
			if (currentToken.kind == Token.EQUAL) {
				acceptIt();
				Expression value = parseExpression();
				accept(Token.SEMICOLON);
				finish(stPosition);
				statement = new AssignStmt(ref, value, stPosition);

			} else {
				ExprList argList = new ExprList();
				accept(Token.LPAREN);
				if (currentToken.kind == Token.RPAREN)
					acceptIt();
				else {
					for (Expression arg : parseArgumentList())
						argList.add(arg);
					accept(Token.RPAREN);
				}
				accept(Token.SEMICOLON);
				finish(stPosition);
				statement = new CallStmt(ref, argList, stPosition);
			}

			break;
		default:
			errorReporter.reportError("cannot start a statement with: "
					+ currentToken.spelling);
		}
		return statement;
	}

	/**
	 * Parse expression
	 * 
	 * @return
	 * 
	 * @throws SyntaxError
	 */
	private Expression parseExpression() throws SyntaxError {

		Expression result = parseBinaryExpression();

		return result;

	}

	/**
	 * Parse the binary expression
	 * 
	 * @return
	 * @throws SyntaxError
	 */
	private Expression parseBinaryExpression() throws SyntaxError {
		SourcePosition binaryExpressionPosition = new SourcePosition();
		start(binaryExpressionPosition);

		Expression c = parseC();
		while (currentToken.kind == Token.OR) {
			Operator op = new Operator(currentToken, currentToken.position);
			acceptIt();
			Expression c2 = parseC();
			finish(binaryExpressionPosition);
			c = new BinaryExpr(op, c, c2, binaryExpressionPosition);
		}
		return c;
	}

	/* Helper method to parse the binary expression */
	private Expression parseC() throws SyntaxError {
		SourcePosition expc = new SourcePosition();
		start(expc);
		Expression a = parseA();
		while (currentToken.kind == Token.AND) {
			Operator op = new Operator(currentToken, currentToken.position);
			acceptIt();
			Expression a2 = parseA();
			finish(expc);
			a = new BinaryExpr(op, a, a2, expc);
		}
		return a;
	}

	/* Helper method to parse the binary expression */
	private Expression parseA() throws SyntaxError {
		SourcePosition expa = new SourcePosition();
		start(expa);
		Expression b = parseB();
		while (currentToken.kind == Token.ISEQUAL
				|| currentToken.kind == Token.NOTEQUAL) {
			Operator op = new Operator(currentToken, currentToken.position);
			acceptIt();
			Expression b2 = parseB();
			finish(expa);
			b = new BinaryExpr(op, b, b2, expa);
		}
		return b;
	}

	/* Helper method to parse the binary expression */
	private Expression parseB() throws SyntaxError {
		SourcePosition expb = new SourcePosition();
		start(expb);
		Expression d = parseD();
		while (currentToken.kind == Token.LESSTHANEQUAL
				|| currentToken.kind == Token.LESSTHAN
				|| currentToken.kind == Token.GREATER
				|| currentToken.kind == Token.GREATEREQUAL) {
			Operator op = new Operator(currentToken, currentToken.position);
			acceptIt();
			Expression d2 = parseD();
			finish(expb);
			d = new BinaryExpr(op, d, d2, expb);
		}
		return d;
	}

	/* Helper method to parse the binary expression */
	private Expression parseD() throws SyntaxError {
		SourcePosition expd = new SourcePosition();
		start(expd);
		Expression e = parseE();
		while (currentToken.kind == Token.PLUS
				|| currentToken.kind == Token.MINUS) {
			Operator op = new Operator(currentToken, currentToken.position);
			acceptIt();
			Expression e2 = parseE();
			finish(expd);
			e = new BinaryExpr(op, e, e2, expd);
		}
		return e;
	}

	/* Helper method to parse the binary expression */
	private Expression parseE() throws SyntaxError {
		SourcePosition expe = new SourcePosition();
		start(expe);
		Expression g = parseG();
		while (currentToken.kind == Token.TIMES
				|| currentToken.kind == Token.DEVIDE) {
			Operator op = new Operator(currentToken, currentToken.position);
			acceptIt();
			Expression g2 = parseG();
			g = new BinaryExpr(op, g, g2, expe);
		}
		return g;
	}

	/**
	 * Parse the unary expression
	 * 
	 * @return
	 * @throws SyntaxError
	 */

	private Expression parseG() throws SyntaxError {
		Expression result;
		SourcePosition unaryExpressionPosition = new SourcePosition();
		start(unaryExpressionPosition);
		if (currentToken.kind == Token.NEGATE ) {
			Operator op = new Operator(currentToken, currentToken.position);
			acceptIt();
			Expression e = parseG();
			finish(unaryExpressionPosition);
			result = new UnaryExpr(op, e, unaryExpressionPosition);
		} 
		else if(currentToken.kind== Token.NOT) {
			Operator op = new Operator(currentToken, currentToken.position);
			acceptIt();
			Expression e = parseG();
			finish(unaryExpressionPosition);
			result = new UnaryExpr(op, e, unaryExpressionPosition);

		}
		else
			result=parseGPrime();
		return result;
	}

	/* Helper method to parse the binary expression */
	private Expression parseGPrime() throws SyntaxError {
		Expression result = null;
		switch (currentToken.kind) {
		case Token.LPAREN:
			acceptIt();
			result = parseExpression();
			accept(Token.RPAREN);
			break;

		case Token.NUM:
			result = new LiteralExpr(new IntLiteral(currentToken.spelling,
					currentToken.position), currentToken.position);
			acceptIt();
			break;

		case Token.TRUE:
		case Token.FALSE:
			result = new LiteralExpr(new BooleanLiteral(currentToken.spelling,
					currentToken.position), currentToken.position);
			acceptIt();
			break;

		case Token.NEW:
			SourcePosition st = new SourcePosition();
			start(st);
			acceptIt();
			if (currentToken.kind == Token.INT) {
				Type type = new BaseType(TypeKind.INT, currentToken.position);
				acceptIt();
				accept(Token.LBRACKET);
				Expression e = parseExpression();
				accept(Token.RBRACKET);
				finish(st);
				result = new NewArrayExpr(type, e, st);
			} else {
				Identifier id = new Identifier(currentToken.spelling,
						currentToken.position);
				ClassType type = new ClassType(id, currentToken.position);
				accept(Token.IDENTIFIER);
				if (currentToken.kind == Token.LPAREN) {
					acceptIt();
					accept(Token.RPAREN);
					finish(st);
					result = new NewObjectExpr(type, st);
				} else {
					accept(Token.LBRACKET);
					Expression e = parseExpression();
					accept(Token.RBRACKET);
					finish(st);
					result = new NewArrayExpr(type, e, st);
				}
			}
			break;
		case Token.THIS:
		case Token.IDENTIFIER:
			SourcePosition ref = new SourcePosition();
			start(ref);
			Reference r = parseReference();
			ExprList list = new ExprList();
			if (currentToken.kind == Token.LPAREN) {
				acceptIt();
				if (currentToken.kind == Token.RPAREN) {
					acceptIt();
				} else {
					for (Expression e : parseArgumentList())
						list.add(e);
					accept(Token.RPAREN);
				}
				finish(ref);
				result = new CallExpr(r, list, ref);
			} else {
				finish(ref);
				result = new RefExpr(r, ref);
			}
			break;
		default:
			errorReporter.reportError("cannot start an expression with:"
					+ currentToken.spelling);
		}
		return result;
	}

	public void setPreviousSourcePosition(SourcePosition previousSourcePosition) {
		this.previousSourcePosition = previousSourcePosition;
	}

	public SourcePosition getPreviousSourcePosition() {
		return previousSourcePosition;
	}
}