package miniJava.CodeGenerator;

import java.util.LinkedList;
import java.util.List;

import mJAM.Machine;
import mJAM.Machine.Op;
import mJAM.Machine.Prim;
import mJAM.Machine.Reg;
import miniJava.AbstractSyntaxTrees.AST;
import miniJava.AbstractSyntaxTrees.ArrayType;
import miniJava.AbstractSyntaxTrees.AssignStmt;
import miniJava.AbstractSyntaxTrees.BaseType;
import miniJava.AbstractSyntaxTrees.BinaryExpr;
import miniJava.AbstractSyntaxTrees.BlockStmt;
import miniJava.AbstractSyntaxTrees.BooleanLiteral;
import miniJava.AbstractSyntaxTrees.CallExpr;
import miniJava.AbstractSyntaxTrees.CallStmt;
import miniJava.AbstractSyntaxTrees.ClassDecl;
import miniJava.AbstractSyntaxTrees.ClassType;
import miniJava.AbstractSyntaxTrees.Declaration;
import miniJava.AbstractSyntaxTrees.ExprList;
import miniJava.AbstractSyntaxTrees.Expression;
import miniJava.AbstractSyntaxTrees.FieldDecl;
import miniJava.AbstractSyntaxTrees.IdRef;
import miniJava.AbstractSyntaxTrees.Identifier;
import miniJava.AbstractSyntaxTrees.IfStmt;
import miniJava.AbstractSyntaxTrees.IndexedRef;
import miniJava.AbstractSyntaxTrees.IntLiteral;
import miniJava.AbstractSyntaxTrees.LiteralExpr;
import miniJava.AbstractSyntaxTrees.MethodDecl;
import miniJava.AbstractSyntaxTrees.NewArrayExpr;
import miniJava.AbstractSyntaxTrees.NewObjectExpr;
import miniJava.AbstractSyntaxTrees.Operator;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.AbstractSyntaxTrees.ParameterDecl;
import miniJava.AbstractSyntaxTrees.ParameterDeclList;
import miniJava.AbstractSyntaxTrees.QualifiedRef;
import miniJava.AbstractSyntaxTrees.RefExpr;
import miniJava.AbstractSyntaxTrees.Statement;
import miniJava.AbstractSyntaxTrees.StatementList;
import miniJava.AbstractSyntaxTrees.ThisRef;
import miniJava.AbstractSyntaxTrees.TypeKind;
import miniJava.AbstractSyntaxTrees.UnaryExpr;
import miniJava.AbstractSyntaxTrees.VarDecl;
import miniJava.AbstractSyntaxTrees.VarDeclStmt;
import miniJava.AbstractSyntaxTrees.Visitor;
import miniJava.AbstractSyntaxTrees.WhileStmt;

public class CodeGenerator implements Visitor<Integer, Integer> {
	private boolean debug = false, secondTraversal = false;
	private boolean isMethodCall = false;
	private boolean isMainMethod = false;
	int labelMain = 0, patchMe = 0;
	int ST = 0;
	public ClassDecl staticClassRef;
	public Declaration instanceClassRef;
	List<MethodOffSetPair> methodsOffSets;
	List<ClassOffSetPair> classOffSets;
	public ClassDecl currentClass;
	public int arrayLength;
	public boolean newArray;

	public final int ARRAYUPDATE = 4;
	public final int OBJUPDATE = 3;
	public final int STATICREF = 2;
	public final int MEMREF = 1;
	public final int LOCALREF = 0;
	public int OP = LOCALREF; // default value
	public boolean isLHS = false;
	private int ADDRESSSIZE = 1;
	private int VOIDRSIZE = 0;
	private int BASETSIZE = 1;

	public CodeGenerator() {
		Machine.initCodeGen();
		methodsOffSets = new LinkedList<MethodOffSetPair>();
		classOffSets = new LinkedList<ClassOffSetPair>();
		newArray = false;
	}

	public void generateCode(AST ast) {
		ast.visit(this, Integer.valueOf(0));
		secondTraversal = true;
		ast.visit(this, Integer.valueOf(0));
		for (MethodOffSetPair m : methodsOffSets) {
			Machine.patch(m.offset, m.decl.storage.address);
		}
		for (ClassOffSetPair c : classOffSets) {
			Machine.patch(c.offset, c.decl.storage.offset);
		}
		Machine.emit(Op.LOADL, -1);
		Machine.emit(Op.CALL, Reg.CB, labelMain);
		Machine.emit(Op.HALT, 0, 0, 0);
	}

