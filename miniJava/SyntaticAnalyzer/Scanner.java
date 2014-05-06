package miniJava.SyntaticAnalyzer;

/**
 * The scanner class for scanning the source file; a stream of token is produced
 * 
 * @author hxing
 * 
 */
public class Scanner {

	private SourceFile sourceFile;
	private boolean debug;

	private char currentChar;
	private StringBuffer spelling;
	private boolean continueScanning;
	private Token previousToken;
	private Token currentToken;
	private int tokenRead;

	private boolean isLetter(char character) {
		return (character >= 'a' && character <= 'z')
				|| (character >= 'A' && character <= 'Z');
	}

	private boolean isDigit(char character) {
		return (character >= '0' && character <= '9');
	}

	public Scanner(SourceFile source) {
		sourceFile = source;
		currentChar = sourceFile.readCharacter();
		debug = false;
		previousToken = currentToken = null;
		tokenRead = 0;

	}

	public void enableDebugging() {
		debug = true;
	}

	/**
	 * takeIt appends the current character to the current token, and gets the
	 * next character from the source program.
	 */

	private void takeIt() {
		if (continueScanning)
			spelling.append(currentChar);
		currentChar = sourceFile.readCharacter();
	}

	/**
	 * Skip the spaces
	 */

	private void skipSpaces() {
		while (currentChar == ' ' || currentChar == '\n' || currentChar == '\r'
				|| currentChar == '\t')
			takeIt();

	}

	/**
	 * Skip the comments
	 */
	private int skipComments() {
		int result = 0;
		if (currentChar == '/') {
			takeIt();
			if (currentChar == '/') {
				takeIt();
				while ((currentChar != SourceFile.EOL)
						&& (currentChar != SourceFile.EOT))
					takeIt();
				if (currentChar == SourceFile.EOL) {
					takeIt();
					skipSpaces();
				}
				result = skipComments();

			} else if (currentChar == '*') {
				takeIt();

				result = looping();

			} else {
				spelling = new StringBuffer("");
				spelling.append('/');
			}

		}
		return result;
	}

	private int looping() {
		char previousChar = Character.MIN_VALUE;

		while ((previousChar != '*' || currentChar != '/')
				&& currentChar != SourceFile.EOT) {

			previousChar = currentChar;

			takeIt();

		}

		if (currentChar == '/' && previousChar == '*') {

			takeIt();
			skipSpaces();
			skipComments();
			skipSpaces();
			return 0;
		} else {
			return Token.ERROR;
		}
	}

	/**
	 * Scan and return the kind of the token being read
	 * 
	 * @return
	 */
	private int scanToken() {

		switch (currentChar) {

		case 'a':
		case 'b':
		case 'c':
		case 'd':
		case 'e':
		case 'f':
		case 'g':
		case 'h':
		case 'i':
		case 'j':
		case 'k':
		case 'l':
		case 'm':
		case 'n':
		case 'o':
		case 'p':
		case 'q':
		case 'r':
		case 's':
		case 't':
		case 'u':
		case 'v':
		case 'w':
		case 'x':
		case 'y':
		case 'z':
		case 'A':
		case 'B':
		case 'C':
		case 'D':
		case 'E':
		case 'F':
		case 'G':
		case 'H':
		case 'I':
		case 'J':
		case 'K':
		case 'L':
		case 'M':
		case 'N':
		case 'O':
		case 'P':
		case 'Q':
		case 'R':
		case 'S':
		case 'T':
		case 'U':
		case 'V':
		case 'W':
		case 'X':
		case 'Y':
		case 'Z':
			takeIt();
			while (isLetter(currentChar) || isDigit(currentChar)
					|| currentChar == '_')
				takeIt();
			return Token.IDENTIFIER;

		case '0':
		case '1':
		case '2':
		case '3':
		case '4':
		case '5':
		case '6':
		case '7':
		case '8':
		case '9':
			takeIt();
			while (isDigit(currentChar))
				takeIt();
			return Token.NUM;

		case '|':
			takeIt();
			if (currentChar == '|') {
				takeIt();
				return Token.OR;
			} else {
				return Token.ERROR;
			}

		case '/':
			takeIt();
			return Token.DEVIDE;
		case '*':
			takeIt();
			return Token.TIMES;
		case '&':
			takeIt();
			if (currentChar == '&') {
				takeIt();
				return Token.AND;
			} else {
				return Token.ERROR;
			}
		case '+':
			takeIt();
			return Token.PLUS;

		case '<':
			takeIt();
			boolean equal = false;
			if (currentChar == '=') {
				equal = true;
				takeIt();
			}
			return (equal == true) ? Token.LESSTHANEQUAL : Token.LESSTHAN;
		case '>':
			takeIt();
			boolean equals = false;
			if (currentChar == '=') {
				equals = true;
				takeIt();
			}
			return (equals == true) ? Token.GREATEREQUAL : Token.GREATER;

		case '-':
			takeIt();
			if (previousToken.kind == Token.IDENTIFIER
					|| previousToken.kind == Token.NUM
					|| previousToken.kind == Token.RPAREN) {
				return Token.MINUS;
			} else {
				if(currentChar=='-'){
					takeIt();
					return Token.ERROR;
				}
				else
					return Token.NEGATE;
			}

		case '.':
			takeIt();
			return Token.DOT;

		case '!':
			takeIt();
			boolean isUnop = true;
			if (currentChar == '=') {
				takeIt();
				isUnop = false;
			}
			return (isUnop == true) ? Token.NOT : Token.NOTEQUAL;

		case '=':
			takeIt();
			boolean bin = false;
			if (currentChar == '=') {
				takeIt();
				bin = true;
			}
			return (bin == true) ? Token.ISEQUAL : Token.EQUAL;

		case ';':
			takeIt();
			return Token.SEMICOLON;

		case ',':
			takeIt();
			return Token.COMMA;

		case '(':
			takeIt();
			return Token.LPAREN;

		case ')':
			takeIt();
			return Token.RPAREN;

		case '[':
			takeIt();
			return Token.LBRACKET;

		case ']':
			takeIt();
			return Token.RBRACKET;

		case '{':
			takeIt();
			return Token.LCURLY;

		case '}':
			takeIt();
			return Token.RCURLY;

		case SourceFile.EOT:
			return Token.EOT;

		default:
			takeIt();
			return Token.ERROR;
		}
	}

	/**
	 * Scan and return a single token
	 * 
	 * @return: the token being read
	 */
	public Token scan() {
		if (tokenRead > 0) {
			previousToken = currentToken;
		}
		SourcePosition pos;
		int kind;
		spelling = new StringBuffer("");
		continueScanning = false;

		skipSpaces();
		int error = skipComments();
		continueScanning = true;

		if (error != 0) {
			pos = new SourcePosition();
			pos.startPosition = sourceFile.getCurrentLine();

			pos.finishPosition = sourceFile.getCurrentLine();
			return new Token(Token.ERROR, "<error>", pos);
		}
		if (spelling.length() == 0) {
			pos = new SourcePosition();
			pos.startPosition = sourceFile.getCurrentLine();

			kind = scanToken();

			pos.finishPosition = sourceFile.getCurrentLine();
			currentToken = new Token(kind, spelling.toString(), pos);

			if (debug)
				System.out.println(currentToken);
			tokenRead++;
			return currentToken;
		} else {
			pos = new SourcePosition();
			pos.startPosition = sourceFile.getCurrentLine() - 1;
			pos.finishPosition = sourceFile.getCurrentLine();
			return new Token(Token.DEVIDE, spelling.toString(), pos);
		}

	}
}
