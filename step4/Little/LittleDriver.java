/*
    Author: Logan Shy, Chris Erickson, James Jacobs

*/
import java.io.IOException;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import javax.management.RuntimeErrorException;

import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.*;
import static org.antlr.v4.runtime.CharStreams.fromFileName;


// Steps to run parser walker:
// 1. exectue micro.sh script

public class LittleDriver {

    public static void prettyPrint(LinkedHashMap<String, LinkedHashMap<String, String>> finalTable){
        if(!(finalTable.containsKey("DECLARATION ERROR"))){
            //print off individual linkedhashmaps
            finalTable.forEach((k,v) -> {
                if(k != "GLOBAL"){
                    System.out.println();
                }
                System.out.println("Symbol table " + k);
                v.forEach((var, type) -> {
                    System.out.println("name " + var + " type " + type);
                });
            });
        } else {//print off var that errored
            String errorVar = finalTable.get("DECLARATION ERROR").get("error");
            System.out.println("DECLARATION ERROR " + errorVar);
        }
    }

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
        
        //Initialize inheriting listener
        MyLittleListener listener = new MyLittleListener();

        //Walk the generated parse tree
        new ParseTreeWalker().walk(listener, parser.program());
        
        //Get the final symbol table from listener and print
        // LinkedHashMap<String, LinkedHashMap<String, String>> s = listener.getSymbolTable();
        // prettyPrint(s);

        System.out.println("\nAST Nodes:");
        while(!listener.nodePool.empty()){
            AstNode.Print(listener.nodePool.pop());
            System.out.println("-----------------");
        }
        System.out.println("\nFinal Trees: ");
        LinkedList<AstNode> printList = new LinkedList<AstNode>();
        printList = (LinkedList<AstNode>) listener.treeList.clone();
        while(!printList.isEmpty()){
            AstNode.Print(printList.removeFirst());
            System.out.println("-----------------");
        }

        //Generate code
        System.out.println("\nGenerating Code from trees:");
        Stack<CodeObject> codeStack = new Stack<CodeObject>();
        while(!listener.treeList.isEmpty()){
            AstNode.GenerateCode(listener.treeList.removeFirst(), codeStack);
            while(!codeStack.empty()){//print code object for given AST
                CodeObject.Print(codeStack.pop());
                System.out.println("______________________");
            }
        }

    }
}

class CodeObject{
    String temp;
    String type;
    String code;

    public CodeObject(String temp, String type, String code){
        this.temp = temp;
        this.type = type;
        this.code = code;
    }

    public static void Print(CodeObject o){
        System.out.println("Temp name: "+ o.temp);
        System.out.println("Type:"+ o.type);
        System.out.println("Generated Code:\n" + o.code);
    }
}

class AstNode{

    String type;
    String[] data = new String[2];

    AstNode Lchild;
    AstNode Rchild;

