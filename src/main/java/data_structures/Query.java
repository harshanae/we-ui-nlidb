package data_structures;

import database.SchemaGraph;

import java.util.ArrayList;

public class Query {
    public SchemaGraph graph;

    public Sentence sentence;

    public ArrayList<String[]> treeTable = new ArrayList<String[]>(); // the dependency tree table: Position, Phrase, Tag, Parent, all strings; each phrase is an entry
    public ArrayList<String> conjunctionTable = new ArrayList<String>(); // conjunction table: a^b

    public Query(String query, SchemaGraph graph) {
        this.sentence = new Sentence(query);
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