	// Package
	public Integer visitPackage(Package prog, Integer arg) {
		for (ClassDecl c : prog.classDeclList) {
			c.visit(this, Integer.valueOf(0));
			currentClass = c;
		}
		return (Integer.valueOf(VOIDRSIZE));
	}

	// Declarations
	public Integer visitClassDecl(ClassDecl clas, Integer arg) {
		int methodOffset = 0, methodLabel = 0;
		if (!secondTraversal) {
			/*
			 * format of class object static fields super class no of methods
			 * method pointers
			 */
			int i = 0;
			for (FieldDecl f : clas.fieldDeclList) {
				if (f.isStatic) {
					f.storage.offset = ST;
					Machine.emit(Op.PUSH, 1);
					Machine.emit(Op.LOADL, 0);
					Machine.emit(Op.STORE, Reg.SB, ST);
					ST++;
				} else {
					f.storage.offset = i;
					i++;
				}
			}
			// storing the offset of the classobject
			clas.storage.offset = ST;
			Machine.emit(Op.LOADL, -1);
			// superclass, no of methods
			Machine.emit(Op.LOADL, clas.methodDeclList.size());
			if (clas.methodDeclList.size() > 0)
				Machine.emit(Op.PUSH, clas.methodDeclList.size());
			ST += 2;
			for (MethodDecl m : clas.methodDeclList) {
				m.storage.offset = methodOffset;
				ST++;
				methodOffset++;
			}
		} else {
			for (MethodDecl m : clas.methodDeclList) {
				methodLabel = Machine.nextInstrAddr();
				Machine.emit(Op.LOADL, methodLabel + 3);
				Machine.emit(Op.STORE, Reg.SB, clas.storage.offset + 2
						+ m.storage.offset);
				// check for main method
				if (m.isStatic
						&& m.isPrivate == false
						&& m.name.equals("main")
						&& m.type.typeKind == TypeKind.VOID
						&& m.parameterDeclList.size() == 1
						&& m.parameterDeclList.get(0).type.typeKind == TypeKind.ARRAY
						&& ((ClassType) ((ArrayType) (m.parameterDeclList
								.get(0).type)).eltType).className.spelling
								.equals("String")) {

					labelMain = Machine.nextInstrAddr() + 1;
					isMainMethod = true;
				}
				patchMe = Machine.nextInstrAddr();
				Machine.emit(Op.JUMP, Reg.CB, 0);

				m.visit(this, Integer.valueOf(0));
				int patch = Machine.nextInstrAddr();

				Machine.patch(patchMe, patch);
				isMainMethod = false;
			}
		}
		return (Integer.valueOf(VOIDRSIZE));
	}

	public void generatePrintlnCode() {

		Machine.emit(Prim.putintnl);

	}

	public Integer visitFieldDecl(FieldDecl f, Integer arg) {

		return (Integer.valueOf(VOIDRSIZE));
	}

	public Integer visitMethodDecl(MethodDecl m, Integer arg) {
		int offset = 3;

		if (debug)
			System.out.println("in method declaration: " + m.name);
		m.storage.address = Machine.nextInstrAddr();
		ParameterDeclList pdl = m.parameterDeclList;
		int length = pdl.size();
		for (ParameterDecl pd : pdl) {
			pd.storage.offset = -length;
			length--;
		}

		StatementList sl = m.statementList;

		for (Statement s : sl) {
			offset += s.visit(this, Integer.valueOf(offset)).intValue();
		}
		// when returning subtract 3 offset and pop those many elements from the
		// stack
		if (m.returnExp != null) {
			int pushCount = m.returnExp.visit(this, Integer.valueOf(offset));

			Machine.emit(Op.RETURN, pushCount, 0, m.parameterDeclList.size());
		} else {
			// when returning subtract 3 offset and pop those many elements from
			// the stack
			if (!isMainMethod)
				Machine.emit(Op.RETURN, 0, 0, m.parameterDeclList.size());
			else
				Machine.emit(Op.RETURN, 0, 0, 0);
		}

		// useless, but doing it for convention
		return (Integer.valueOf(VOIDRSIZE));
	}

	public Integer visitParameterDecl(ParameterDecl pd, Integer arg) {
		return (Integer.valueOf(VOIDRSIZE));
	}