    public AstNode(AstNode Lchild, AstNode Rchild, String type, String[] data){
        this.Lchild = Lchild;
        this.Rchild = Rchild;
        this.type = type;
        this.data = data;
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

    public static void Print(AstNode head){
        
        if(head != null){
            if(head.type != null){
                System.out.println("Type: " + head.type);
            }
            if(head.data != null){
                System.out.println("Data: " + "[" + head.data[0] + ", " + head.data[1] + "]");
            }
            System.out.println();
            if(head.Lchild != null){
                System.out.println("Left...");
                Print(head.Lchild);
            }
            if(head.Rchild != null){
                System.out.println("Right...");
                Print(head.Rchild);
            }
        }
    }

    public static void GenerateCode(AstNode head, Stack<CodeObject> generatedCode){
        if(head != null){
            if(head.Lchild != null){
                GenerateCode(head.Lchild, generatedCode);
            }
            if(head.Rchild != null){
                GenerateCode(head.Rchild, generatedCode);
            }


            if(head.type.contains("AssignExpr")){
                //generate new temp for name
                String tempName = CreateTemporary();
                String code = "";
                String typeName = "";

                //pull up rhs and lhs code objects
                CodeObject assignee = generatedCode.pop();
                CodeObject assigner = generatedCode.pop();

                if(assignee.code != null){
                    code += assignee.code;
                }
                if(assignee.type.contains("INT")){
                    code += "STOREI" + " " + assignee.temp + " " + assigner.temp + "\n";
                    typeName = "INT";
                } else if(assignee.type.contains("FLOAT")){
                    code += "STOREF" + " " + assignee.temp + " " + assigner.temp + "\n";
                    typeName = "FLOAT";
                }
                CodeObject assignObject = new CodeObject(tempName, typeName, code);
                generatedCode.push(assignObject);
            } else if(head.type.contains("Literal")){//float or int
                if(head.type.contains("Int")){
                    //create new code object with only two fields filled in
                    //Literal
                    CodeObject newObject = new CodeObject(head.data[0], "INTLIT", null);
                    generatedCode.push(newObject);
                } else if(head.type.contains("Float")){
                    //Float
                    CodeObject newObject = new CodeObject(head.data[0], "FLOATLIT", null);
                    generatedCode.push(newObject);
                }
            } else if(head.type.contains("VarRef")){//Var Reference
                CodeObject newObject = new CodeObject(head.data[0], head.data[1], null);
                generatedCode.push(newObject);
            } else if(head.type.contains("AddExpr")){//Add Expression
                //Generate new temp for name
                String tempName = CreateTemporary();
                String typeName = "";
                String code = "";
                String leftTemp = "[LEFT TEMP NAME]";
                String rightTemp = "[RIGHT TEMP NAME]";

                //Guaranteed two nodes from left and right child to be added
                CodeObject rightObject = generatedCode.pop();
                CodeObject leftObject = generatedCode.pop();

                //Left child code generation
                if(!leftObject.temp.contains("$T") && !leftObject.type.contains("LIT")){//var reference
                    leftTemp = leftObject.temp;
                    code += "LD " + leftObject.temp + " " + CreateTemporary() + "\n";
                } else if(leftObject.type.contains("LIT")){//Int Literal
                    leftTemp = leftObject.temp;
                } else if(leftObject.temp.contains("$T")){//expression
                    //load generated code from expression and add to new code
                    leftTemp = leftObject.temp;
                    code += leftObject.code;
                }

                //Right Child code generation
                if(!rightObject.temp.contains("$T") && !rightObject.type.contains("LIT")){//var reference
                    rightTemp = rightObject.temp;
                    code += "LD " + rightObject.temp + " " + CreateTemporary() + "\n";
                } else if(rightObject.type.contains("LIT")){//Int Literal
                    rightTemp = rightObject.temp;
                } else if(rightObject.temp.contains("$T")){//expression
                    //load generated code from expression and add to new code
                    rightTemp = rightObject.temp;
                    code += rightObject.code;
                }


                //Finalize Add expr Code generation
                if(head.type.contains("+")){//use Add instruction variant
                    if(leftObject.type.contains("INT")){//Use AddI instruction
                        code += "ADDI "+ leftTemp +" "+ rightTemp +" "+ tempName +"\n";
                        typeName = "INT";
                    } else if(leftObject.type.contains("FLOAT")){//Use AddF instruction
                        code += "ADDF "+ leftTemp +" "+ rightTemp +" "+ tempName +"\n";
                        typeName = "FLOAT";
                    }
                } else if(head.type.contains("-")){//Use Sub instruction variant
                    if(leftObject.type.contains("INT")){//Use SubI instruction
                        code += "SUBI "+ leftTemp +" "+ rightTemp +" "+ tempName +"\n";
                        typeName = "INT";
                    } else if(leftObject.type.contains("FLOAT")){//Use SubF instruction
                        code += "SUBF "+ leftTemp +" "+ rightTemp +" "+ tempName +"\n";
                        typeName = "FLOAT";
                    }
                }
                
                //Create new Code Object and push up
                // System.out.println("CREATED CODE:\n" + code);
                CodeObject AddExpr = new CodeObject(tempName, typeName, code);
                generatedCode.push(AddExpr);
            } else if (head.type.contains("VarDecl")){//variable declaration
                String tempName = head.data[0];
                String typeName = head.data[1];
                String code = "var "+tempName;
                CodeObject varObject = new CodeObject(tempName, typeName, code);
                generatedCode.push(varObject);
            } else if(head.type.contains("StringDecl")){//String declaration
                String strName = head.data[0];
                String strVal = head.data[1];
                String code = "str " + strName + " " + strVal;
                CodeObject strObject = new CodeObject(strName, "STRING", code);
                generatedCode.push(strObject);
            } else if(head.type.contains("WriteExpr")){//write statement
                String tempName = "WRITE";
                String typeName = "";
                String code = "";
                if(head.data[1].contains("\"")){//write string
                    typeName = "STRING";
                    code = "sys writes " + head.data[0];
                    CodeObject strObject = new CodeObject(tempName, typeName, code);
                    generatedCode.push(strObject);
                } else{//float or integer
                    if(head.data[1].contains("INT")){//write int
                        typeName = head.data[1];
                        code = "sys writei " + head.data[0];
                    } else if(head.data[1].contains("FLOAT")){//write float
                        typeName = head.data[1];
                        code = "sys writer " + head.data[0];
                    } else{
                        System.out.println("SOMETHING WACK JUST HAPPENED");
                    }
                    CodeObject varObject = new CodeObject(tempName, typeName, code);
                    generatedCode.push(varObject);
                }
            }
        }
    }

    public static String CreateTemporary(){
        MyLittleListener.tempCounter++;
        String temporary = "$T" + MyLittleListener.tempCounter;
        return temporary;
    }
}


class MyLittleListener extends LittleBaseListener{
    
