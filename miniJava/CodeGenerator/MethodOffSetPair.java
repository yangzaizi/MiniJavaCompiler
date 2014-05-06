package miniJava.CodeGenerator;

import miniJava.AbstractSyntaxTrees.MethodDecl;



public class MethodOffSetPair {

	MethodDecl decl;
	int offset;
	
	public MethodOffSetPair(MethodDecl m, int off){
		decl=m;
		offset=off;
	}
}
