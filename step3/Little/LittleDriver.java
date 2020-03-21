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

        LinkedHashMap<String, LinkedHashMap<String, String>> s = listener.getSymbolTable();
 
        System.out.println("Final symbol table: \n" + s);
    }
}

class MyLittleListener extends LittleBaseListener{
    // Init new symbole table
    int blockIter = 1;
    Stack<LinkedHashMap<String,String>> scopeStack = new Stack<LinkedHashMap<String,String>>();
    LinkedHashMap<String, LinkedHashMap<String,String>> finalTable = new LinkedHashMap<String, LinkedHashMap<String,String>>();

    public LinkedHashMap<String, LinkedHashMap<String, String>> getSymbolTable(){
        return this.finalTable;
    }

    ///////// These functions create, push and pop symbol tables as new scope is entered
    @Override
    public void enterProgram(LittleParser.ProgramContext ctx){
        LinkedHashMap<String, String> GLOBAL = new LinkedHashMap<String, String>();
        //create and add global scope to scopestack
        this.scopeStack.push(GLOBAL);
    }

    @Override
    public void exitProgram(LittleParser.ProgramContext ctx){
        //pop global symbol table from scope stack and add to final table 
        this.finalTable.put("GLOBAL", this.scopeStack.pop());
    }


    @Override
    public void enterFunc_decl(LittleParser.Func_declContext ctx){
        LinkedHashMap<String, String> funcScope = new LinkedHashMap<String, String>();
        // funcScope.put("scope", ctx.id().getStart().getText());//create and add function scope to scopestack
        this.scopeStack.push(funcScope);
    }

    @Override
    public void exitFunc_decl(LittleParser.Func_declContext ctx){
        //remove now complete function table from scopestack and add to final table
        this.finalTable.put(ctx.id().getStart().getText(), this.scopeStack.pop());
    }


    @Override
    public void enterWhile_stmt(LittleParser.While_stmtContext ctx){
        LinkedHashMap<String, String> whileScope = new LinkedHashMap<String, String>();
        //create and add while loop scope to scopestack
        this.scopeStack.push(whileScope);
    }

    @Override
    public void exitWhile_stmt(LittleParser.While_stmtContext ctx){
        this.finalTable.put("BLOCK " + this.blockIter++, this.scopeStack.pop());
    }


    @Override
    public void enterIf_stmt(LittleParser.If_stmtContext ctx){
        LinkedHashMap<String, String> thenScope = new LinkedHashMap<String, String>();
        //create and add then block scope to scopestack
        this.scopeStack.push(thenScope);
    }

    public void exitIf_stmt(LittleParser.If_stmtContext ctx){
        if(!(ctx.else_part().getChildCount() > 0)){//pop then block scope since if_else did not
            this.finalTable.put("BLOCK " + this.blockIter++, this.scopeStack.pop());
        }
    }


    public void enterElse_part(LittleParser.Else_partContext ctx){
        if(ctx.getChildCount() > 0){
            //Pop previous then scope block
            this.finalTable.put("BLOCK " + this.blockIter++, this.scopeStack.pop());
            LinkedHashMap<String, String> elseScope = new LinkedHashMap<String, String>();
            //create and add else block scope to scopestack
            this.scopeStack.push(elseScope);
        }
    }

    public void exitElse_part(LittleParser.Else_partContext ctx){
        if(ctx.getChildCount() > 0){
            this.finalTable.put("BLOCK " + this.blockIter++, this.scopeStack.pop());
        }
    }




    //TO-DO: add error handling for variable declaration duplication
    ///////////////These functions watch for variable declaration and add to its current symbol table
    @Override
    public void exitVar_decl(LittleParser.Var_declContext ctx){
        String type = ctx.var_type().getStart().getText();
        LinkedHashMap<String, String> scope = this.scopeStack.peek();//get current scope
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
        LinkedHashMap<String,String> scope = this.scopeStack.peek();//get current scope
        //add string identifier/value pair to current scope table
        scope.put(ctx.id().getStart().getText(), ctx.str().getStart().getText());
    }


    @Override
    public void exitParam_decl(LittleParser.Param_declContext ctx){
        LinkedHashMap<String,String> scope = this.scopeStack.peek();//get current scope
        //add function integer name/type pair to current scope table
        scope.put(ctx.id().getStart().getText(), ctx.var_type().getStart().getText());
    }
}