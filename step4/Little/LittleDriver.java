/*
    Author: Logan Shy
*/
import java.util.*;
import java.util.regex.*;

import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.*;
import static org.antlr.v4.runtime.CharStreams.fromFileName;


// To run code generation, exectue micro.sh script with an input file
public class LittleDriver {

    public static void main(String[] args) throws Exception {
        ////A relative file path to a .micro file should be passed
        ////Example: 'micro.sh ../inputs/test1.micro
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

        //Generate code
        String finalCode = "";
        Stack<CodeObject> codeStack = new Stack<CodeObject>();
        while(!listener.treeList.isEmpty()){
            AstNode.GenerateCode(listener.treeList.removeFirst(), codeStack);
            while(!codeStack.empty()){//concat code from all objects in generated code stack
                finalCode += codeStack.pop().code;
            }
        }

        //Write final code to console
        System.out.println(finalCode);
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
                if(!(Pattern.matches("r[0-9]+", assignee.temp))){//assigning a literal
                    code += "move " + assignee.temp + " " + tempName + "\n" +
                            "move " + tempName + " " + assigner.temp + "\n";
                } else{//assigning expression inside of register
                    code += "move" + " " + assignee.temp + " " + assigner.temp + "\n";
                }
                typeName = assignee.type;
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
                String addRegister = "";
                String leftTemp = "[LEFT TEMP NAME]";
                String rightTemp = "[RIGHT TEMP NAME]";


                //Guaranteed two nodes from left and right child to be added
                CodeObject rightObject = generatedCode.pop();
                CodeObject leftObject = generatedCode.pop();

                //Left child code generation
                if(!(Pattern.matches("r[0-9]+", leftObject.temp))){//var reference or literal
                    addRegister = tempName;
                    leftTemp = addRegister;
                    code += "move " + leftObject.temp + " " + addRegister + "\n";
                } else if(Pattern.matches("r[0-9]+", leftObject.temp)){//expression
                    //load generated code from expression and add to new code
                    addRegister = tempName;
                    leftTemp = leftObject.temp;
                    code += leftObject.code + "move " + leftTemp + " " + addRegister + "\n";
                }

                //Right child code generation
                if(!(Pattern.matches("r[0-9]+", rightObject.temp))){//var reference or literal
                    rightTemp = rightObject.temp;
                } else if(Pattern.matches("r[0-9]+", rightObject.temp)){//expression
                    //load generated code from expression and add to new code
                    rightTemp = rightObject.temp;
                    code += rightObject.code;
                }


                //Finalize Add expr Code generation
                if(head.type.contains("+")){//use Add instruction variant
                    if(leftObject.type.contains("INT")){//Use AddI instruction
                        code += "addi "+ rightTemp +" "+ addRegister +"\n";
                        typeName = "INT";
                    } else if(leftObject.type.contains("FLOAT")){//Use AddF instruction
                        code += "addr "+ rightTemp +" "+ addRegister +"\n";
                        typeName = "FLOAT";
                    }
                } else if(head.type.contains("-")){//Use Sub instruction variant
                    if(leftObject.type.contains("INT")){//Use SubI instruction
                        code += "subi "+ rightTemp +" "+ addRegister +"\n";
                        typeName = "INT";
                    } else if(leftObject.type.contains("FLOAT")){//Use SubF instruction
                        code += "subr "+ rightTemp +" "+ addRegister +"\n";
                        typeName = "FLOAT";
                    }
                }
                //Create new Code Object and push up
                CodeObject AddExpr = new CodeObject(tempName, typeName, code);
                generatedCode.push(AddExpr);
            
            } else if (head.type.contains("MulExpr")){//Mul or div expression
                //Generate new temp for name
                String tempName = CreateTemporary();
                String typeName = "";
                String code = "";
                String mulRegister = "";
                String leftTemp = "[LEFT TEMP NAME]";
                String rightTemp = "[RIGHT TEMP NAME]";


                //Guaranteed two nodes from left and right child to be added
                CodeObject rightObject = generatedCode.pop();
                CodeObject leftObject = generatedCode.pop();

                //Left child code generation
                if(!(Pattern.matches("r[0-9]+", leftObject.temp))){//var reference or literal
                    mulRegister = tempName;
                    leftTemp = mulRegister;
                    code += "move " + leftObject.temp + " " + mulRegister + "\n";
                } else if(Pattern.matches("r[0-9]+", leftObject.temp)){//expression
                    //load generated code from expression and add to new code
                    mulRegister = tempName;
                    leftTemp = leftObject.temp;
                    code += leftObject.code + "move " + leftTemp + " " + mulRegister + "\n";
                }

                //Right child code generation
                if(!(Pattern.matches("r[0-9]+", rightObject.temp))){//var reference or literal
                    rightTemp = rightObject.temp;
                } else if(Pattern.matches("r[0-9]+", rightObject.temp)){//expression
                    //load generated code from expression and add to new code
                    rightTemp = rightObject.temp;
                    code += rightObject.code;
                }
                
                //Finalize mul expr Code generation
                if(head.type.contains("*")){//use mul instruction variant
                    if(leftObject.type.contains("INT")){//Use mulI instruction
                        code += "muli "+ rightTemp +" "+ mulRegister +"\n";
                        typeName = "INT";
                    } else if(leftObject.type.contains("FLOAT")){//Use mulF instruction
                        code += "mulr "+ rightTemp +" "+ mulRegister +"\n";
                        typeName = "FLOAT";
                    }
                } else if(head.type.contains("/")){//Use div instruction variant
                    if(leftObject.type.contains("INT")){//Use divI instruction
                        code += "divi "+ rightTemp +" "+ mulRegister +"\n";
                        typeName = "INT";
                    } else if(leftObject.type.contains("FLOAT")){//Use divF instruction
                        code += "divr "+ rightTemp +" "+ mulRegister +"\n";
                        typeName = "FLOAT";
                    }
                }
                //Create new Code Object and push up
                CodeObject mulExpr = new CodeObject(tempName, typeName, code);
                generatedCode.push(mulExpr);


            } else if (head.type.contains("VarDecl")){//variable declaration
                String tempName = head.data[0];
                String typeName = head.data[1];
                String code = "var "+tempName +"\n";
                CodeObject varObject = new CodeObject(tempName, typeName, code);
                generatedCode.push(varObject);

            } else if(head.type.contains("StringDecl")){//String declaration
                String strName = head.data[0];
                String strVal = head.data[1];
                String code = "str " + strName + " " + strVal +"\n";
                CodeObject strObject = new CodeObject(strName, "STRING", code);
                generatedCode.push(strObject);

            } else if(head.type.contains("WriteExpr")){//write statement
                String tempName = "WRITE";
                String typeName = "";
                String code = "";
                if(head.data[1].contains("\"")){//write string
                    typeName = "STRING";
                    code = "sys writes " + head.data[0] +"\n";
                    CodeObject strObject = new CodeObject(tempName, typeName, code);
                    generatedCode.push(strObject);
                } else{//float or integer
                    if(head.data[1].contains("INT")){//write int
                        typeName = head.data[1];
                        code = "sys writei " + head.data[0] +"\n";
                    } else if(head.data[1].contains("FLOAT")){//write float
                        typeName = head.data[1];
                        code = "sys writer " + head.data[0] +"\n";
                    } else{
                        System.out.println("\nunexpected error occurred\n");
                    }
                    CodeObject varObject = new CodeObject(tempName, typeName, code);
                    generatedCode.push(varObject);
                }

            } else if (head.type.contains("ReadExpr")){//read expression
                String tempName = "READ";
                String typeName = "";
                String code = "";
                if(head.data[1].contains("INT")){//Read int
                    typeName = head.data[1];
                    code = "sys readi " + head.data[0] +"\n";
                } else if(head.data[1].contains("FLOAT")){//Read Float
                    typeName = head.data[1];
                    code = "sys readf " + head.data[0] +"\n";
                } else {
                    System.out.println("\nunexpected error occurred\n");
                }
                CodeObject readObject = new CodeObject(tempName, typeName, code);
                generatedCode.push(readObject);

            } else if (head.type.contains("HALT")){//end of program
                String tempName = "HALT";
                String typeName = "";
                String code = "sys halt";
                CodeObject haltObject = new CodeObject(tempName, typeName, code);
                generatedCode.push(haltObject);
            }
        }
    }

    public static String CreateTemporary(){
        MyLittleListener.tempCounter++;
        String temporary = "r" + MyLittleListener.tempCounter;
        return temporary;
    }
}


