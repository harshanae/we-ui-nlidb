package command_interface;

import components.*;
import configuration.DBConfig;
import data_structures.Query;
import database.Database;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.Scanner;

public class CommandInterface {
    LexicalizedParser lexicalizedParser;
    Document tokens;
    Query query;
    Database db;

    public CommandInterface(String dbName) throws Exception {
        lexicalizedParser = LexicalizedParser.loadModel("edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz");
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        tokens = builder.parse(new File("files/tokens.xml"));
        System.out.println("Welcome to WE-UI NLIDB...");

        this.db = new Database(DBConfig.getProperty("dbHost"),
                DBConfig.getIntegerProperty("dbPort"),
                DBConfig.getProperty("dbUser"),
                DBConfig.getProperty("dbPassword"), dbName);
    }

    public void readQuery() throws Exception {
        String q = "";
        Scanner scan = new Scanner(System.in);
        while (!q.equals("exit")) {
            System.out.println("Enter the query");
             q = scan.nextLine();

             if(q.equals("exit")) {
                 break;
             }

            query = new Query(q, db.schemaGraph);

            // dependency parse the query
            StanfordNLParser.initiateParse(query, lexicalizedParser);

            query.printTreeTable();

            this.processQuery(query);
            IdentifyEntitites.idEntities(query);
            IntermediateTreeHandler.handleIntermediateTree(query, db);
            SQLEngine.generateSQL(query, db);
            System.out.println("Conjunction array: ");
            for (int i = 0; i < query.conjunctionTable.size(); i++) {
                System.out.println(query.conjunctionTable.get(i));
                if(query.queryTree.allNodes.size()>=2) {
                    System.out.println(query.queryTree.searchNodeByOrder(Integer.parseInt(query.conjunctionTable.get(i).split(" ")[0])).label);
                    System.out.println(query.queryTree.searchNodeByOrder(Integer.parseInt(query.conjunctionTable.get(i).split(" ")[1])).label);
                }
            }
        }
    }

    public void processQuery(Query query) throws Exception {
        KeywordMapper.mapKeywords(query, db, tokens);
    }

    public static void main(String[] args) throws Exception {
        CommandInterface commandInterface = new CommandInterface("mas");
        commandInterface.readQuery();
    }


}
