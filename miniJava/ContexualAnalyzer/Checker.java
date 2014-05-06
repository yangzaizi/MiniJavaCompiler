package miniJava.ContexualAnalyzer;

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
import miniJava.AbstractSyntaxTrees.ClassDeclList;
import miniJava.AbstractSyntaxTrees.ClassType;
import miniJava.AbstractSyntaxTrees.Declaration;
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
import miniJava.AbstractSyntaxTrees.MethodDecl;
import miniJava.AbstractSyntaxTrees.MethodDeclList;
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
import miniJava.AbstractSyntaxTrees.Type;
import miniJava.AbstractSyntaxTrees.TypeKind;
import miniJava.AbstractSyntaxTrees.UnaryExpr;
import miniJava.AbstractSyntaxTrees.VarDecl;
import miniJava.AbstractSyntaxTrees.VarDeclStmt;
import miniJava.AbstractSyntaxTrees.Visitor;
import miniJava.AbstractSyntaxTrees.WhileStmt;
import miniJava.SyntaticAnalyzer.SourcePosition;

public class Checker implements Visitor<String, Object> {
	private Type methodReturnType;
	public static boolean hasError = false;
	public static boolean hasDuplicate = false;
	public IdentificationTable idTable;
	private Declaration currentDecl;
	private boolean mainMethodFound;
	private boolean staticMethod;
	private String expressionClassName;
	private String referenceClassName;
	private boolean baseTypeArray;
	private TypeKind referenceBaseType;
	private String referenceName;
	private boolean insideConditional, insideWhile;
	private String ClassDeclName;
	private ExprList args;
	private ClassDeclList classes;
	private CallStmt currentCallStatement;
	private CallExpr currentCallExpression;
	private boolean callSt, callExp;
	private boolean fieldDecl, paramDecl, varDecl, underRef;
	private VarDecl varDeclName;
	private Declaration prevRefDecl;
	private final ClassType nullType;
	private final SourcePosition pos = new SourcePosition();
	private boolean moreThanOneStatement;

	public Checker() {
		fieldDecl = false;
		varDecl = false;
		paramDecl = false;
		moreThanOneStatement = false;
		currentDecl = null;
		idTable = new IdentificationTable();
		addStandardEnvironment();
		mainMethodFound = false;
		staticMethod = false;
		args = null;
		classes = new ClassDeclList();
		nullType = new ClassType(null, pos);
	}

	public void check(AST ast) {

		ast.visit(this, "");

	}

	public void addStandardEnvironment() {
		MethodDeclList printlnMethodList = new MethodDeclList();

		FieldDecl printlnfield = new FieldDecl(false, false, new BaseType(
				TypeKind.VOID, null), "println", null);
		ParameterDeclList param = new ParameterDeclList();
		param.add(new ParameterDecl(new BaseType(TypeKind.INT, null), "n", null));
		MethodDecl println = new MethodDecl(printlnfield, param,
				new StatementList(), null, null);
		printlnMethodList.add(println);
		ClassDecl printStream = new ClassDecl("_PrintStream",
				new FieldDeclList(), printlnMethodList, null);
		printStream.type = new ClassType(new Identifier("_PrintStream", null),
				null);
		printStream.type.typeKind = TypeKind.CLASS;
		idTable.enter("_PrintStream", printStream);

		ClassDecl String = new ClassDecl("String", new FieldDeclList(),
				new MethodDeclList(), null);
		String.type = new ClassType(new Identifier("String", null), null);
		String.type.typeKind = TypeKind.CLASS;
		idTable.enter("String", String);

		FieldDeclList systemFields = new FieldDeclList();
		FieldDecl out = new FieldDecl(false, true, new ClassType(
				new Identifier("_PrintStream", null), null), "out", null);
		systemFields.add(out);
		ClassDecl system = new ClassDecl("System", systemFields,
				new MethodDeclList(), null);
		system.type = new ClassType(new Identifier("System", null), null);
		system.type.typeKind = TypeKind.CLASS;
		idTable.enter("System", system);

	}

	@Override
	public Object visitPackage(Package prog, String arg) {
		ClassDeclList list = prog.classDeclList;

		// enter all classes into identification table and check for duplicate
		for (ClassDecl c : list) {
			classes.add(c);
			c.type = new ClassType(new Identifier(c.name, c.posn), c.posn);
			boolean duplicate = idTable.enter(c.name, c);
			if (duplicate == true) {
				hasError = true;
				System.out.println("*** Duplicate class declaration " + c.name
						+ ". Error at line " + c.posn + ".");
			}

		}
		// visit each class
		for (ClassDecl c : list) {
			idTable.openScope();

			currentDecl = c;
			referenceClassName = currentDecl.name;
			c.visit(this, arg);
			idTable.closeScope();

		}
		// check if main method is found
		if (!mainMethodFound) {
			hasError = true;
			System.out.println("*** no main method found!");
		}
		return null;
	}

	@Override
	public Object visitClassDecl(ClassDecl cd, String arg) {
		// enter all fields of the class
		for (FieldDecl f : cd.fieldDeclList) {
			if (f.type.typeKind == TypeKind.CLASS) {
				String className = ((ClassType) f.type).className.spelling;
				if (idTable.retrieve(className) == null) {
					hasError = true;
					System.out.println("*** Error at line " + f.posn
							+ ". Class type " + className
							+ " cannot be resolved.");
					System.exit(4);
				}
			}
			f.visit(this, arg);
		}
		// enter all methods of the class
		for (MethodDecl m : cd.methodDeclList) {
			boolean duplicate = idTable.enter(m.name, m);
			if (duplicate == true) {
				hasError = true;
				System.out
						.println("*** Duplicate method declaration. Error at line "
								+ m.posn + ".");
			}
			// check for duplicate main method
			if (mainMethodFound == true) {
				if (m.isStatic
						&& m.isPrivate == false
						&& m.name.equals("main")
						&& m.type.typeKind == TypeKind.VOID
						&& m.parameterDeclList.size() == 1
						&& m.parameterDeclList.get(0).type.typeKind == TypeKind.ARRAY
						&& ((ClassType) ((ArrayType) (m.parameterDeclList
								.get(0).type)).eltType).className.spelling
								.equals("String")) {
					hasError = true;
					System.out
							.println("*** Duplicate main method. Only one unique main is allowed. Error at line "
									+ m.posn + ".");
				}
			} else if (m.isStatic
					&& m.isPrivate == false
					&& m.name.equals("main")
					&& m.type.typeKind == TypeKind.VOID
					&& m.parameterDeclList.size() == 1
					&& m.parameterDeclList.get(0).type.typeKind == TypeKind.ARRAY
					&& ((ClassType) ((ArrayType) (m.parameterDeclList.get(0).type)).eltType).className.spelling
							.equals("String")) {

				mainMethodFound = true;
			}
		}
		// visit each method
		for (MethodDecl m : cd.methodDeclList) {
			idTable.openScope();
			m.visit(this, arg);
			idTable.closeScope();

		}

		return null;
	}

	@Override
	public Object visitFieldDecl(FieldDecl fd, String arg) {
		// fields cannot be of type void
		if (fd.type.typeKind == TypeKind.VOID) {
			hasError = true;
			System.out.println("*** Invalid type for a field. Error at "
					+ fd.posn + ".");
		}
		// check for duplicate field
		boolean duplicate = idTable.enter(fd.name, fd);
		if (duplicate == true) {
			hasError = true;
			System.out
					.println("*** Duplicate field declaration. Error at line "
							+ fd.posn + ".");
		}
		return null;
	}

