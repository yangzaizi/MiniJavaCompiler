MiniJavaCompiler
================

A compiler for MiniJava


This is a compiler for MiniJava, a subset of Java language. 

To run the program, use the following steps:

1) javac miniJava/Compiler.java
2) java miniJava/Compiler program1.java (where program1.java is a miniJava program)

By end of step 2, if the program1.java file have any syntatical or contexual analysis erros, the program would output
these errors. The contexual analysis error starts with "***".

If there is no syntatical or contexual analysis error, step 2) would produce an program1.mJAM and program1.asm file.
The program1.mJAM is the object code which will be interpreted. Please follow the next few steps

3) javac mJAM/Interpreter.java
4) java mJAM/Interpreter program1.mJAM

Step 4) would interpret the mJAM object code. Any result of println would starts with ">>>".

Some sample valid MiniJava program is provided in the Testcase directory

Note:
These are the limitation of MiniJava compiler: 

1)	No constructer is supported
2)	No method overloading is supported
3)	No inheritance or interface is supported
4)	Null value is syntactically not allowed
5)	Static fields cannot be initialized
6)	For loops are not supported
7)	No user-defined class System