class MyLittleListener extends LittleBaseListener{
    
    public static int tempCounter = -1;
    Stack<AstNode> nodePool = new Stack<AstNode>();
    LinkedList<AstNode> treeList = new LinkedList<AstNode>();
    // Init new symbole table
    int blockIter = 1;
    String errorVar = "";
    Stack<LinkedHashMap<String,String>> scopeStack = new Stack<LinkedHashMap<String,String>>();
    LinkedHashMap<String, LinkedHashMap<String,String>> finalTable = new LinkedHashMap<String, LinkedHashMap<String,String>>();

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
    public void exitRead_stmt(LittleParser.Read_stmtContext ctx){
        LinkedHashMap<String,String> symbolTable = this.finalTable.get("GLOBAL");
        //read first variable
        String varId = ctx.id_list().id().getStart().getText();
        String varType = symbolTable.get(varId);
        AstNode readop = new AstNode(null, null, "ReadExpr", new String[] {varId, varType});
        this.treeList.add(readop);
        //check for subsequent ids in id_tails()
        if(ctx.id_list().id_tail().getChildCount() > 0){//more to write
            LittleParser.Id_tailContext tailCtx = ctx.id_list().id_tail();
            while(true){
                varId = tailCtx.id().getStart().getText();
                varType = symbolTable.get(varId);
                AstNode nextReadOp = new AstNode(null, null, "ReadExpr", new String[] {varId, varType});
                this.treeList.add(nextReadOp);
                if(tailCtx.id_tail().getChildCount() > 0){//more to print
                    tailCtx = tailCtx.id_tail();
                } else{//no more to print, break out
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

        //Generate final return AST node
        AstNode returnNode = new AstNode(null, null, "HALT", null);
        this.treeList.add(returnNode);
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
            this.treeList.add(varNode);
            while(commaCtx.getStart().getText().contains(",")){
                //get next variable id's if any exist and not already declared
                varName = commaCtx.id().getStart().getText();
                if(!(scope.containsKey(varName))){
                    scope.put(varName, type);
                    varNode = new AstNode(null, null, "VarDecl", new String[] {varName, type});
                    this.treeList.add(varNode);
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