	@Override
	public Object visitMethodDecl(MethodDecl md, String arg) {
		if (md.isStatic == true)
			staticMethod = true;

		// visit each paramter
		for (ParameterDecl param : md.parameterDeclList) {

			param.visit(this, arg);

		}
		// visit each statement inside the method
		for (Statement st : md.statementList) {

			st.visit(this, arg);

		}

		// check for proper return if the method is void
		if (md.type.typeKind == TypeKind.VOID) {
			if (md.returnExp != null) {
				hasError = true;
				System.out.println("*** Invalid method return at line "
						+ md.returnExp.posn + ". This is a void method.");
			}

		}
		// check return type
		else {
			// missing return statement
			if (md.returnExp == null) {
				hasError = true;
				System.out.println("*** Missing return statement at line "
						+ md.posn + ".");
			} else {
				TypeKind returnKind = (TypeKind) md.returnExp.visit(this, arg);
				// if the return expression is a literal expression, check if
				// method return type is either int or boolean. Also check for
				// mismatch
				if (md.returnExp instanceof LiteralExpr) {
					if (md.type.typeKind != TypeKind.INT
							&& md.type.typeKind != TypeKind.BOOLEAN) {
						hasError = true;
						System.out
								.println("*** Mismatch in return type at line "
										+ md.returnExp.posn + ".");
					} else if (md.type.typeKind != returnKind) {
						hasError = true;
						System.out
								.println("*** Mismatch in return type at line "
										+ md.returnExp.posn + ".");
					}
				}
				// if the return expression is binary, check if the method
				// return type is int or boolean. Also check for type mismatch
				else if (md.returnExp instanceof BinaryExpr) {
					if (md.type.typeKind != TypeKind.INT
							&& md.type.typeKind != TypeKind.BOOLEAN) {
						hasError = true;
						System.out
								.println("*** Mismatch in return type at line "
										+ md.returnExp.posn + ".");
					} else if (md.type.typeKind != returnKind) {
						hasError = true;
						System.out
								.println("*** Mismatch in return type at line "
										+ md.returnExp.posn + ".");
					}

				}
				// if the return expression is unary, check if method return
				// type is boolean. Also check for type mismatch
				else if (md.returnExp instanceof UnaryExpr) {
					if (md.type.typeKind != TypeKind.INT
							&& md.type.typeKind != TypeKind.BOOLEAN) {
						hasError = true;
						System.out
								.println("*** Mismatch in return type at line "
										+ md.returnExp.posn + ".");
					}

					else if (md.type.typeKind != returnKind) {
						hasError = true;
						System.out
								.println("*** Mismatch in return type at line "
										+ md.returnExp.posn + ".");
					}

				}
				// if the return expression is a newObjectExpression, check for
				// method return type is of type object. Also check for name
				// equivalence
				else if (md.returnExp instanceof NewObjectExpr) {
					if (md.type instanceof ClassType == false) {
						hasError = true;
						System.out
								.println("*** Mismatch in return type at line "
										+ md.returnExp.posn + ".");
					} else {
						String name = ((ClassType) md.type).className.spelling;
						md.returnExp.visit(this, arg);
						String name2 = expressionClassName;
						if (!name.equals(name2)) {
							hasError = false;
							System.out
									.println("*** Mismatch in return type at line "
											+ md.returnExp.posn + ".");
						}
					}
				}
				// if the return expression is of newArrayExpr, check if the
				// method return type is array type. Also check if the element
				// type matches
				else if (md.returnExp instanceof NewArrayExpr) {
					
					if (md.type instanceof ArrayType == false) {
						
						hasError = true;
						System.out
								.println("*** Mismatch in return type at line "
										+ md.returnExp.posn + ".");
					} else {
						TypeKind elt = ((ArrayType) md.type).eltType.typeKind;
						TypeKind p = ((ArrayType)md.type).eltType.typeKind;
						
						
						if (elt == TypeKind.INT || elt == TypeKind.BOOLEAN) {
							
							if (p != TypeKind.INT && p != TypeKind.BOOLEAN) {
								hasError = true;
								
								System.out
										.println("*** Mismatch in return type at line "
												+ md.returnExp.posn + ".");
							} else if (elt != p) {
								hasError = true;
								System.out
										.println("*** Mismatch in return type at line "
												+ md.returnExp.posn + ".");
							}
						} else {
							if (p != TypeKind.CLASS) {
								hasError = true;
								System.out
										.println("*** Mismatch in return type at line "
												+ md.returnExp.posn + ".");
							} else {
								String name = ((ClassType) ((ArrayType) md.type).eltType).className.spelling;
								String name2 = ((ClassType) ((NewArrayExpr) (md.returnExp)).eltType).className.spelling;
								if (!name.equals(name2)) {
									hasError = false;
									System.out
											.println("*** Mismatch in return type at line "
													+ md.returnExp.posn + ".");
								}
							}
						}
					}
				}
				// if the return expression is callExpr, check for match in
				// returned type and method return type
				else if (md.returnExp instanceof CallExpr) {

					if (returnKind == TypeKind.ERROR) {
						hasError = true;
						System.out
								.println("*** Mismatch in return type at line "
										+ md.returnExp.posn + ".");
					}
					// call expression returns a int type
					else if (returnKind == TypeKind.INT) {
						if (md.type.typeKind != TypeKind.INT) {
							hasError = true;
							System.out
									.println("*** Mismatch in return type at line "
											+ md.returnExp.posn + ".");
						}
					}
					// call expression returns a boolean type
					else if (returnKind == TypeKind.BOOLEAN) {
						if (md.type.typeKind != TypeKind.INT) {
							hasError = true;
							System.out
									.println("*** Mismatch in return type at line "
											+ md.returnExp.posn + ".");
						}
					}
					// call expression returns a class type
					else if (returnKind == TypeKind.CLASS) {
						if (md.type.typeKind != TypeKind.CLASS) {
							hasError = true;
							System.out
									.println("*** Mismatch in return type at line "
											+ md.returnExp.posn + ".");
						} else {
							String name1 = ((ClassType) (md.type)).className.spelling;
							String name2 = ((ClassType) (methodReturnType)).className.spelling;
							if (!name1.equals(name2)) {
								hasError = true;
								System.out
										.println("*** Mismatch in return type at line "
												+ md.returnExp.posn + ".");
							}
						}
					}
					// call expression returns a array type
					else {
						if (md.type.typeKind != TypeKind.ARRAY) {
							hasError = true;
							System.out
									.println("*** Mismatch in return type at line "
											+ md.returnExp.posn + ".");
						}
						TypeKind k = ((ArrayType) methodReturnType).eltType.typeKind;
						if (k == TypeKind.INT) {
							if (md.type.typeKind != TypeKind.INT) {
								hasError = true;
								System.out
										.println("*** Mismatch in return type at line "
												+ md.returnExp.posn + ".");
							}

						} else if (k == TypeKind.BOOLEAN) {
							if (md.type.typeKind != TypeKind.BOOLEAN) {
								hasError = true;
								System.out
										.println("*** Mismatch in return type at line "
												+ md.returnExp.posn + ".");
							}
						} else {
							String name1 = ((ClassType) (md.type)).className.spelling;
							String name2 = ((ClassType) ((ArrayType) methodReturnType).eltType).className.spelling;
							if (!name1.equals(name2)) {
								hasError = true;
								System.out
										.println("*** Mismatch in return type at line "
												+ md.returnExp.posn + ".");
							}
						}
					}
				}
			}
		}

		staticMethod = false;

		return null;
	}

	@Override
	public Object visitParameterDecl(ParameterDecl pd, String arg) {
		if (pd.type.typeKind == TypeKind.VOID) {
			hasError = true;
			System.out.println("Paramter cannot be of type void. Error at "
					+ pd.posn + ".");
		}
		// check if parameter type can be resolved
		else if (pd.type.typeKind == TypeKind.CLASS) {
			String name = ((ClassType) pd.type).className.spelling;
			if (idTable.retrieve(name) == null
					|| idTable.retrieve(name) instanceof ClassDecl == false) {
				hasError = true;
				System.out.println("Parameter is of unkown type. Error at "
						+ pd.posn + ".");
			}
		}
		idTable.enter(pd.name, pd);

		return null;
	}

