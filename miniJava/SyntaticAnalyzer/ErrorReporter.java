package miniJava.SyntaticAnalyzer;

public class ErrorReporter {

	private int numErrors;

	public ErrorReporter() {
		numErrors = 0;
	}

	public boolean hasErrors() {
		return numErrors > 0;
	}

	public void reportError(String message) {
		System.out.println(message);
		numErrors++;
	}	
}
