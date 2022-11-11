package data_structures;

import java.util.ArrayList;

public class ParseTreeNode {

    static int NODEID = 0;
    public int nodeId;
    public int wordOrder = -1; // word order within the sentence
    public String label; // word
    public String posValue = null;
    public String relationship = null; // dependency relationship
    public String tokenType = null; // CommandToken, NounToken, VerbToken...
    public  String function = null; // Only exist in function or orderby token
    public ParseTreeNode parent;
    public ArrayList<ParseTreeNode> children = new ArrayList<ParseTreeNode>();

    public String QT = "";
    public String prep = "";
    public String leftRel = "";
    // list of mapped schema elements
    public int choice = -1;
    public boolean isAdded = false;

    public ParseTreeNode(int wordOrder, String label, String posValue, String relationship, ParseTreeNode parent) {
        this.nodeId = NODEID;
        NODEID++;
        this.wordOrder = wordOrder;
        this.label = label;
        this.posValue = posValue;
        this.relationship = relationship;
        this.parent = parent;
    }


}