	public Integer visitVarDecl(VarDecl vd, Integer arg) {
		int size = 0;

		vd.storage.offset = arg.intValue();
		size = vd.type.visit(this, arg).intValue();
		vd.storage.size = size;
		Machine.emit(Op.PUSH, size);

		return (Integer.valueOf(size));
	}

	// Statements

	public Integer visitVardeclStmt(VarDeclStmt stmt, Integer arg) {
		int pushCount = stmt.varDecl.visit(this, arg).intValue();

		// visit initialization expression
		stmt.initExp.visit(this, Integer.valueOf((arg.intValue()) + pushCount));

		// STORE

		Machine.emit(Op.STORE, Reg.LB, stmt.varDecl.storage.offset);
		return (Integer.valueOf(pushCount));
	}

	public Integer visitBlockStmt(BlockStmt stmt, Integer arg) {
		int offset = arg.intValue();
		int pushCount = 0;

		StatementList sl = stmt.sl;
		for (Statement s : sl) {
			pushCount += s.visit(this, Integer.valueOf(offset));
			offset += pushCount;
		}

		if (pushCount > 0)
			Machine.emit(Op.POP, pushCount);

		return (Integer.valueOf(VOIDRSIZE));
	}

	public Integer visitAssignStmt(AssignStmt stmt, Integer arg) {
		if (debug)
			System.out.println("Assignment statement");
		int op = LOCALREF, offset = 0;
		isLHS = true;
		int pushCount = stmt.ref.visit(this, arg).intValue();

		op = OP;
		isLHS = false;

		if ((op == LOCALREF) || (op == STATICREF)) {
			/*
			 * for local ref and static ref, we get back the offset from SB|LB
			 * depending on ref type
			 */
			offset = pushCount;
			pushCount = 0;
		}

		OP = LOCALREF;
		stmt.val.visit(this, Integer.valueOf((arg.intValue()) + pushCount));
		if (op == ARRAYUPDATE) {
			Machine.emit(Prim.arrayupd);
		} else if (op == MEMREF) {
			Machine.emit(Prim.fieldupd);
		} else if (op == LOCALREF) {
			if (debug)
				System.out.println("In visitAssignStmt, LOCALREF case");
			Machine.emit(Op.STORE, Reg.LB, offset);
		} else {
			// static ref case
			Machine.emit(Op.STORE, Reg.SB, offset);
		}
		return (Integer.valueOf(VOIDRSIZE));
	}

	public Integer visitCallStmt(CallStmt stmt, Integer arg) {
		if (debug)
			System.out.println("In call stmt");

		int pushCount = pushArgList(stmt.argList, arg).intValue();
		isMethodCall = true;
		// ignoring the return value from a call statement
		pushCount = stmt.methodRef.visit(this,
				Integer.valueOf(arg.intValue() + pushCount)).intValue();
		
		isMethodCall = false;

		return (Integer.valueOf(VOIDRSIZE));
	}

	public Integer visitIfStmt(IfStmt stmt, Integer arg) {
		stmt.cond.visit(this, arg).intValue();
		int patchIf = Machine.nextInstrAddr();
		int skipElse = 0;

		Machine.emit(Op.JUMPIF, 0, Reg.CB, 0);

		stmt.thenStmt.visit(this, arg).intValue();

		if (stmt.elseStmt != null) {
			skipElse = Machine.nextInstrAddr();
			Machine.emit(Op.JUMP, Reg.CB, 0);
			int elsePC = Machine.nextInstrAddr();
			Machine.patch(patchIf, elsePC);
			stmt.elseStmt.visit(this, arg);
			Machine.patch(skipElse, Machine.nextInstrAddr());
		} else {
			int endInstr = Machine.nextInstrAddr();
			Machine.patch(patchIf, endInstr);
		}
		return (Integer.valueOf(VOIDRSIZE));
	}

	public Integer visitWhileStmt(WhileStmt stmt, Integer arg) {
		int J2condEval = Machine.nextInstrAddr();

		Machine.emit(Op.JUMP, Reg.CB, 0);
		int body = Machine.nextInstrAddr();
		stmt.body.visit(this, arg);
		int condEval = Machine.nextInstrAddr();
		Machine.patch(J2condEval, condEval);
		stmt.cond.visit(this, arg);
		Machine.emit(Op.JUMPIF, 1, Reg.CB, body);

		return (Integer.valueOf(VOIDRSIZE));
	}

