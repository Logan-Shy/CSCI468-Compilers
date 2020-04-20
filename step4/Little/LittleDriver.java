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

class AST{
    AstNode head;
    
    public AST(AstNode head){
        this.head = head;
    }
}

class AstNode{
    AstNode Lchild;
    AstNode Rchild;

    public AstNode(AstNode Lchild, AstNode Rchild){
        this.Lchild = Lchild;
        this.Rchild = Rchild;
    }

    public AstNode[] GetChildren(){
        AstNode[] nodeArray = {this.Lchild, this.Rchild};
        return nodeArray;
    }

    public AstNode GetLeftChild(){
        return this.Lchild;
    }

    public AstNode GetRightChild(){
        return this.Rchild;
    }

    public void SetLeftChild(AstNode newChild){
        this.Lchild = newChild;
    }

    public void SetRightChild(AstNode newChild){
        this.Rchild = newChild;
    }
}

class MyLittleListener extends LittleBaseListener{


    ///////////////These functions watch for variable declaration and add to its current symbol table
    @Override
    public void exitVar_decl(LittleParser.Var_declContext ctx){
        String type = ctx.var_type().getStart().getText();
        LinkedHashMap<String, String> scope = this.scopeStack.peek();//get current scope
        LittleParser.Id_tailContext commaCtx = ctx.id_list().id_tail();
        //put first var id into table
        if(!(scope.containsKey(ctx.id_list().getStart().getText()))){
            scope.put(ctx.id_list().getStart().getText(), type);
            while(commaCtx.getStart().getText().contains(",")){
                //get next variable id's if any exist and not already declared
                if(!(scope.containsKey(commaCtx.id().getStart().getText()))){
                    scope.put(commaCtx.id().getStart().getText(), type);
                    commaCtx = commaCtx.id_tail();
                } else{//duplicate declaration found
                    if(this.errorVar.isEmpty()){
                        this.errorVar = commaCtx.id().getStart().getText();
                    }
                }
            }
        } else{
            if(this.errorVar.isEmpty()){
                this.errorVar = ctx.id_list().getStart().getText();
            }
        }
    }

    @Override
    public void exitString_decl(LittleParser.String_declContext ctx){
        LinkedHashMap<String,String> scope = this.scopeStack.peek();//get current scope
        //add string identifier/value pair to current scope table
        if(!(scope.containsKey(ctx.id().getStart().getText()))){
            scope.put(ctx.id().getStart().getText(), "STRING value " + ctx.str().getStart().getText());
        } else {
            if(this.errorVar.isEmpty()){
                this.errorVar = ctx.id().getStart().getText();
            }
        }
    }


    @Override
    public void exitParam_decl(LittleParser.Param_declContext ctx){
        LinkedHashMap<String,String> scope = this.scopeStack.peek();//get current scope
        //add function integer name/type pair to current scope table
        if(!(scope.containsKey(ctx.id().getStart().getText()))){
            scope.put(ctx.id().getStart().getText(), ctx.var_type().getStart().getText());
        } else {
            if(this.errorVar.isEmpty()){
                this.errorVar = ctx.id().getStart().getText();
            }
        }
    }
}