    public static int tempCounter = 0;
    Stack<AstNode> nodePool = new Stack<AstNode>();
    LinkedList<AstNode> treeList = new LinkedList<AstNode>();
    // Init new symbole table
    int blockIter = 1;
    String errorVar = "";
    Stack<LinkedHashMap<String,String>> scopeStack = new Stack<LinkedHashMap<String,String>>();
    LinkedHashMap<String, LinkedHashMap<String,String>> finalTable = new LinkedHashMap<String, LinkedHashMap<String,String>>();

    public LinkedHashMap<String, LinkedHashMap<String, String>> getSymbolTable(){
        if(this.errorVar.isEmpty()){//check if any declaration errors occurred
            return this.finalTable;
        } else {
            LinkedHashMap<String,String> error = new LinkedHashMap<String,String>();
            LinkedHashMap<String, LinkedHashMap<String,String>> errorTable = new LinkedHashMap<String, LinkedHashMap<String,String>>();
            error.put("error", this.errorVar);
            errorTable.put("DECLARATION ERROR", error);
            return errorTable;
        }
    }

    ///////// These functions handle AST generation \\\\\\\\\\\\\\\
    @Override
    public void exitWrite_stmt(LittleParser.Write_stmtContext ctx){
        LinkedHashMap<String,String> symbolTable = this.finalTable.get("GLOBAL");
        //Check if only one variable to write
        if(ctx.id_list().id_tail().getChildCount() <= 0){//only one
            String varId = ctx.id_list().id().getStart().getText();
            String varType = symbolTable.get(varId);
            AstNode writeop = new AstNode(null, null, "WriteExpr", new String[] {varId, varType});
            this.treeList.add(writeop);
        } else{//more than one var to write
            //Write id in id_list()
            String varId = ctx.id_list().id().getStart().getText();
            String varType = symbolTable.get(varId);
            AstNode writeop = new AstNode(null, null, "WriteExpr", new String[] {varId, varType});
            this.treeList.add(writeop);
            //Write subsequent ids in id_tails()
            LittleParser.Id_tailContext tailCtx = ctx.id_list().id_tail();
            while(true){
                varId = tailCtx.id().getStart().getText();
                varType = symbolTable.get(varId);
                AstNode nextWriteop = new AstNode(null, null, "WriteExpr", new String[] {varId, varType});
                this.treeList.add(nextWriteop);
                if(tailCtx.id_tail().getChildCount() > 0){//more to print
                    tailCtx = tailCtx.id_tail();
                } else{//No more to print, break out
                    break;                    
                }
            }

        }
    }
    
    @Override
    public void exitAddop(LittleParser.AddopContext ctx){
        //Add correct node depending on text of addop
        if (ctx.getStart().getText().contains("+")){
            AstNode addop = new AstNode(null, null, "AddExpr(+)", null);
            this.nodePool.push(addop);
        } else if(ctx.getStart().getText().contains("-")){
            AstNode addop = new AstNode(null, null, "AddExpr(-)", null);
            this.nodePool.push(addop);
        }
    }

    @Override 
    public void exitMulop(LittleParser.MulopContext ctx){
        //Add correct node depending on text of mulop
        if(ctx.getStart().getText().contains("*")){
            AstNode mulop = new AstNode(null, null, "MulExpr(*)", null);
            this.nodePool.push(mulop);
        } else if(ctx.getStart().getText().contains("/")){
            AstNode mulop = new AstNode(null, null, "MulExpr(/)", null);
            this.nodePool.push(mulop);
        }
    }