	// Expressions
	public Integer visitUnaryExpr(UnaryExpr expr, Integer arg) {
		if (debug)
			System.out.println("In visitUnaryExpr");
		int pushCount = expr.expr.visit(this, arg);

		visitUnaryOperator(expr.operator, arg);

		return (Integer.valueOf(pushCount));
	}

	public Integer visitBinaryExpr(BinaryExpr expr, Integer arg) {
		if (debug)
			System.out.println("In visitBinaryExpr");
		// visit the left side expression
		int pushCount = expr.left.visit(this, arg).intValue();

		// if it is "and" operation
		if (expr.operator.spelling.equals("&&")) {
			Machine.emit(Op.LOAD, Reg.ST, -1);
			int jumpToEnd = Machine.nextInstrAddr();
			Machine.emit(Op.JUMPIF, 0, Reg.CB, 0);
			expr.right.visit(this, Integer.valueOf(arg.intValue() + pushCount));
			visitBinaryOperator(expr.operator, arg);
			Machine.patch(jumpToEnd, Machine.nextInstrAddr());

		}
		// if it is "or" operation
		else if (expr.operator.spelling.equals("||")) {
			Machine.emit(Op.LOAD, Reg.ST, -1);
			int jumpToEnd = Machine.nextInstrAddr();
			Machine.emit(Op.JUMPIF, 1, Reg.CB, 0);
			expr.right.visit(this, Integer.valueOf(arg.intValue() + pushCount));
			visitBinaryOperator(expr.operator, arg);
			Machine.patch(jumpToEnd, Machine.nextInstrAddr());
		}
		// other operations
		else {
			expr.right.visit(this, Integer.valueOf(arg.intValue() + pushCount));
			visitBinaryOperator(expr.operator, arg);

		}
		return (Integer.valueOf(pushCount));
	}

	public Integer visitRefExpr(RefExpr expr, Integer arg) {
		return (expr.ref.visit(this, arg));
	}

	public Integer visitCallExpr(CallExpr expr, Integer arg) {
		if (debug)
			System.out.println("In call expression");

		int pushCount = pushArgList(expr.argList, arg).intValue();
		isMethodCall = true;
		// need to call the function in the ref method itself
		pushCount = expr.functionRef.visit(this,
				Integer.valueOf(arg.intValue() + pushCount)).intValue();
		isMethodCall = false;
		return (Integer.valueOf(pushCount));
	}

	public Integer visitLiteralExpr(LiteralExpr expr, Integer arg) {
		// will any push the value onto the stack
		return (expr.literal.visit(this, arg));

	}

	public Integer visitNewArrayExpr(NewArrayExpr expr, Integer arg) {
		newArray = true;
		expr.sizeExpr.visit(this, arg);

		Machine.emit(Prim.newarr);
		newArray = false;
		return (Integer.valueOf(ADDRESSSIZE));
	}

	public Integer visitNewObjectExpr(NewObjectExpr expr, Integer arg) {
		int address = Machine.nextInstrAddr();
		classOffSets
				.add(new ClassOffSetPair(expr.classtype.classDecl, address));
		Machine.emit(Op.LOADA, Reg.SB, 0);
		Machine.emit(Op.LOADL, expr.classtype.classDecl.fieldDeclList.size());
		Machine.emit(Prim.newobj);
		return (Integer.valueOf(ADDRESSSIZE));
	}

	// Types
	public Integer visitBaseType(BaseType type, Integer arg) {

		return (Integer.valueOf(BASETSIZE));
	}

	public Integer visitClassType(ClassType type, Integer arg) {
		return (Integer.valueOf(ADDRESSSIZE));
	}

	public Integer visitArrayType(ArrayType type, Integer arg) {
		return (Integer.valueOf(ADDRESSSIZE));
	}

	// References

	public Integer visitQualifiedRef(QualifiedRef ref, Integer arg)

