package miniJava.SyntaticAnalyzer;

/**
 * The Token class for the scanner. Each token has a kind, a spelling, and a
 * source position in the source file
 * 
 * @author hxing
 * 
 */
public class Token extends Object {

	public int kind;
	public String spelling;
	protected SourcePosition position;

	/**
	 * Constructor for the Token class
	 * 
	 * @param kind
	 *            : the token kind
	 * @param spelling
	 *            : the spelling of the token
	 * @param position
	 *            : the position of the token in the sourcefile
	 */
	public Token(int kind, String spelling, SourcePosition position) {

		if (kind == Token.IDENTIFIER) {
			int currentKind = firstReservedWord;
			boolean searching = true;

			while (searching) {
				int comparison = tokenTable[currentKind].compareTo(spelling);
				if (comparison == 0) {
					this.kind = currentKind;
					searching = false;
				} else if (comparison > 0 || currentKind == lastReservedWord) {
					this.kind = Token.IDENTIFIER;
					searching = false;
				} else {
					currentKind++;
				}
			}
		} else
			this.kind = kind;

		this.spelling = spelling;
		this.position = position;

	}

	public static String spell(int kind) {
		return tokenTable[kind];
	}

	public String toString() {
		return "Kind=" + kind + ", spelling=" + spelling + ", position="
				+ position;
	}

	// Token kinds

	public static final int

	// literals, identifiers, operators...
			NUM = 0,
			IDENTIFIER = 1,
			

			// reserved words - must be in alphabetical order...
			BOOLEAN = 2, CLASS = 3, ELSE = 4, FALSE = 5, IF = 6, INT = 7,

			NEW = 8,

			PRIVATE = 9, PUBLIC = 10,

			RETURN = 11,
			STATIC = 12,
			THIS = 13,
			TRUE = 14,
			VOID = 15,
			WHILE = 16,

			// punctuation...
			DOT = 17,
			SEMICOLON = 18,
			COMMA = 19,
			EQUAL = 20,

			// brackets...
			LPAREN = 21, RPAREN = 22, LBRACKET = 23,
			RBRACKET = 24,
			LCURLY = 25, RCURLY = 26, OR = 27, AND = 28,
			ISEQUAL = 29,
			NOTEQUAL = 30, LESSTHANEQUAL = 31,
			LESSTHAN = 32,
			GREATEREQUAL = 33, GREATER = 34, PLUS = 35, MINUS = 36,
			TIMES = 37,
			DEVIDE = 38, NEGATE = 39, NOT = 40,

			// special tokens...
			EOT = 41, ERROR = 42;

	private static String[] tokenTable = new String[] { "num", "identifier",
			"boolean", "class", "else", "false",
			"if", "int", "new", "private", "public", "return", "static",
			"this", "true", "void", "while", ".", ";", ",", "=", "(", ")", "[",
			"]", "{", "}", "||", "&&", "==", "!=", "<=", "<", ">=", ">", "+",
			"-", "*", "/", "-", "!", "", "<error>" };

	private final static int firstReservedWord = Token.BOOLEAN,
			lastReservedWord = Token.WHILE;
}