    @Override
    public void exitFactor(LittleParser.FactorContext ctx){
        LinkedHashMap<String,String> symbolTable = this.finalTable.get("GLOBAL");
        if(ctx.factor_prefix().getChildCount() <= 0){//no factor prefix, so just var/literal/expr usage.
            if (ctx.postfix_expr().primary().id() == null){//int literal or expr
                if(ctx.postfix_expr().primary().expr() == null){
                    //Literal
                    String literal = ctx.postfix_expr().primary().getStart().getText();
                    if(literal.contains(".")){
                        //float literal
                        AstNode lit = new AstNode(null, null, "FloatLiteral", new String[] {literal, "FLOAT"});
                        this.nodePool.push(lit);
                    } else{
                        //int literal
                        AstNode lit = new AstNode(null, null, "IntLiteral", new String[] {literal, "INT"});
                        this.nodePool.push(lit);
                    }
                } else{
                    //Expression, whose node should have been taken care of by expr().
                    //No need to push anything.
                }
            } else{
                //variable reference
                String varId = ctx.postfix_expr().primary().id().getStart().getText();
                String varType = symbolTable.get(varId);
                AstNode var = new AstNode(null, null, "VarRef", new String[] {varId, varType});
                this.nodePool.push(var);
            }

        } else if(ctx.factor_prefix().getChildCount() > 0){
            //factor prefix exists
            if(ctx.postfix_expr().primary().id() == null){//literal or expression
                if(ctx.postfix_expr().primary().expr() == null){
                    //literal
                    AstNode factorPrefixVar = this.nodePool.pop(); 
                    String literal = ctx.postfix_expr().primary().getStart().getText();
                    if(literal.contains(".")){
                        //float literal
                        AstNode lit = new AstNode(null, null, "FloatLiteral", new String[] {literal, "FLOAT"});
                        factorPrefixVar.Rchild = lit;
                        this.nodePool.push(factorPrefixVar);
                    } else{
                        //int literal
                        AstNode lit = new AstNode(null, null, "IntLiteral", new String[] {literal, "INT"});
                        factorPrefixVar.Rchild = lit;
                        this.nodePool.push(factorPrefixVar);
                    }
                } else{
                    //expression
                    AstNode exprNode = this.nodePool.pop();
                    AstNode factorPrefixVar = this.nodePool.pop();

                    factorPrefixVar.Rchild = exprNode;
                    this.nodePool.push(factorPrefixVar);
                }
            } else {//variable reference
                AstNode factorPrefixVar = this.nodePool.pop(); 
                String varId = ctx.postfix_expr().primary().id().getStart().getText();
                String varType = symbolTable.get(varId);
                AstNode var = new AstNode(null, null, "VarRef", new String[] {varId, varType});
                
                factorPrefixVar.Rchild = var;
                this.nodePool.push(factorPrefixVar);
            }
        }
    }

