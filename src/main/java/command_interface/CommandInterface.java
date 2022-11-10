package command_interface;

import components.StanfordNLParser;
import configuration.DBConfig;
import data_structures.Query;
import database.Database;
import database.SchemaGraph;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import org.w3c.dom.Document;

import javax.xml.crypto.Data;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class CommandInterface {
    LexicalizedParser lexicalizedParser;
    Document tokens;
    Query query;
    Database db;

    public CommandInterface() throws Exception {
        lexicalizedParser = LexicalizedParser.loadModel("edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz");
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
//        tokens = builder.parse(new File("src/zfiles/tokens.xml"));

//        db = new Database(DBConfig.getProperty("dbHost"), DBConfig.getIntegerProperty("dbPort"),
//                DBConfig.getProperty("dbUser"), DBConfig.getProperty("dbPassword"), "mas");
    }

    public void readQuery() throws FileNotFoundException {
        Scanner scan = new Scanner(System.in);
        System.out.println("Enter the query");
        String q = scan.nextLine();
        SchemaGraph schemaGraph = new SchemaGraph("mas");

        query = new Query(q, schemaGraph);

        StanfordNLParser.StanfordParse(query, lexicalizedParser);

        query.printTreeTable();



    }

    public static void main(String args[]) throws Exception {
        CommandInterface commandInterface = new CommandInterface();
        commandInterface.readQuery();
    }


}
