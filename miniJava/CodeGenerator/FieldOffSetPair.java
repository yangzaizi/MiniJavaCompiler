package miniJava.CodeGenerator;

import miniJava.AbstractSyntaxTrees.FieldDecl;

public class FieldOffSetPair {
	
	FieldDecl decl;
	int offset;
	
	public FieldOffSetPair(FieldDecl m, int off){
		decl=m;
		offset=off;
	}

}