    @Override 
    public void exitFactor_prefix(LittleParser.Factor_prefixContext ctx){
        //Get current symbol table
        LinkedHashMap<String,String> symbolTable = this.finalTable.get("GLOBAL");
        if(ctx.getChildCount() > 0){//Check rule is not empty
            // System.out.println("\nWe got children\n");
            if(ctx.factor_prefix().getChildCount() > 0){//includes another factor prefix
                if(ctx.postfix_expr().primary().id() == null){//literal or expression
                    if(ctx.postfix_expr().primary().expr() == null){
                        //Literal
                        AstNode mulop = this.nodePool.pop();
                        AstNode mulopVar = this.nodePool.pop();
                        String literal = ctx.postfix_expr().primary().getStart().getText();
                        if(literal.contains(".")){
                            //float literal
                            AstNode lit = new AstNode(null, null, "FloatLiteral", new String[] {literal, "FLOAT"});
                            mulopVar.Rchild = lit;
                            mulop.Lchild = mulopVar;
                            this.nodePool.push(mulop);
                        } else{
                            //int literal
                            AstNode lit = new AstNode(null, null, "IntLiteral", new String[] {literal, "INT"});
                            mulopVar.Rchild = lit;
                            mulop.Lchild = mulopVar;
                            this.nodePool.push(mulop);
                        }
                    } else {
                        //expression
                        AstNode mulop = this.nodePool.pop();
                        AstNode exprNode = this.nodePool.pop();
                        AstNode mulopVar = this.nodePool.pop();
                        
                        mulopVar.Rchild = exprNode;
                        mulop.Lchild = mulopVar;
                        this.nodePool.push(mulop);
                    }
                } else{
                    //variable reference
                    AstNode mulop = this.nodePool.pop();
                    AstNode mulopVar = this.nodePool.pop();
                    String varId = ctx.postfix_expr().primary().id().getStart().getText();
                    String varType = symbolTable.get(varId);
                    AstNode var = new AstNode(null, null, "VarRef", new String[] {varId, varType});
                    mulopVar.Rchild = var;
                    mulop.Lchild = mulopVar;
                    this.nodePool.push(mulop);
                }
            } else{//No extra factor prefix
                AstNode mulop = this.nodePool.pop();
                
                if(ctx.postfix_expr().primary().id() == null){//literal or expression
                    //Literal
                    if(ctx.postfix_expr().primary().expr() == null){
                        String literal = ctx.postfix_expr().primary().getStart().getText();
                        if(literal.contains(".")){
                            //float literal
                            AstNode lit = new AstNode(null, null, "FloatLiteral", new String[] {literal, "FLOAT"});
                            mulop.Lchild = lit;
                            this.nodePool.push(mulop);
                        } else{
                            //int literal
                            AstNode lit = new AstNode(null, null, "IntLiteral", new String[] {literal, "INT"});
                            mulop.Lchild = lit;
                            this.nodePool.push(mulop);
                        }
                    } else {//expression
                        AstNode exprNode = this.nodePool.pop();
                        // System.out.println("Factor > factor_prefix > postfix_expr > primary > expr: ");
                        // AstNode.Print(exprNode);
                        mulop.Lchild = exprNode;
                        this.nodePool.push(mulop);
                    }
                } else{
                    //variable reference
                    String varId = ctx.postfix_expr().primary().id().getStart().getText();
                    String varType = symbolTable.get(varId);
                    AstNode var = new AstNode(null, null, "VarRef", new String[] {varId, varType});
                    mulop.Lchild = var;
                    this.nodePool.push(mulop);
                }
            }
        }
    }

    @Override
    public void exitExpr_prefix(LittleParser.Expr_prefixContext ctx){
        if(ctx.getChildCount() > 0){//Check rule is not empty
            if(ctx.expr_prefix().getChildCount() <= 0){//no expr_prefix
                AstNode addop = this.nodePool.pop();
                AstNode factorVar = this.nodePool.pop();
                addop.Lchild = factorVar;
                this.nodePool.push(addop);
            } else if(ctx.getChildCount() > 2){//all nodes available
                AstNode addop = this.nodePool.pop();
                AstNode factorVar = this.nodePool.pop();
                AstNode expr_prefixVar = this.nodePool.pop();
                
                expr_prefixVar.Rchild = factorVar;
                addop.Lchild = expr_prefixVar;
                this.nodePool.push(addop);
            }
        }
    }

    @Override
    public void exitExpr(LittleParser.ExprContext ctx){
        if(ctx.expr_prefix().getChildCount() == 3 && ctx.factor().getChildCount() == 2){
            AstNode mulExpr = this.nodePool.pop();
            AstNode addExpr = this.nodePool.pop();
            addExpr.Rchild = mulExpr;
            this.nodePool.push(addExpr);
        }
    }

    @Override
    public void exitAssign_expr(LittleParser.Assign_exprContext ctx){
        //Get current symbol table
        LinkedHashMap<String,String> symbolTable = this.finalTable.get("GLOBAL");

        //Get variable name being assigned to and make AST node from it
        String varId = ctx.id().getStart().getText();
        String varType = symbolTable.get(varId);
        AstNode assignId = new AstNode(null,null,"VarRef",new String[] {varId, varType});
        
        //Get completed expr node 
        AstNode exprNode = this.nodePool.pop();

        //Create assignment node and put variable as left child and expression as right
        AstNode assignNode = new AstNode(assignId, exprNode, "AssignExpr(:=)", null);
        this.treeList.add(assignNode);
    }


    ///////// These functions create, push and pop symbol tables as new scope is entered
    @Override
    public void enterProgram(LittleParser.ProgramContext ctx){
        LinkedHashMap<String, String> GLOBAL = new LinkedHashMap<String, String>();
        //create and add global scope to scopestack and to final table
        this.scopeStack.push(GLOBAL);
        this.finalTable.put("GLOBAL", this.scopeStack.peek());
    }

