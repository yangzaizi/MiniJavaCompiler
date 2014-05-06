package miniJava.SyntaticAnalyzer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Class for reading the source file.
 * 
 * @author hxing
 * 
 */
public class SourceFile {

	public static final char EOL = '\n';
	public static final char EOT = '\u0000';

	File sourceCode;
	FileInputStream input;
	int currentLine;

	/**
	 * Constructor
	 * 
	 * @param filename
	 *            : the file name of the source file; either direct path or
	 *            relative path
	 */
	public SourceFile(String filename) {
		try {
			sourceCode = new File(filename);
			input = new FileInputStream(sourceCode);

			currentLine = 1;
		} catch (IOException e) {
			sourceCode = null;
			input = null;
			currentLine = 0;
		}
	}

	/**
	 * Read a character from the source file
	 * 
	 * @return: the character read
	 */
	public char readCharacter() {
		try {
			int character = input.read();

			if (character == -1) {
				character = EOT;
			} else if (character == EOL) {
				currentLine++;
			}
			return (char) character;

		} catch (IOException e) {
			return EOT;
		}
	}

	public int getCurrentLine() {
		return currentLine;
	}
}