	@Override
	public Object visitVarDecl(VarDecl decl, String arg) {
		// variable declared cannot be of type void
		if (decl.type.typeKind == TypeKind.VOID) {
			hasError = true;
			System.out
					.println("*** Invalid type for a variable declaration. Error at "
							+ decl.posn + ".");
		}
		// if declared variable is of class type, check if the class type can be
		// resolved
		else if (decl.type.typeKind == TypeKind.CLASS) {
			String name = ((ClassType) decl.type).className.spelling;
			if (idTable.retrieve(name) == null
					|| idTable.retrieve(name) instanceof ClassDecl == false) {
				boolean error = true;
				for (ClassDecl c : classes) {
					if (c.name.equals(name)) {
						error = false;
						break;
					}
				}
				if (error == true) {
					hasError = true;
					System.out.println("*** " + name
							+ " cannot be resolved to a type. Error at line "
							+ decl.posn + ".");
				}
			}
			ClassDeclName = name;
		}
		// if declared variable is of array type whose element is of class type,
		// check if the class type can be resolved
		else if (decl.type.typeKind == TypeKind.ARRAY) {
			if (((ArrayType) decl.type).eltType.typeKind == TypeKind.CLASS) {
				String name = ((ClassType) ((ArrayType) decl.type).eltType).className.spelling;
				if (idTable.retrieve(name) == null
						|| idTable.retrieve(name) instanceof ClassDecl == false) {
					hasError = true;
					System.out.println("*** " + name
							+ " cannot be resolved to a type. Error at line "
							+ decl.posn + ".");
				}

			}
		}
		// check for duplicate declaration
		Boolean duplicate = idTable.enter(decl.name, decl);
		if (duplicate == true)
			System.out.println("*** Duplicate variable. Error at line "
					+ decl.posn + ".");
		return null;
	}

	@Override
	public Object visitBaseType(BaseType type, String arg) {
		if (type.typeKind == TypeKind.BOOLEAN) {
			return TypeKind.BOOLEAN;
		} else {
			return TypeKind.INT;
		}

	}

	@Override
	public Object visitClassType(ClassType type, String arg) {

		return TypeKind.CLASS;
	}

	@Override
	public Object visitArrayType(ArrayType type, String arg) {

		return TypeKind.ARRAY;

	}

	@Override
	public Object visitBlockStmt(BlockStmt stmt, String arg) {

		if (insideConditional == true && stmt.sl.size() == 1
				&& stmt.sl.get(0) instanceof VarDeclStmt) {

			hasError = true;
			System.out
					.println("*** Local variable is never used. Error at line "
							+ stmt.sl.get(0).posn + ".");
		}
		// visit each statement in the block statement
		idTable.openScope();
		for (Statement st : stmt.sl) {
			st.visit(this, arg);
		}
		idTable.closeScope();
		referenceClassName = currentDecl.name;
		return null;
	}

	@Override
	public Object visitVardeclStmt(VarDeclStmt stmt, String arg) {
		prevRefDecl = null;
		varDeclName = stmt.varDecl;
		// check for case of lonely declaration statement inside a conditional
		if ((insideConditional == true || insideWhile == true)
				&& moreThanOneStatement == false) {
			hasError = true;
			System.out
					.println("***Lonely var declaration statement inside conditional. Error at "
							+ stmt.posn + ".");
		}
		stmt.varDecl.visit(this, arg);
		TypeKind init = (TypeKind) stmt.initExp.visit(this, arg);
		
		TypeKind varKind = stmt.varDecl.type.typeKind;

		if (stmt.initExp instanceof RefExpr) {
			if (((RefExpr) stmt.initExp).ref instanceof IdRef) {

				String spelling = ((IdRef) ((RefExpr) stmt.initExp).ref).id.spelling;
				if (idTable.retrieve(spelling) instanceof VarDecl
						&& spelling.equals(stmt.varDecl.name)) {
					hasError = true;
					System.out
							.println("*** Variable may not have been initialised. Error at line "
									+ stmt.posn + ".");
				}
			}

		}
		// check for valid initializations
		if (varKind == TypeKind.INT) {

			if (init == TypeKind.ARRAY) {
				init = ((ArrayType) stmt.initExp.type).eltType.typeKind;
			}
			if (init != varKind) {
				hasError = true;
				System.out
						.println("*** Invalid variable initialization. Error at line "
								+ stmt.posn + ".");
			}
		} else if (varKind == TypeKind.BOOLEAN) {
			if (init != varKind) {
				hasError = true;
				System.out
						.println("*** Invalid variable initialization. Error at line "
								+ stmt.posn + ".");
			}
		} else if (varKind == TypeKind.CLASS) {
			
			if (init == TypeKind.ARRAY) {
				init = ((ArrayType) stmt.initExp.type).eltType.typeKind;
			}
			if (init != varKind) {
				hasError = true;
				System.out
						.println("*** Invalid variable initialization. Error at line "
								+ stmt.posn + ".");
			}
			
			else if (!expressionClassName.equals(ClassDeclName)) {
				hasError = true;
				System.out
						.println("*** Invalid variable initialization. Error at line "
								+ stmt.posn + ".");
			}
			if (stmt.initExp instanceof RefExpr
					&& ((RefExpr) stmt.initExp).ref instanceof IdRef) {

				String spelling = ((IdRef) ((RefExpr) stmt.initExp).ref).id.spelling;
				if (idTable.retrieve(spelling) instanceof ClassDecl) {
					hasError = true;
					System.out.println("*** Invalid reference. Error at "
							+ stmt.posn + ".");
				}
			}

		} else if (varKind == TypeKind.ARRAY) {
			TypeKind elt = ((ArrayType) stmt.varDecl.type).eltType.typeKind;
			
			if(init!=TypeKind.ARRAY){
				
				hasError = true;
				System.out
						.println("*** Invalid variable initialization. Error at line "
								+ stmt.posn + ".");
			}
			init = ((ArrayType) stmt.initExp.type).eltType.typeKind;
			
			if (init != elt) {
				hasError = true;
				
				System.out
						.println("*** Invalid variable initialization. Error at line "
								+ stmt.posn + ".");
			} else if (elt == TypeKind.CLASS) {
				if (!((ClassType) ((ArrayType) stmt.varDecl.type).eltType).className.spelling
						.equals(expressionClassName)) {
					hasError = true;
					
					System.out
							.println("*** Invalid variable initialization. Error at line "
									+ stmt.posn);
				}

			}

		}
		varDeclName = null;
		referenceClassName = currentDecl.name;
		return null;
	}