    @Override
    public void exitProgram(LittleParser.ProgramContext ctx){
        //pop global symbol table from scope stack 
        this.scopeStack.pop();
    }


    @Override
    public void enterFunc_decl(LittleParser.Func_declContext ctx){
        LinkedHashMap<String, String> funcScope = new LinkedHashMap<String, String>();
        //create and add function scope to scopestack and to final table
        this.scopeStack.push(funcScope);
        this.finalTable.put(ctx.id().getStart().getText(), this.scopeStack.peek());
    }

    @Override
    public void exitFunc_decl(LittleParser.Func_declContext ctx){
        //remove now complete function table from scopestack
        this.scopeStack.pop();
    }


    @Override
    public void enterWhile_stmt(LittleParser.While_stmtContext ctx){
        LinkedHashMap<String, String> whileScope = new LinkedHashMap<String, String>();
        //create and add while loop scope to scopestack and to final table
        this.scopeStack.push(whileScope);
        this.finalTable.put("BLOCK " + this.blockIter++, this.scopeStack.peek());
    }

    @Override
    public void exitWhile_stmt(LittleParser.While_stmtContext ctx){
        this.scopeStack.pop();
    }


    @Override
    public void enterIf_stmt(LittleParser.If_stmtContext ctx){
        LinkedHashMap<String, String> thenScope = new LinkedHashMap<String, String>();
        //create and add then block scope to scopestack and to final table
        this.scopeStack.push(thenScope);
        this.finalTable.put("BLOCK " + this.blockIter++, this.scopeStack.peek());
    }

    @Override
    public void exitIf_stmt(LittleParser.If_stmtContext ctx){
        if(!(ctx.else_part().getChildCount() > 0)){//pop then block scope since if_else did not
            this.scopeStack.pop();
        }
    }

    @Override
    public void enterElse_part(LittleParser.Else_partContext ctx){
        if(ctx.getChildCount() > 0){
            //Pop previous then scope block
            this.scopeStack.pop();
            //create and add else block scope to scopestack and to final table
            LinkedHashMap<String, String> elseScope = new LinkedHashMap<String, String>();
            this.scopeStack.push(elseScope);
            this.finalTable.put("BLOCK " + this.blockIter++, this.scopeStack.peek());
        }
    }

    @Override
    public void exitElse_part(LittleParser.Else_partContext ctx){
        if(ctx.getChildCount() > 0){// pop else block scope from scope stack
            this.scopeStack.pop();
        }
    }

    ///////////////These functions watch for variable declaration and add to its current symbol table
    @Override
    public void exitVar_decl(LittleParser.Var_declContext ctx){
        String type = ctx.var_type().getStart().getText();
        String varName = ctx.id_list().getStart().getText();
        LinkedHashMap<String, String> scope = this.scopeStack.peek();//get current scope
        LittleParser.Id_tailContext commaCtx = ctx.id_list().id_tail();
        //put first var id into table and create first AST node
        if(!(scope.containsKey(varName))){
            scope.put(varName, type);
            AstNode varNode = new AstNode(null, null, "VarDecl", new String[] {varName, type});
            this.treeList.push(varNode);
            while(commaCtx.getStart().getText().contains(",")){
                //get next variable id's if any exist and not already declared
                varName = commaCtx.id().getStart().getText();
                if(!(scope.containsKey(varName))){
                    scope.put(varName, type);
                    varNode = new AstNode(null, null, "VarDecl", new String[] {varName, type});
                    this.treeList.push(varNode);
                    commaCtx = commaCtx.id_tail();
                } else{//duplicate declaration found
                    if(this.errorVar.isEmpty()){
                        this.errorVar = varName;
                    }
                }
            }
        } else{
            if(this.errorVar.isEmpty()){
                this.errorVar = varName;
            }
        }
    }

    @Override
    public void exitString_decl(LittleParser.String_declContext ctx){
        LinkedHashMap<String,String> scope = this.scopeStack.peek();//get current scope
        //add string identifier/value pair to current scope table
        String strName = ctx.id().getStart().getText();
        String strVal = ctx.str().getStart().getText();
        if(!(scope.containsKey(strName))){
            scope.put(strName,  strVal);
            AstNode stringNode = new AstNode(null, null, "StringDecl", new String[] {strName, strVal});
            this.treeList.push(stringNode);
        } else {
            if(this.errorVar.isEmpty()){
                this.errorVar = strName;
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