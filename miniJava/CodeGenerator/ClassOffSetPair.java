package miniJava.CodeGenerator;

import miniJava.AbstractSyntaxTrees.ClassDecl;

public class ClassOffSetPair {
	
	ClassDecl decl;
	int offset;
	
	public ClassOffSetPair(ClassDecl cl, int off){
		decl=cl;
		offset=off;
	}

}