	{

		if (debug)
			System.out.println("In Dereference" + isMethodCall);

		boolean isMethodCallLocalFlag = isMethodCall;

		boolean isLHSLocalFlag = isLHS;
		// resetting the flags
		isMethodCall = false;
		isLHS = false;
		if (isMethodCallLocalFlag && ref.ref.decl instanceof ClassDecl
				&& ref.ref.decl.name.equals("System")
				&& ref.id.decl.name.equals("println")) {
			generatePrintlnCode();
		} else {
			// recursion
			ref.ref.visit(this, arg);
			// check the rest
			int address = Machine.nextInstrAddr();

			if (isMethodCallLocalFlag) {
				// qualified reference from instance class variable
				if (instanceClassRef != null) {

					Machine.emit(Op.CALLI, Reg.CB, 0);

					methodsOffSets.add(new MethodOffSetPair(
							(MethodDecl) ref.id.decl, address));

					instanceClassRef = null;
				}
				// static qualified reference
				else {

					Machine.emit(Op.CALL, Reg.CB, 0);
					methodsOffSets.add(new MethodOffSetPair(
							(MethodDecl) ref.id.decl, address));

					staticClassRef = null;

				}
				isMethodCall = isMethodCallLocalFlag;

				return (Integer.valueOf(ref.id.decl.storage.size));
			}

			if (isLHSLocalFlag) {
				if (instanceClassRef != null
						|| ref.ref.returnType == TypeKind.ARRAY) {
					OP = MEMREF;
					Machine.emit(Op.LOADL, ref.id.decl.storage.offset);
					isLHS = isLHSLocalFlag;
					return (Integer.valueOf(2));
				} else {
					OP = STATICREF;
					isLHS = isLHSLocalFlag;
					return (Integer.valueOf(ref.id.decl.storage.offset));

				}
			}
			// special case for Array.length
			if (ref.ref.returnType == TypeKind.ARRAY
					&& ref.id.spelling.equals("length")) {

				Machine.emit(Op.LOADL, -1);
				Machine.emit(Prim.add);
				Machine.emit(Op.LOADI, 1);
			} else {
				if (((FieldDecl) ref.id.decl).isStatic == true) {
					Machine.emit(Op.LOAD, Reg.SB, ref.id.decl.storage.offset);
				} else {
					Machine.emit(Op.LOADL, ref.id.decl.storage.offset);
					Machine.emit(Prim.fieldref);
				}
			}
			isMethodCall = isMethodCallLocalFlag;
			isLHS = isLHSLocalFlag;

		}

		return (Integer.valueOf(ADDRESSSIZE));

	}

	public Integer visitIndexedRef(IndexedRef ref, Integer arg) {
		boolean isLHSBack = false;
		if (debug)
			System.out.println("In Indexed reference");

		int pushCount = ref.ref.visit(this, arg);

		if (debug)
			System.out.println("index reference isLHS: " + isLHS);

		if ((OP == LOCALREF) && isLHS) {
			Machine.emit(Op.LOAD, Reg.LB, pushCount);
			pushCount = 1;
		} else if ((OP == STATICREF) && isLHS) {
			Machine.emit(Op.LOAD, Reg.SB, pushCount);
			pushCount = 1;
		} else if ((OP == MEMREF) && isLHS) {
			Machine.emit(Prim.fieldref);
			pushCount = 1;
		}

		// backup isLHS flag
		isLHSBack = isLHS;
		isLHS = false;

		// need to keep track of the pushCount in case if it is a assignment
		pushCount += ref.indexExpr.visit(this,
				Integer.valueOf(arg.intValue() + pushCount));

		if (isLHSBack) {
			OP = ARRAYUPDATE;
			isLHS = true;
			return (Integer.valueOf(pushCount));
		}

		Machine.emit(Prim.arrayref);
		return (Integer.valueOf(ADDRESSSIZE));
	}

	public Integer visitThisRef(ThisRef ref, Integer arg) {
		instanceClassRef = currentClass;
		Machine.emit(Op.LOADA, Reg.OB, 0);
		return (Integer.valueOf(VOIDRSIZE));
	}

	public Integer visitIdentifier(Identifier id, Integer arg) {
		if (debug)
			System.out.println("In visitIdentifier method");

		return (Integer.valueOf(VOIDRSIZE));
	}

	public Integer visitOperator(Operator op, Integer arg) {
		return (Integer.valueOf(VOIDRSIZE));
	}

	public Integer visitBinaryOperator(Operator op, Integer arg) {
		String opName = op.spelling;

		// different binary operator cases
		if (opName.equals("+"))
			Machine.emit(Prim.add);
		else if (opName.equals("-"))
			Machine.emit(Prim.sub);
		else if (opName.equals("*"))
			Machine.emit(Prim.mult);
		else if (opName.equals("/"))
			Machine.emit(Prim.div);
		else if (opName.equals(">"))
			Machine.emit(Prim.gt);
		else if (opName.equals("<"))
			Machine.emit(Prim.lt);
		else if (opName.equals(">="))
			Machine.emit(Prim.ge);
		else if (opName.equals("<="))
			Machine.emit(Prim.le);
		else if (opName.equals("!="))
			Machine.emit(Prim.ne);
		else if (opName.equals("=="))
			Machine.emit(Prim.eq);
		else if (opName.equals("&&"))
			Machine.emit(Prim.and);
		else if (opName.equals("||"))
			Machine.emit(Prim.or);

		else if (debug)
			System.out.println("Hit the default case in visit binary operator");

		return (Integer.valueOf(VOIDRSIZE));
	}