	@Override
	public Object visitAssignStmt(AssignStmt stmt, String arg) {
		prevRefDecl = null;
		TypeKind refKind = (TypeKind) stmt.ref.visit(this, arg);
		// referenced variable must be either a field, locally declared
		// variable, or parameter

		if (!fieldDecl && !varDecl && !paramDecl) {

			hasError = true;
			System.out
					.println("*** Left hand should either be a fieldDecl, varDecl, or paramDecl. Error at "
							+ stmt.posn + ".");
		}
		if(stmt.ref instanceof QualifiedRef){
			
			QualifiedRef ref=(QualifiedRef)stmt.ref;
			
			if(ref.ref.returnType==TypeKind.ARRAY && ref.id.spelling.equals("length")){
				
				hasError=true;
				System.out.println("*** length attribute of array can not be assigned. Error at "+stmt.posn+".");
			}
			
		}

		String refClassName = referenceClassName;
		referenceClassName = currentDecl.name;
		TypeKind expKind = (TypeKind) stmt.val.visit(this, arg);
		referenceClassName = currentDecl.name;
		// check for invalid variable reference
		if (refKind == TypeKind.ERROR) {
			hasError = true;
			System.out.println("*** The reference cannot be resolved at line "
					+ stmt.ref.posn + ".");
			System.exit(4);
		}
		// check for invalid assignment expression
		else if (expKind == TypeKind.ERROR) {
			hasError = true;
			System.out.println("*** Invalid assignment expression at line "
					+ stmt.val.posn + ".");
		}
		// check if the assignment is valid
		else {
			if (refKind == TypeKind.CLASS) {
				if(expKind==TypeKind.ARRAY){
					expKind=((ArrayType)stmt.val.type).eltType.typeKind;
				}
				if (expKind != TypeKind.CLASS) {
					hasError = true;
					System.out.println("*** A variable of type " + refKind
							+ " cannot be assigned with a value of type "
							+ expKind + ". Error at line " + stmt.posn + ".");
				} else if (!refClassName.equals(expressionClassName)) {
					hasError = true;
					System.out.println("*** A variable of class "
							+ refClassName
							+ " cannot be assigned with a value of class "
							+ expressionClassName + ". Error at line "
							+ stmt.posn + ".");

				}
			} else if (refKind == TypeKind.INT) {
				if(expKind==TypeKind.ARRAY){
					expKind=((ArrayType)stmt.val.type).eltType.typeKind;
				}
				if (expKind != TypeKind.INT) {
					hasError = true;
					System.out.println("*** A variable of type " + refKind
							+ " cannot be assigned with a value of type "
							+ expKind + ". Error at line " + stmt.posn + ".");
				}
			} else if (refKind == TypeKind.BOOLEAN) {
				if (expKind != TypeKind.BOOLEAN) {
					hasError = true;
					System.out.println("*** A variable of type " + refKind
							+ " cannot be assigned with a value of type "
							+ expKind + ". Error at line " + stmt.posn + ".");
				}

			} else if (refKind == TypeKind.ARRAY) {

				if (expKind == TypeKind.ARRAY) {
					if (baseTypeArray == false) {
						if (expKind != TypeKind.CLASS) {
							hasError = true;
							System.out
									.println("*** An array of class "
											+ refClassName
											+ " cannot be assigned with an array of type "
											+ expKind + ". Error at line "
											+ stmt.posn + ".");
						} else if (!refClassName.equals(expressionClassName)) {
							hasError = true;
							System.out
									.println("*** An array of class "
											+ referenceClassName
											+ " cannot be assigned with an array of class "
											+ expressionClassName
											+ ". Error at line " + stmt.posn
											+ ".");
						}
					} else {
						if (expKind == TypeKind.ARRAY) {
							expKind = ((ArrayType) stmt.val.type).eltType.typeKind;
						}
						if (referenceBaseType != expKind) {
							hasError = true;
							System.out
									.println("*** A array of type "
											+ referenceBaseType
											+ " cannot be assigned with an array of type "
											+ expKind + ". Error at line "
											+ stmt.posn + ".");

						}
					}

				}

				else if (baseTypeArray == false) {
					if (expKind != TypeKind.CLASS) {
						hasError = true;
						System.out.println("*** A cell of array of class "
								+ refClassName
								+ " cannot be assigned with a value of type "
								+ expKind + ". Error at line " + stmt.posn
								+ ".");
					} else if (!refClassName.equals(expressionClassName)) {
						hasError = true;
						System.out.println("*** A cell of array of class "
								+ referenceClassName
								+ " cannot be assigned with a value of class "
								+ expressionClassName + ". Error at line "
								+ stmt.posn + ".");

					}
				} else {
					if (referenceBaseType != expKind) {
						hasError = true;
						System.out.println("*** A cell of array of type "
								+ referenceBaseType
								+ " cannot be assigned with a value of type "
								+ expKind + ". Error at line " + stmt.posn
								+ ".");

					}
				}

			}
		}

		return null;
	}

	@Override
	public Object visitCallStmt(CallStmt stmt, String arg) {
		currentCallStatement = stmt;
		callSt = true;
		prevRefDecl = null;

		// visit each passed in value
		for (Expression exp : stmt.argList) {
			exp.visit(this, arg);

		}
		args = stmt.argList;
		referenceClassName = currentDecl.name;
		prevRefDecl = null;
		TypeKind methodKind = (TypeKind) stmt.methodRef.visit(this, arg);

		// called method does not exist
		if (methodKind == TypeKind.ERROR) {
			hasError = true;
			System.out.println("*** Method called at line " + stmt.posn
					+ " is not declared.");
			System.exit(4);
		}
		referenceClassName = currentDecl.name;
		callSt = false;
		return null;
	}

	@Override
	public Object visitIfStmt(IfStmt stmt, String arg) {
		TypeKind condition = (TypeKind) stmt.cond.visit(this, arg);
		// check for valid if conditional expression
		if (condition != TypeKind.BOOLEAN) {
			hasError = true;
			System.out.println("*** Invalid if condition at line "
					+ stmt.cond.posn + ".");
		}

		idTable.openScope();
		insideConditional = true;
		if (stmt.thenStmt instanceof BlockStmt) {
			if (((BlockStmt) stmt.thenStmt).sl.size() > 1)
				moreThanOneStatement = true;
		}
		stmt.thenStmt.visit(this, arg);
		insideConditional = false;
		moreThanOneStatement = false;
		idTable.closeScope();

		if (stmt.elseStmt != null) {
			idTable.openScope();
			insideConditional = true;
			if (stmt.elseStmt instanceof BlockStmt) {
				if (((BlockStmt) stmt.elseStmt).sl.size() > 1)
					moreThanOneStatement = true;
			}
			stmt.elseStmt.visit(this, arg);
			insideConditional = false;
			moreThanOneStatement = false;
			idTable.closeScope();
		}
		return null;
	}

	@Override
	public Object visitWhileStmt(WhileStmt stmt, String arg) {

		TypeKind condition = (TypeKind) stmt.cond.visit(this, arg);
		// check for valid while conditional expression
		if (condition != TypeKind.BOOLEAN) {
			hasError = true;
			System.out.println("*** Invalid while condition at line "
					+ stmt.cond.posn + ".");
		}
		idTable.openScope();
		insideWhile = true;
		if (stmt.body instanceof BlockStmt) {
			if (((BlockStmt) stmt.body).sl.size() > 1)
				moreThanOneStatement = true;
		}
		stmt.body.visit(this, arg);
		insideWhile = false;
		moreThanOneStatement = false;
		idTable.closeScope();
		referenceClassName = currentDecl.name;
		return null;
	}

	@Override
	public Object visitUnaryExpr(UnaryExpr expr, String arg) {
		TypeKind r = (TypeKind) expr.expr.visit(this, arg);
		String op = (String) expr.operator.visit(this, arg);
		// - operator can only be applied on type int
		if (op.equals("-")) {
			if (r != TypeKind.INT) {
				hasError = true;
				System.out.println("*** Illegal operation at line " + expr.posn
						+ ".");
				expr.returnType = TypeKind.ERROR;
				expr.type = new BaseType(expr.returnType, expr.posn);
			} else {
				expr.returnType = TypeKind.INT;
				expr.type = new BaseType(expr.returnType, expr.posn);
			}
		}
		// ! operator can only be applied on type boolean
		else if (op.equals("!")) {
			if (r != TypeKind.BOOLEAN) {
				hasError = true;
				System.out.println("*** Illegal operation at line " + expr.posn
						+ ".");
				expr.returnType = TypeKind.ERROR;
				expr.type = new BaseType(expr.returnType, expr.posn);
			} else {
				expr.returnType = TypeKind.BOOLEAN;
				expr.type = new BaseType(expr.returnType, expr.posn);
			}

		}
		// unary operator applied on wrong type
		else {
			hasError = true;
			System.out.println("*** Illegal operator at line "
					+ expr.operator.posn + ".");
			expr.returnType = TypeKind.ERROR;
			expr.type = new BaseType(expr.returnType, expr.posn);
		}
		return expr.returnType;
	}

