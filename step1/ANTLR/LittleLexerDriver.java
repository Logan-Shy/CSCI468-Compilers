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
// 3. 'java LittleLexerDriver ./path/to/input/file' (this runs the driver)

public class LittleLexerDriver {
  public static void main(String[] args) throws Exception {
    ////A relative file path to a .micro file should be passed
    ////as a command line argument when running this driver file,
    ////along with a name for the output file.
    ////Example: 'java LittleLexerDriver ../inputs/fibonacci.micro'
    String filePath = args[0];
    CharStream fileStream = fromFileName(filePath);
    LittleLexer lexer = new LittleLexer(fileStream);
    String tempStr;

    while(true) {
        tempStr = "";
        Token token = lexer.nextToken();
        if(token.getType() == LittleLexer.EOF) {
          break;
        }
        tempStr += ("Token Type: " + indexToToken(token.getType())+"\n");
        tempStr += ("Value: " + token.getText());
        //System.out.println(token.getType() + " :: " + token.getText());
        // pw.write(tempStr);
        System.out.println(tempStr);
    }

    // try {
    //     PrintWriter pw = new PrintWriter(new FileWriter(outputFile));
    //     String tempStr;

    //     while(true) {
    //         tempStr = "";
    //         Token token = lexer.nextToken();
    //         if(token.getType() == LittleLexer.EOF) {
    //           break;
    //         }
    //         tempStr += ("Token Type: " + indexToToken(token.getType()) + "\n");
    //         tempStr += ("Value: " + token.getText() + "\n");
    //         //System.out.println(token.getType() + " :: " + token.getText());
    //         pw.write(tempStr);
    //     }
    //     pw.close();
    // } catch (IOException e) {
    //     e.printStackTrace();
    // }
    
  
}


  // Static Helper function to convert a given file to a string
  public static String fileToString(String filePath) {
      String fileString = "";
      try {
          fileString = new String (Files.readAllBytes(Paths.get(filePath)));
      } catch (IOException e){
          e.printStackTrace();
      }
      return fileString;
  }


  // Static Helper function to convert token types to their literal 
  //String representation. Can return:
  //OPERATOR
  //KEYWORD
  //STRINGLITERAL
  //INTLITERAL
  //FLOATLITERAL
  //IDENTIFIER
  public static String indexToToken(Integer index){
      switch(index){
            case 1 : return "KEYWORD"; 
            case 2 : return "KEYWORD"; 
            case 3 : return "KEYWORD"; 
            case 4 : return "KEYWORD"; 
            case 5 : return "KEYWORD"; 
            case 6 : return "KEYWORD"; 
            case 7 : return "KEYWORD"; 
            case 8 : return "KEYWORD"; 
            case 9 : return "KEYWORD"; 
            case 10 : return "KEYWORD"; 
            case 11 : return "KEYWORD"; 
            case 12 : return "KEYWORD"; 
            case 13 : return "KEYWORD"; 
            case 14 : return "KEYWORD"; 
            case 15 : return "KEYWORD"; 
            case 16 : return "KEYWORD"; 
            case 17 : return "KEYWORD"; 
            case 18 : return "KEYWORD"; 
            case 19 : return "OPERATOR"; 
            case 20 : return "OPERATOR"; 
            case 21 : return "OPERATOR"; 
            case 22 : return "OPERATOR"; 
            case 23 : return "OPERATOR"; 
            case 24 : return "OPERATOR"; 
            case 25 : return "OPERATOR"; 
            case 26 : return "OPERATOR"; 
            case 27 : return "OPERATOR"; 
            case 28 : return "OPERATOR"; 
            case 29 : return "OPERATOR"; 
            case 30 : return "OPERATOR"; 
            case 31 : return "OPERATOR"; 
            case 32 : return "OPERATOR"; 
            case 33 : return "OPERATOR"; 
            case 34 : return "IDENTIFIER"; 
            case 35 : return "INTLITERAL"; 
            case 36 : return "FLOATLITERAL"; 
            case 37 : return "STRINGLITERAL"; 
            default: return "no type match";
      }
  }
}