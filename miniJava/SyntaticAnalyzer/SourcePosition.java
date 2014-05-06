package miniJava.SyntaticAnalyzer;

/**
 * Class to keep track of the token position in the source file
 * 
 * @author hxing
 * 
 */
public class SourcePosition {

	public int startPosition, finishPosition;

	/**
	 * Constructor
	 */
	public SourcePosition() {
		startPosition = 0;
		finishPosition = 0;
	}

	public SourcePosition(int s, int f) {
		startPosition = s;
		finishPosition = f;
	}

	public String toString() {
		return "(" + startPosition + ", " + finishPosition + ")";
	}
}