	@Override
	public Object visitBinaryExpr(BinaryExpr expr, String arg) {
		TypeKind l = (TypeKind) expr.left.visit(this, arg);
		referenceClassName = currentDecl.name;
		TypeKind r = (TypeKind) expr.right.visit(this, arg);
		referenceClassName = currentDecl.name;
		String op = (String) expr.operator.visit(this, arg);

		// +,-,/,* operators can only be applied on type int
		if (op.equals("+") || op.equals("-") || op.equals("*")
				|| op.equals("/")) {
			if (l == TypeKind.ARRAY) {
				l = ((ArrayType) expr.left.type).eltType.typeKind;
			}
			if (r == TypeKind.ARRAY) {
				r = ((ArrayType) expr.right.type).eltType.typeKind;
			}
			if (l != TypeKind.INT || r != TypeKind.INT) {
				hasError = true;
				System.out.println("*** Illegal operation at line " + expr.posn
						+ ".");
				expr.returnType = TypeKind.ERROR;
				expr.type = new BaseType(expr.returnType, expr.posn);
			} else {
				expr.returnType = TypeKind.INT;
				expr.type = new BaseType(expr.returnType, expr.posn);
			}
		}
		// <,<=,>,>= operators can only be applied on type int
		else if (op.equals("<") || op.equals(">") || op.equals("<=")
				|| op.equals(">=")) {
			if (l == TypeKind.ARRAY) {
				l = ((ArrayType) expr.left.type).eltType.typeKind;
			}
			if (r == TypeKind.ARRAY) {
				r = l = ((ArrayType) expr.right.type).eltType.typeKind;
			}
			if (l != TypeKind.INT || r != TypeKind.INT) {
				hasError = true;
				System.out.println("*** Illegal operation at line " + expr.posn
						+ ".");
				expr.returnType = TypeKind.ERROR;
				expr.type = new BaseType(expr.returnType, expr.posn);
			} else {
				expr.returnType = TypeKind.BOOLEAN;
				expr.type = new BaseType(expr.returnType, expr.posn);
			}
		}
		// ||,&& operators can only be applied on type boolean
		else if (op.equals("||") || op.equals("&&")) {
			if (l != TypeKind.BOOLEAN || r != TypeKind.BOOLEAN) {
				hasError = true;
				System.out.println("*** Illegal operation at line " + expr.posn
						+ ".");
				expr.returnType = TypeKind.ERROR;
				expr.type = new BaseType(expr.returnType, expr.posn);
			} else {
				expr.returnType = TypeKind.BOOLEAN;
				expr.type = new BaseType(expr.returnType, expr.posn);

			}
		}
		// ==,!= operators can only be applied on two sides of same type
		else if (op.equals("==") || op.equals("!=")) {

			if (l == TypeKind.INT && r == TypeKind.INT) {
				expr.returnType = TypeKind.BOOLEAN;
				expr.type = new BaseType(expr.returnType, expr.posn);
			} else if (l == TypeKind.BOOLEAN && r == TypeKind.BOOLEAN) {

				expr.returnType = TypeKind.BOOLEAN;
				expr.type = new BaseType(expr.returnType, expr.posn);
			} else if (l == TypeKind.CLASS && r == TypeKind.CLASS) {

				String name = ((ClassType) expr.left.type).className.spelling;
				String name2 = ((ClassType) expr.right.type).className.spelling;
				if (!name.equals(name2)) {
					hasError = true;
					System.out
							.println("*** Type mismatch in operation. Error at line "
									+ expr.posn + ".");
					expr.returnType = TypeKind.ERROR;
					expr.type = new BaseType(expr.returnType, expr.posn);
				} else {
					expr.returnType = TypeKind.BOOLEAN;
					expr.type = new BaseType(expr.returnType, expr.posn);
				}
			} else if (l == TypeKind.ARRAY && r == TypeKind.ARRAY) {

				TypeKind le = ((ArrayType) expr.left.type).eltType.typeKind;

				TypeKind re = ((ArrayType) expr.right.type).eltType.typeKind;

				if (le == TypeKind.INT && re == TypeKind.INT) {
					expr.returnType = TypeKind.BOOLEAN;
					expr.type = new BaseType(expr.returnType, expr.posn);

				} else if (le == TypeKind.CLASS && re == TypeKind.CLASS) {
					String name = ((ClassType) ((ArrayType) expr.left.type).eltType).className.spelling;
					String name2 = ((ClassType) ((ArrayType) expr.right.type).eltType).className.spelling;
					if (!name.equals(name2)) {
						hasError = true;
						System.out
								.println("*** Type mismatch in operation. Error at line "
										+ expr.posn + ".");
						expr.returnType = TypeKind.ERROR;
						expr.type = new BaseType(expr.returnType, expr.posn);
					} else {
						expr.returnType = TypeKind.BOOLEAN;
						expr.type = new BaseType(expr.returnType, expr.posn);
					}
				} else {
					hasError = true;
					System.out
							.println("*** Type mismatch in operation. Error at line "
									+ expr.posn + ".");
					expr.returnType = TypeKind.ERROR;
					expr.type = new BaseType(expr.returnType, expr.posn);
				}
			} else if (l == TypeKind.ARRAY && r == TypeKind.INT) {
				TypeKind le = ((ArrayType) expr.left.type).eltType.typeKind;
				if (le != TypeKind.INT) {
					hasError = true;
					System.out
							.println("*** TYpe mismatch in operation. Error at line "
									+ expr.posn + ".");
					expr.returnType = TypeKind.ERROR;
					expr.type = new BaseType(expr.returnType, expr.posn);
				} else {
					expr.returnType = TypeKind.BOOLEAN;
					expr.type = new BaseType(expr.returnType, expr.posn);
				}
			} 
			else if (l == TypeKind.ARRAY && r == TypeKind.CLASS) {
				Type le = ((ArrayType) expr.left.type).eltType;
				if (le.typeKind != TypeKind.CLASS) {
					hasError = true;
					System.out
							.println("*** TYpe mismatch in operation. Error at line "
									+ expr.posn + ".");
					expr.returnType = TypeKind.ERROR;
					expr.type = new BaseType(expr.returnType, expr.posn);
				}
				
				else {
					String name2 = ((ClassType) ((ArrayType) expr.right.type).eltType).className.spelling;
					if(!((ClassType)le).className.spelling.equals(name2)){
						
						hasError = true;
						System.out
								.println("*** TYpe mismatch in operation. Error at line "
										+ expr.posn + ".");
						expr.returnType = TypeKind.ERROR;
						expr.type = new BaseType(expr.returnType, expr.posn);
					}
					else{
						expr.returnType = TypeKind.BOOLEAN;
						expr.type = new BaseType(expr.returnType, expr.posn);
					}
				}
			}
			
			else if (r == TypeKind.ARRAY && l == TypeKind.CLASS) {
				Type re = ((ArrayType) expr.right.type).eltType;
				if (re.typeKind != TypeKind.CLASS) {
					hasError = true;
					System.out
							.println("*** TYpe mismatch in operation. Error at line "
									+ expr.posn + ".");
					expr.returnType = TypeKind.ERROR;
					expr.type = new BaseType(expr.returnType, expr.posn);
				}
				
				else {
					String name2 = ((ClassType) ((ArrayType) expr.left.type).eltType).className.spelling;
					if(!((ClassType)re).className.spelling.equals(name2)){
						
						hasError = true;
						System.out
								.println("*** TYpe mismatch in operation. Error at line "
										+ expr.posn + ".");
						expr.returnType = TypeKind.ERROR;
						expr.type = new BaseType(expr.returnType, expr.posn);
					}
					else{
						expr.returnType = TypeKind.BOOLEAN;
						expr.type = new BaseType(expr.returnType, expr.posn);
					}
				}
			}
			else if (r == TypeKind.ARRAY && l == TypeKind.INT) {
				TypeKind re = ((ArrayType) expr.right.type).eltType.typeKind;
				if (re != TypeKind.INT) {
					hasError = true;
					System.out
							.println("*** TYpe mismatch in operation. Error at line "
									+ expr.posn + ".");
					expr.returnType = TypeKind.ERROR;
					expr.type = new BaseType(expr.returnType, expr.posn);
				} else {
					expr.returnType = TypeKind.BOOLEAN;
					expr.type = new BaseType(expr.returnType, expr.posn);
				}
			}
			// binary operator is applied on wrong type
			else {
				hasError = true;

				System.out
						.println("*** Type mismatch in operation. Error at line "
								+ expr.operator.posn + ".");
				expr.returnType = TypeKind.ERROR;
				expr.type = new BaseType(expr.returnType, expr.posn);
			}
		}
		return expr.returnType;

	}

