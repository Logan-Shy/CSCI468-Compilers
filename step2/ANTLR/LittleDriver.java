/*
    Author: Logan Shy, Chris Erickson, James Jacobs
    References and snippets taken from:
    https://stackoverflow.com/questions/7283478/scanner-lexing-keywords-with-antlr

*/
import java.io.IOException;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.antlr.v4.runtime.*;
import static org.antlr.v4.runtime.CharStreams.fromFileName;

// Steps to run Lexical Analyzer:
// 1. 'antlr4 LittleLexer.g4' (this will generate LittleLexer.java)
// 2. 'javac LittleLexer*.java' (this compiles generated file with driver)
// 3. 'java LittleLexerDriver ./path/to/input/file name_of_output.out' (this runs the driver)

public class LittleDriver {
    public static void main(String[] args) throws Exception {
        ////A relative file path to a .micro file should be passed
        ////as a command line argument when running this driver file
        ////Example: 'java LittleLexerDriver ../inputs/fibonacci.micro
        String filePath = args[0];

        CharStream fileStream = fromFileName(filePath);

        //Construct Lexer
        LittleLexer lexer = new LittleLexer(fileStream);

        //Construct token stream from lexer
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);

        //Construct parser and feed in lexer stream
        LittleParser parser = new LittleParser(tokenStream);

        parser.removeErrorListeners();

        parser.program();


        int totalErrors = parser.getNumberOfSyntaxErrors();
        if(totalErrors == 0){
            System.out.println("Accepted\n");
        } else {
            System.out.println("Not Accepted\n");
        }
    }
}