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

        Stack<HashMap<String,String>> s = listener.getSymbolTable();
 
        System.out.println("Final symbol table: \n" + s);
    }
}

class MyLittleListener extends LittleBaseListener{
    // Init new symbole table
    int blockIter = 0;
    Stack<HashMap<String,String>> scopeStack = new Stack<HashMap<String,String>>();

    public Stack<HashMap<String,String>> getSymbolTable(){
        return this.scopeStack;
    }


    @Override
    public void enterProgram(LittleParser.ProgramContext ctx){
        HashMap<String, String> GLOBAL = new HashMap<String, String>();
        GLOBAL.put("scope", "GLOBAL");//create and add global scope to scopestack
        this.scopeStack.push(GLOBAL);
    }

    @Override
    public void exitVar_decl(LittleParser.Var_declContext ctx){
        String type = ctx.var_type().getStart().getText();
        HashMap<String, String> scope = this.scopeStack.peek();//get current scope
        LittleParser.Id_tailContext commaCtx = ctx.id_list().id_tail();
        //put first var id into table
        scope.put(ctx.id_list().getStart().getText(), type);
        while(commaCtx.getStart().getText().contains(",")){
            //get next variable id's if any exist
            scope.put(commaCtx.id().getStart().getText(), type);
            commaCtx = commaCtx.id_tail();
        }
    }

    @Override
    public void exitString_decl(LittleParser.String_declContext ctx){
        HashMap<String,String> scope = this.scopeStack.peek();//get current scope
        //add string identifier/value pair to current scope table
        scope.put(ctx.id().getStart().getText(), ctx.str().getStart().getText());
    }

    @Override
    public void enterFunc_decl(LittleParser.Func_declContext ctx){
        //todo
    }
}