	@Override
	public Object visitRefExpr(RefExpr expr, String arg) {
		prevRefDecl = null;
		expr.returnType = (TypeKind) expr.ref.visit(this, arg);
		if (expr.returnType == TypeKind.CLASS)
			expressionClassName = referenceClassName;
		expr.type = expr.ref.type;
		referenceClassName=currentDecl.name;
		return expr.returnType;
	}

	@Override
	public Object visitCallExpr(CallExpr expr, String arg) {
		prevRefDecl = null;
		currentCallExpression = expr;
		callExp = true;
		for (Expression p : expr.argList) {
			p.visit(this, arg);
		}
		args = expr.argList;
		prevRefDecl = null;
		expr.returnType = (TypeKind) expr.functionRef.visit(this, arg);
		// System.out.println(expr.returnType);
		expr.type = expr.functionRef.type;
		callExp = false;
		referenceClassName=currentDecl.name;
		return expr.returnType;
	}

	@Override
	public Object visitLiteralExpr(LiteralExpr expr, String arg) {
		expr.returnType = (TypeKind) expr.literal.visit(this, arg);
		expr.type = expr.literal.type;
		return expr.returnType;
	}

	@Override
	public Object visitNewObjectExpr(NewObjectExpr expr, String arg) {

		expr.returnType = TypeKind.CLASS;
		expressionClassName = expr.classtype.className.spelling;

		expr.type = new ClassType(
				new Identifier(expressionClassName, expr.posn), expr.posn);
		for (ClassDecl c : classes) {
			if (c.name.equals(expressionClassName)) {
				expr.classtype.classDecl = c;
				break;
			}
		}
		return expr.returnType;
	}

	@Override
	public Object visitNewArrayExpr(NewArrayExpr expr, String arg) {
		expr.returnType = TypeKind.ARRAY;
		expr.type = new ArrayType(expr.eltType, expr.posn);
		if (expr.eltType.typeKind == TypeKind.CLASS) {
			expressionClassName = ((ClassType) expr.eltType).className.spelling;
		}
		if (expr.sizeExpr.visit(this, arg) != TypeKind.INT) {
			hasError = true;
			System.out.println("*** Invalid array creation. Error at "
					+ expr.posn + ".");
		}
		return expr.returnType;
	}

