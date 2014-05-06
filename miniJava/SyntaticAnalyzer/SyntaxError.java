package miniJava.SyntaticAnalyzer;

/**
 * SyntaxError class
 * 
 * @author hxing
 * 
 */
public class SyntaxError extends Exception {

	/**
	 * SyntaxError Constructor
	 */
	private static final long serialVersionUID = 6761629251508016007L;

	public SyntaxError() {
		super();
	}

	public String error() {
		return super.toString();
	}

}
