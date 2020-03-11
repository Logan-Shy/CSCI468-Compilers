/*
    Author: Logan Shy, Chris Erickson, James Jacobs

*/
import java.io.IOException;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import javax.sound.sampled.SourceDataLine;

import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.*;
import static org.antlr.v4.runtime.CharStreams.fromFileName;


// Steps to run parser walker:
// 1. exectue micro.sh script

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
        
        MyLittleListener listener = new MyLittleListener();

        new ParseTreeWalker().walk(listener, parser.program());
 
    }
}

class MyLittleListener extends LittleBaseListener{
    // Init new symbole table
    int blockIter = 0;
    Stack<HashMap> scopeStack = new Stack<HashMap>();


    @Override
    public void enterProgram(LittleParser.ProgramContext ctx){
        System.out.println("Just entered Program!");
        HashMap<String, String> GLOBAL = new HashMap<String, String>();
        GLOBAL.put("scope", "GLOBAL");
        this.scopeStack.push(GLOBAL);
    }

    @Override
    public void exitVar_decl(LittleParser.Var_declContext ctx){
        System.out.println("Rule Text: " + ctx.getText());
    }
}