	@Override
	public Object visitQualifiedRef(QualifiedRef ref, String arg) {

		underRef = true;
		TypeKind refType = (TypeKind) ref.ref.visit(this, arg);
		if (refType == TypeKind.ARRAY
				&& (ref.ref instanceof IdRef || ref.ref instanceof QualifiedRef)
				&& ref.id.spelling.equals("length")) {
			ref.returnType = TypeKind.INT;
		}
		// no access through a method in qualified reference
		if (prevRefDecl instanceof MethodDecl
				&& !prevRefDecl.name.equals(ref.id.spelling)) {

			hasError = true;
			System.out
					.println("*** No access through a method in qualified reference. Error at "
							+ ref.posn + ".");
		}

		// check for reference that cannot be resolved
		if (refType == TypeKind.ERROR) {
			hasError = true;
			System.out.println("*** Unkown reference. Error at line "
					+ ref.posn + ".");
			ref.returnType = TypeKind.ERROR;
			ref.type = new BaseType(ref.returnType, ref.posn);
			System.exit(4);
		}
		// check for specific cases
		else if (refType == TypeKind.CLASS || (refType==TypeKind.ARRAY && !ref.id.spelling.equals("length"))) {

			Declaration d = idTable.retrieve(referenceName);

			if (d == null) {
				d = idTable.retrieve(referenceClassName);
			}

			Declaration cl = d;
			ref.decl = d;
			if (d instanceof FieldDecl || d instanceof VarDecl || d instanceof ParameterDecl) {

				if (d.type.typeKind == TypeKind.CLASS) {
					String name = ((ClassType) d.type).className.spelling;
					cl = idTable.retrieve(name);
				}
				if (d.type.typeKind==TypeKind.ARRAY){
					if(((ArrayType)d.type).eltType.typeKind==TypeKind.CLASS){
						String name2 = ((ClassType)((ArrayType)d.type).eltType).className.spelling;
						cl=idTable.retrieve(name2);
					}
				}
				if (cl instanceof ClassDecl == false) {
					for (ClassDecl c : classes) {
						if (c.name.equals(d.name)) {
							cl = c;
							break;
						}
					}
				}

			}

			if (cl instanceof ClassDecl) {

				if (!cl.name.equals(currentDecl.name)) {

					Declaration item = null;
					for (FieldDecl f : ((ClassDecl) cl).fieldDeclList) {

						if (f.name.equals(ref.id.spelling)) {

							item = f;
							break;
						}
					}
					for (MethodDecl m : ((ClassDecl) cl).methodDeclList) {

						if (m.name.equals(ref.id.spelling)) {
							item = m;

							break;
						}
					}
					prevRefDecl = item;

					ref.id.decl = item;
					if (item == null) {

						hasError = true;
						System.out
								.println("*** Reference cannot be resolved. Error at line "
										+ ref.posn + ".");
						ref.returnType = TypeKind.ERROR;
						ref.type = new BaseType(ref.returnType, ref.posn);
						System.exit(4);
					} else if (item instanceof FieldDecl) {

						fieldDecl = true;

						if (((FieldDecl) item).isStatic == false
								&& idTable.retrieve(referenceName) instanceof ClassDecl
								&& ref.ref instanceof ThisRef == false) {
							hasError = true;
							System.out.println("*** Referenced variable "
									+ ref.id.spelling
									+ " is not static at line " + ref.posn
									+ ".");

						}
						if (((FieldDecl) item).isStatic == true
								&& idTable.retrieve(referenceName) instanceof ClassDecl == false) {
							hasError = true;
							System.out
									.println("*** Referenced variable "
											+ ref.id.spelling
											+ " should be statically accessed. Error at line "
											+ ref.posn + ".");

						}

						else if (((FieldDecl) item).isPrivate == true) {
							hasError = true;
							System.out.println("*** Referenced variable "
									+ ref.id.spelling
									+ " is private. Error at line " + ref.posn
									+ ".");
						}
						if (item.type.typeKind == TypeKind.CLASS) {

							referenceClassName = ((ClassType) item.type).className.spelling;
							referenceName = item.name;

						}
						ref.returnType = item.type.typeKind;
						ref.type = item.type;

					} else if (item instanceof MethodDecl) {
						fieldDecl = false;
						varDecl = false;
						paramDecl = false;
						if (((MethodDecl) item).isStatic == false
								&& idTable.retrieve(referenceName) instanceof ClassDecl
								&& ref.ref instanceof ThisRef == false) {
							hasError = true;
							System.out.println("*** Referenced method "
									+ ref.id.spelling
									+ " is not static. Error at line "
									+ ref.posn + ".");

						}
						if (((MethodDecl) item).isStatic == true
								&& d instanceof ClassDecl == false) {
							hasError = true;
							System.out
									.println("*** Referenced method "
											+ ref.id.spelling
											+ " should be statically accessed. Error at line "
											+ ref.posn + ".");

						}

						else if (((MethodDecl) item).isPrivate == true) {
							hasError = true;
							System.out.println("*** Referenced method "
									+ ref.id.spelling
									+ " is private. Error at line " + ref.posn
									+ ".");
						} else {
							ParameterDeclList param = ((MethodDecl) item).parameterDeclList;
							if (callSt || callExp) {
								if (param.size() != args.size()) {
									hasError = true;
									System.out.println("*** Method "
											+ item.name + " expects "
											+ param.size()
											+ " parameters but only gets "
											+ args.size() + ". Error at line "
											+ ref.posn + ".");

								} else {

									for (int i = 0; i < param.size(); i++) {
										//param.get(i).visit(this, arg);
										TypeKind exp = param.get(i).type.typeKind;
										TypeKind given = args.get(i).returnType;
										if (given == TypeKind.ARRAY) {
											given = ((ArrayType) args.get(i).type).eltType.typeKind;
										}
										if (exp != given) {
											hasError = true;
											System.out
													.println("*** Method "
															+ item.name
															+ " expects a parameter of type "
															+ exp
															+ " but gets a value of type "
															+ ((given == TypeKind.CLASS) ? ((ClassType) ((ArrayType) args
																	.get(i).type).eltType).className.spelling
																	: given)
															+ ". Error at line "
															+ ref.posn + ".");
										}
									}
								}
							}
						}
						ref.returnType = item.type.typeKind;
						ref.type = item.type;
						referenceName = item.name;
						if(callExp){
							if(((MethodDecl)item).type instanceof ClassType)
								expressionClassName=((ClassType)((MethodDecl)item).type).className.spelling;
						}
					}
				} else {
					Declaration item = null;
					for (FieldDecl f : ((ClassDecl) cl).fieldDeclList) {

						if (f.name.equals(ref.id.spelling)) {

							item = f;
							break;
						}
					}
					for (MethodDecl m : ((ClassDecl) cl).methodDeclList) {

						if (m.name.equals(ref.id.spelling)) {
							item = m;

							break;
						}
					}
					prevRefDecl = item;
					ref.id.decl = item;
					if (item == null) {

						hasError = true;
						System.out
								.println("*** Reference cannot be resolved. Error at line "
										+ ref.posn + ".");
						ref.returnType = TypeKind.ERROR;
						ref.type = new BaseType(ref.returnType, ref.posn);
						System.exit(4);
					} else if (item instanceof FieldDecl) {

						fieldDecl = true;

						if (((FieldDecl) item).isStatic == false
								&& idTable.retrieve(referenceName) instanceof ClassDecl
								&& ref.ref instanceof ThisRef == false) {
							hasError = true;

							System.out.println("*** Referenced variable "
									+ ref.id.spelling
									+ " is not static at line " + ref.posn
									+ ".");

						}

						if (((FieldDecl) item).isStatic == true
								&& d instanceof ClassDecl == false) {
							hasError = true;
							System.out
									.println("*** Referenced variable "
											+ ref.id.spelling
											+ " should be statically accessed. Error at line "
											+ ref.posn + ".");

						}

						if (item.type.typeKind == TypeKind.CLASS) {

							referenceClassName = ((ClassType) item.type).className.spelling;
							referenceName = item.name;

						}
						ref.returnType = item.type.typeKind;
						ref.type = item.type;

					} else if (item instanceof MethodDecl) {
						fieldDecl = false;
						varDecl = false;
						paramDecl = false;
						if (((MethodDecl) item).isStatic == false
								&& idTable.retrieve(referenceName) instanceof ClassDecl
								&& ref.ref instanceof ThisRef == false) {
							hasError = true;
							System.out.println("*** Referenced method "
									+ ref.id.spelling
									+ " is not static. Error at line "
									+ ref.posn + ".");

						}
						if (((MethodDecl) item).isStatic == true
								&& d instanceof ClassDecl == false) {
							hasError = true;
							System.out
									.println("*** Referenced method "
											+ ref.id.spelling
											+ " should be statically accessed. Error at line "
											+ ref.posn + ".");

						}

						else if (((MethodDecl) item).isPrivate == true) {
							hasError = true;
							System.out.println("*** Referenced method "
									+ ref.id.spelling
									+ " is private. Error at line " + ref.posn
									+ ".");
						} else {
							ParameterDeclList param = ((MethodDecl) item).parameterDeclList;
							if (callSt || callExp) {
								if (param.size() != args.size()) {
									hasError = true;
									System.out.println("*** Method "
											+ item.name + " expects "
											+ param.size()
											+ " parameters but only gets "
											+ args.size() + ". Error at line "
											+ ref.posn + ".");

								} else {

									for (int i = 0; i < param.size(); i++) {
										//param.get(i).visit(this, arg);
										TypeKind exp = param.get(i).type.typeKind;
										TypeKind given = args.get(i).returnType;
										if (given == TypeKind.ARRAY) {
											given = ((ArrayType) args.get(i).type).eltType.typeKind;
										}
										if (exp != given) {
											hasError = true;
											System.out
													.println("*** Method "
															+ item.name
															+ " expects a parameter of type "
															+ exp
															+ " but gets a value of type "
															+ ((given == TypeKind.CLASS) ? ((ClassType) ((ArrayType) args
																	.get(i).type).eltType).className.spelling
																	: given)
															+ ". Error at line "
															+ ref.posn + ".");
										}
									}
								}
							}
						}
						ref.returnType = item.type.typeKind;
						ref.type = item.type;
						referenceName = item.name;
						if(callExp){
							if(((MethodDecl)item).type instanceof ClassType)
								expressionClassName=((ClassType)((MethodDecl)item).type).className.spelling;
						}
					}
				}

			} else if (refType == TypeKind.ARRAY) {
				String refName = referenceClassName;

				Declaration decl = idTable.retrieve(refName);
				ref.decl = decl;
				if (decl instanceof ClassDecl == false) {
					for (ClassDecl c : classes) {
						if (c.name.equals(decl.name)) {
							decl = c;
							break;
						}
					}
				}
				referenceClassName = currentDecl.name;
				Declaration item = null;
				for (FieldDecl f : ((ClassDecl) decl).fieldDeclList) {
					if (f.name.equals(ref.id.spelling)) {
						fieldDecl = true;
						item = f;
						ref.returnType = f.type.typeKind;
						ref.type = f.type;
						if (f.isPrivate == true
								&& !refName.equals(currentDecl.name)) {
							hasError = true;
							System.out.println("*** Referenced variable "
									+ ref.id.spelling
									+ " is private. Error at line " + ref.posn
									+ ".");
						}
						if (f.isStatic == true
								&& idTable.retrieve(referenceName) instanceof ClassDecl == false) {
							hasError = true;
							System.out
									.println("*** Referenced variable "
											+ ref.id.spelling
											+ " should be statically accessed. Error at line "
											+ ref.posn + ".");

						}
						break;
					}
				}
				for (MethodDecl m : ((ClassDecl) decl).methodDeclList) {
					if (m.name.equals(ref.id.spelling)) {
						fieldDecl = false;
						varDecl = false;
						paramDecl = false;
						item = m;
						ref.returnType = m.type.typeKind;
						ref.type = m.type;
						if (m.isPrivate == true
								&& !refName.equals(currentDecl.name)) {
							hasError = true;
							System.out.println("*** Referenced method "
									+ ref.id.spelling
									+ " is private. Error at line " + ref.posn
									+ ".");
						}
						if (m.isStatic == true
								&& idTable.retrieve(referenceName) instanceof ClassDecl == false) {
							hasError = true;
							System.out
									.println("*** Referenced method "
											+ ref.id.spelling
											+ " should be statically accessed. Error at line "
											+ ref.posn + ".");

						}
						referenceName = m.name;
						break;
					}
				}
				prevRefDecl = item;
				ref.id.decl = item;
				if (item == null) {
					hasError = true;
					System.out
							.println("*** Referenced variable or method does not exist. Error at line "
									+ ref.posn + ".");
					ref.returnType = TypeKind.ERROR;
					ref.type = new BaseType(ref.returnType, ref.posn);
					System.exit(4);
				}

				if (item.type.typeKind == TypeKind.ARRAY) {
					TypeKind eltType = ((ArrayType) item.type).eltType.typeKind;
					if (eltType == TypeKind.CLASS) {
						referenceClassName = ((ClassType) ((ArrayType) item.type).eltType).className.spelling;
						baseTypeArray = false;
						referenceName = referenceClassName;
					} else {
						baseTypeArray = true;
						referenceBaseType = eltType;
					}
				}
			}

		} else if (refType == TypeKind.BOOLEAN || refType == TypeKind.INT) {
			ref.returnType = TypeKind.ERROR;
			ref.type = new BaseType(ref.returnType, ref.posn);
		}

		underRef = false;

		return ref.returnType;
	}

