/*
    Author: Logan Shy, Chris Erickson, James Jacobs

*/
import java.io.IOException;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.antlr.v4.runtime.*;
import static org.antlr.v4.runtime.CharStreams.fromFileName;

// Steps to run Lexical Analyzer:
// 1. 'antlr4 Little.g4' (this will generate Little.java)
// 2. 'javac Little*.java' (this compiles generated files with driver)
// 3. 'java LittleDriver ./path/to/input/file' (this runs the driver)

public class LittleDriver {
    public static void main(String[] args) throws Exception {
        ////A relative file path to a .micro file should be passed
        ////Example: 'java LittleDriver ../inputs/test1.micro
        String filePath = args[0];

        //Construct Char stream from file name given via command arguments
        CharStream fileStream = fromFileName(filePath);

        //Construct Lexer
        LittleLexer lexer = new LittleLexer(fileStream);

        //Construct token stream from lexer
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);

        //Construct parser and feed in lexer stream
        LittleParser parser = new LittleParser(tokenStream);

        //Remove error listener
        parser.removeErrorListeners();

        //'program' is the start production, so calling it invokes the parsing
        parser.program();


        //gather total errors when finished parsing
        //if none exist, print accepted, else print not accepted
        int totalErrors = parser.getNumberOfSyntaxErrors();
        if(totalErrors == 0){
            System.out.println("Accepted\n");
        } else {
            System.out.println("Not accepted\n");
        }
    }
}