/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntaticAnalyzer.SourcePosition;

public abstract class Expression extends AST {
  
  public TypeKind returnType;
  public Type type;
  public Expression(SourcePosition posn) {
    super (posn);
    
  }
  
}
