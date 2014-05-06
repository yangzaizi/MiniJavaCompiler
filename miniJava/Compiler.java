package miniJava;

import mJAM.Disassembler;
import mJAM.Interpreter;
import mJAM.ObjectFile;
import miniJava.AbstractSyntaxTrees.AST;
import miniJava.AbstractSyntaxTrees.ASTDisplay;
import miniJava.CodeGenerator.CodeGenerator;
import miniJava.ContexualAnalyzer.Checker;
import miniJava.SyntaticAnalyzer.ErrorReporter;
import miniJava.SyntaticAnalyzer.Parser;
import miniJava.SyntaticAnalyzer.Scanner;
import miniJava.SyntaticAnalyzer.SourceFile;
import miniJava.SyntaticAnalyzer.SyntaxError;

public class Compiler {

	public static void main(String[] args) throws SyntaxError {

		SourceFile source = new SourceFile(args[0]);

		ErrorReporter reporter = new ErrorReporter();
		Scanner scan = new Scanner(source);
		Parser parser = new Parser(scan, reporter);

		AST tree = parser.parseProgram();

		if (reporter.hasErrors()) {
			System.out.println("Parse Error");
			System.exit(4);
		} else {

			Checker checker = new Checker();
			checker.check(tree);
			if (Checker.hasError == false) {

				CodeGenerator codeGenerator = new CodeGenerator();
				codeGenerator.generateCode(tree);

				String objectCodeFileName = new String(args[0].substring(0,
						args[0].lastIndexOf('.') + 1) + "mJAM");
				ObjectFile objF = new ObjectFile(objectCodeFileName);
				System.out.println("Generating the code file "
						+ objectCodeFileName + " ..:)");
				if (objF.write()) {
					System.out.println("FAILED to generate the code file!");
					return;
				} else {
					System.out
							.println("SUCCEEDED in generating the code file!");
				}
				String asmFileName = new String(args[0].substring(0,
						args[0].lastIndexOf('.') + 1)
						+ "asm");
				Disassembler d = new Disassembler(objectCodeFileName);
				if (d.disassemble()) {
					System.out.println("FAILED to generate the assembly file!");
					return;
				} else {
					System.out
							.println("SUCCEEDED in generating the assembly file!");
				}

				System.exit(0);
			}

			else {

				System.exit(4);
			}

		}

		/*
		 * Token k=scan.scan(); while(k.kind!=Token.EOT){
		 * System.out.print(k.spelling+ ":"+k.kind); k=scan.scan(); }
		 */

	}

}
