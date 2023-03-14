package data_structures;

import database.SchemaGraph;
import database.elements.MappedSchemaElement;
import settings.Parameters;

import java.util.ArrayList;

public class Query {
    public SchemaGraph graph;

    public Sentence sentence;

    public Parameters.QUERY_TYPE queryType;

    public boolean isNegated = false;

    public ArrayList<String[]> treeTable = new ArrayList<String[]>(); // the dependency tree table: Position, Phrase, Tag, Parent, all strings; each phrase is an entry
    public ArrayList<String> conjunctionTable = new ArrayList<String>(); // conjunction table: a^b

    public ParseTree originalParseTree; // when making alterations to check for big deviations?
    public ParseTree parseTree;

    public ArrayList<MappedSchemaElement> mappedPredicates = new ArrayList<>();

    public ArrayList<IntermediateTree> candidateTrees = new ArrayList<>();
    public ArrayList<ParseTree> adjustedTrees = new ArrayList<>();
    public ArrayList<NLSentence> NLSentences = new ArrayList<>();

    public int queryTreeID = 0;
    public ParseTree queryTree = new ParseTree();

    public int secondaryWTokenOrder = -1;


    public ArrayList<EntityPair> entities = new ArrayList<EntityPair>();

    // intermediate tree handling

    public Block mainBlock;
    public ArrayList<Block> blocks = new ArrayList<>();
    public String translatedSQL = "";
    public ArrayList<ArrayList<String>> finalResult = new ArrayList<>();


    public Query(String query, SchemaGraph graph) {
        this.sentence = new Sentence(query);
        sentence.printTokens();
        this.graph = graph;
    }

    public void printTreeTable() {
        for(int i=0; i<treeTable.size(); i++) {
            for(int j=0; j<treeTable.get(i).length; j++) {
                System.out.print(treeTable.get(i)[j]+ " ");
            }
            System.out.println();
        }
    }






}