	@Override
	public Object visitIndexedRef(IndexedRef ref, String arg) {

		TypeKind exprKind = (TypeKind) ref.indexExpr.visit(this, arg);
		TypeKind refKind = (TypeKind) ref.ref.visit(this, arg);
		
		if (refKind != TypeKind.ARRAY) {
			hasError = true;
			System.out.println("*** Invalid indexing at line " + ref.posn);
			ref.returnType = TypeKind.ERROR;
			ref.type = new BaseType(ref.returnType, ref.posn);
		}
		// check if index is of type int
		else if (exprKind != TypeKind.INT) {
			hasError = true;
			System.out.println("*** The index should be an int at line "
					+ ref.posn + ".");
			ref.returnType = TypeKind.ERROR;
			ref.type = new BaseType(ref.returnType, ref.posn);
		} else if (refKind == TypeKind.ARRAY) {
			ref.returnType = refKind;
			ref.type = ref.ref.type;
		} else {
			hasError = true;
			ref.returnType = TypeKind.ERROR;
			ref.type = new BaseType(ref.returnType, ref.posn);
		}
		
		return ref.returnType;
	}

	@Override
	public Object visitIdRef(IdRef ref, String arg) {

		ref.returnType = (TypeKind) ref.id.visit(this, arg);
		ref.decl = ref.id.decl;
		ref.type = ref.id.type;
		return ref.returnType;
	}

	@Override
	public Object visitThisRef(ThisRef ref, String arg) {
		ref.returnType = TypeKind.CLASS;
		ref.type = currentDecl.type;
		if (staticMethod == true) {
			hasError = true;
			System.out
					.println("*** Cannot use this reference in a static context. Error at line "
							+ ref.posn);
		}
		referenceClassName = currentDecl.name;
		referenceName = currentDecl.name;
		return ref.returnType;
	}

	@Override
	public Object visitIdentifier(Identifier id, String arg) {
		
		if (referenceClassName.equals(currentDecl.name)) {

			Declaration decl = idTable.retrieve(id.spelling);

			if (decl == null) {
				ClassDecl c = (ClassDecl) currentDecl;
				for (FieldDecl f : c.fieldDeclList) {
					if (f.name.equals(id.spelling)) {
						decl = f;
						break;
					}
				}
				for (MethodDecl m : c.methodDeclList) {
					if (m.name.equals(id.spelling)) {
						decl = m;
						break;
					}
				}
			}
			id.decl = decl;

			if (decl == null) {

				hasError = true;
				id.returnType = TypeKind.ERROR;
				id.type = new BaseType(id.returnType, id.posn);
			} else if (decl instanceof ClassDecl) {

				id.returnType = decl.type.typeKind;
				id.type = decl.type;
				referenceClassName = ((ClassType) decl.type).className.spelling;
				referenceName = decl.name;
			}

			else {

				if (decl instanceof FieldDecl) {
					fieldDecl = true;

					if (decl.type.typeKind == TypeKind.CLASS) {
						referenceClassName = ((ClassType) decl.type).className.spelling;
						referenceName = decl.name;
						for (ClassDecl c : classes) {
							if (c.name
									.equals(((ClassType) decl.type).className.spelling)) {
								((ClassType) decl.type).classDecl = c;
								break;
							}
						}
					}
					if (staticMethod == true) {

						if (((FieldDecl) decl).isStatic == false
								&& underRef == false) {
							hasError = true;
							System.out
									.println("*** Cannot access a non-static variable inside a static method. Error at line "
											+ id.posn + ".");

						}
					}
					id.returnType = decl.type.typeKind;
					id.type = decl.type;
				} else if (decl instanceof MethodDecl) {
					fieldDecl = false;
					varDecl = false;
					paramDecl = false;
					if (callSt == false && callExp == false) {
						for (ClassDecl c : classes) {
							if (c.name.equals(decl.name)) {
								id.returnType = c.type.typeKind;
								id.type = c.type;
								break;
							}
						}
					}

					else if (callSt || callExp) {

						ParameterDeclList param = ((MethodDecl) decl).parameterDeclList;
						if (param.size() != args.size()) {
							hasError = true;
							System.out
									.println("*** Method "
											+ decl.name
											+ " expect "
											+ param.size()
											+ " parameters. But get "
											+ args.size()
											+ ". Error at "
											+ ((callSt) ? currentCallStatement.posn
													: currentCallExpression.posn)
											+ ".");

						} else {
							for (int i = 0; i < param.size(); i++) {
								//param.get(i).visit(this, arg);
								TypeKind exp = param.get(i).type.typeKind;
								TypeKind given = args.get(i).returnType;

								if (given != exp) {
									hasError = true;
									System.out.println("*** Method "
											+ decl.name
											+ " expects a parameter of type "
											+ exp
											+ " but gets a value of type "
											+ given + ". Error at "
											+ args.get(i).posn + ".");
								}
							}
						}
						methodReturnType = decl.type;
						if (staticMethod == true) {

							if (idTable.userDefine(id.spelling)
									&& ((MethodDecl) decl).isStatic == false) {

								hasError = true;
								System.out
										.println("*** Cannot call a non-static method inside a static method. Error at line "
												+ id.posn + ".");

							}
						}
						id.returnType = decl.type.typeKind;
						id.type = decl.type;
						
						if(((MethodDecl)decl).type.typeKind==TypeKind.CLASS){
							expressionClassName=((ClassType)((MethodDecl)decl).type).className.spelling;
							
						}
						else if(((MethodDecl)decl).type.typeKind==TypeKind.ARRAY){
							if(((ArrayType)((MethodDecl)decl).type).eltType.typeKind==TypeKind.CLASS){
								expressionClassName=((ClassType)((ArrayType)((MethodDecl)decl).type).eltType).className.spelling;
							}
						}
					}

				} else if (decl instanceof ParameterDecl) {
					id.returnType = decl.type.typeKind;
					id.type = decl.type;
					paramDecl = true;
					if (decl.type.typeKind == TypeKind.CLASS) {
						for (ClassDecl c : classes) {
							if (c.name
									.equals(((ClassType) decl.type).className.spelling)) {
								((ClassType) decl.type).classDecl = c;
								break;
							}
						}
					}

				} else if (decl instanceof VarDecl) {
					varDecl = true;
					id.returnType = decl.type.typeKind;

					if (varDeclName != null
							&& decl.name.equals(varDeclName.name)) {
						hasError = true;
						System.out.println("*** Variable " + decl.name
								+ " may not have been initiliazed. Error at "
								+ varDeclName.posn + ".");
					}
					id.type = decl.type;
					if (id.returnType == TypeKind.CLASS) {
						referenceClassName = ((ClassType) decl.type).className.spelling;
						referenceName = decl.name;
						for (ClassDecl c : classes) {
							if (c.name
									.equals(((ClassType) decl.type).className.spelling)) {
								((ClassType) decl.type).classDecl = c;
								break;
							}
						}
					}

				}

				if (decl.type instanceof ArrayType) {
					TypeKind eltType = ((ArrayType) decl.type).eltType.typeKind;
					id.returnType = decl.type.typeKind;
					id.type = decl.type;
					if (eltType == TypeKind.CLASS) {
						referenceClassName = ((ClassType) ((ArrayType) decl.type).eltType).className.spelling;
						referenceName = decl.name;
						baseTypeArray = false;
					} else {
						baseTypeArray = true;
						referenceBaseType = eltType;
					}

				}
			}
		}
		
		return id.returnType;

	}

	@Override
	public Object visitOperator(Operator op, String arg) {

		return op.spelling;
	}

	@Override
	public Object visitIntLiteral(IntLiteral num, String arg) {
		num.type = new BaseType(TypeKind.INT, num.posn);
		return TypeKind.INT;
	}

	@Override
	public Object visitBooleanLiteral(BooleanLiteral bool, String arg) {
		bool.type = new BaseType(TypeKind.BOOLEAN, bool.posn);
		return TypeKind.BOOLEAN;
	}

}
