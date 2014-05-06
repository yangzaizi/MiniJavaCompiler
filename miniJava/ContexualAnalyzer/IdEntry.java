package miniJava.ContexualAnalyzer;

import miniJava.AbstractSyntaxTrees.Declaration;

public class IdEntry {

	protected String id;
	protected Declaration attr;
	protected int level;
	protected IdEntry previous;
	protected boolean definedByUser;

	IdEntry(String id, Declaration attr, int level, IdEntry previous, boolean userDefined) {
		this.id = id;
		this.attr = attr;
		this.level = level;
		this.previous = previous;
		definedByUser=userDefined;
	}

}