	public Integer visitUnaryOperator(Operator op, Integer arg) {
		String opName = op.spelling;

		if (opName.equals("-"))
			Machine.emit(Prim.neg);
		else if (opName.equals("!"))
			Machine.emit(Prim.not);
		else if (debug)
			System.out.println("Hit the default case in visit Unary operator");

		return (Integer.valueOf(VOIDRSIZE));
	}

	// Literals
	public Integer visitIntLiteral(IntLiteral num, Integer arg) {
		if (debug)
			System.out.println("In visitIntLiteral");

		Machine.emit(Op.LOADL, Integer.valueOf(num.spelling).intValue());
		return (Integer.valueOf(BASETSIZE));
	}

	public Integer visitBooleanLiteral(BooleanLiteral bool, Integer arg) {
		int value = 0;
		if (bool.spelling.equals("true"))
			value = 1;

		Machine.emit(Op.LOADL, value);
		return (Integer.valueOf(BASETSIZE));
	}

	public Integer pushArgList(ExprList argList, Integer arg) {
		int pushCount = 0;

		Expression expr = null;

		for (int i = 0; i < argList.size(); i++) {
			expr = argList.get(i);
			pushCount += expr.visit(this,
					Integer.valueOf(arg.intValue() + pushCount));
		}

		return (Integer.valueOf(pushCount));
	}

	@Override
	public Integer visitIdRef(IdRef ref, Integer arg) {

		int address = Machine.nextInstrAddr();
		Declaration decl = ref.id.decl;
		// a member ref
		if (decl instanceof FieldDecl || decl instanceof MethodDecl) {

			if (decl instanceof FieldDecl
					&& ((FieldDecl) decl).type.typeKind == TypeKind.CLASS) {
				instanceClassRef = ((ClassType) ((FieldDecl) decl).type).classDecl;
			}
			if (debug)
				System.out.println("In member reference");

			if (isMethodCall) {

				Machine.emit(Op.LOADA, Reg.OB, 0);

				int patch = Machine.nextInstrAddr();

				Machine.emit(Op.CALLI, Reg.CB, 0);
				methodsOffSets.add(new MethodOffSetPair((MethodDecl) decl,
						patch));

				return (Integer.valueOf(decl.storage.size));
			}
			if (isLHS) {

				if (((FieldDecl) decl).isStatic == true) {
					OP = STATICREF;
					return (Integer.valueOf(decl.storage.offset));
				} else {
					OP = MEMREF;
					Machine.emit(Op.LOADA, Reg.OB, 0);

					Machine.emit(Op.LOADL, decl.storage.offset);
				}

				return (Integer.valueOf(2));
			}
			if (((FieldDecl) decl).isStatic == true)
				Machine.emit(Op.LOAD, Reg.SB, decl.storage.offset);
			else
				Machine.emit(Op.LOAD, Reg.OB, decl.storage.offset);
			return (Integer.valueOf(decl.storage.size));

		}
		// class ref
		else if (decl instanceof ClassDecl) {
			staticClassRef = (ClassDecl) decl;
			// classOffSets.add(new ClassOffSetPair(staticClassRef, address));
			// Machine.emit(Op.LOAD, Reg.SB, 0);
			return (Integer.valueOf(decl.storage.size));
		}
		// a local ref (local variable or parameter)
		else {

			if (debug)
				System.out.println("In local reference");
			// check if the variable is instance of a class
			if (decl != null && decl.type.typeKind == TypeKind.CLASS) {
				if (decl instanceof VarDecl) {
					instanceClassRef = ((ClassType) ((VarDecl) decl).type).classDecl;
				} else
					instanceClassRef = ((ClassType) ((ParameterDecl) decl).type).classDecl;
			}
			if (decl != null && isLHS) {
				OP = LOCALREF;
				return (Integer.valueOf(decl.storage.offset));
			}

			Machine.emit(Op.LOAD, Reg.LB, decl.storage.offset);

			return (Integer.valueOf(decl.storage.size));
		}

	}
}
