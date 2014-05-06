/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntaticAnalyzer.SourcePosition;

public class Identifier extends Terminal {

  public TypeKind returnType;
  public Type type;
  public Declaration decl;
  public Identifier (String s, SourcePosition posn) {
    super (s,posn);
  }

  public <A,R> R visit(Visitor<A,R> v, A o) {
      return v.visitIdentifier(this, o);
  }

}
