/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntaticAnalyzer.SourcePosition;

public abstract class Literal extends Terminal {
	public Type type;
	public Literal(String spelling, SourcePosition posn) {
		super(spelling, posn);
	}
}
