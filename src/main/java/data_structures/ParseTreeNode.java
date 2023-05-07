package data_structures;

import database.elements.MappedSchemaElement;

import java.io.Serializable;
import java.util.ArrayList;

public class ParseTreeNode implements Serializable {

    static int NODEID = 0;
    public int nodeId;
    public int wordOrder = -1; // word order within the sentence
    public String label; // word
    public String posValue = "NA";
    public String relationship = "NA"; // dependency relationship
    public String tokenType = "NA"; // CommandToken, NounToken, VerbToken...
    public  String function = "NA"; // Only exist in function or orderby token

    public ParseTreeNode valuePredicate = null;
    public String nerVal  = "NA";

    public String valueType = "NA";
    public ParseTreeNode parent;
    public ArrayList<ParseTreeNode> children = new ArrayList<ParseTreeNode>();

    public ArrayList<MappedSchemaElement> mappedSchemaElements = new ArrayList<MappedSchemaElement>();

    public String QT = "";
    public String prep = "";
    public String leftRel = "";
    // list of mapped schema elements
    public int choice = -1;
    public boolean isAdded = false;
    public boolean isValueVerbAssoc = false;

    public boolean isQuoted = false;

    public boolean isNegated = false;

    public ParseTreeNode correspondingProjectedValueNode = null;
    public  boolean isProjectedAttribute = false;

    public ParseTreeNode(int wordOrder, String label, String posValue, String relationship, String isQuotedS, ParseTreeNode parent) {
        this.nodeId = NODEID;
        NODEID++;
        this.wordOrder = wordOrder;
        this.label = label;
        this.posValue = posValue;
        this.relationship = relationship;
        if(isQuotedS.equals("true")) {
            isQuoted = true;
        }
        this.parent = parent;
    }

    public MappedSchemaElement getNodeChoice() {
        if (choice >= 0 && mappedSchemaElements.size() > 0) {
            return mappedSchemaElements.get(choice);
        }
        return null;
    }


    public boolean checkIsRelatedByForToken(String tokenType) {
        boolean isRelated = false;
        if(this.parent.tokenType.equals(tokenType)) {
            isRelated = true;
        } else {
            for(int i=0; i<this.children.size(); i++) {
                if(this.children.get(i).tokenType.equals(tokenType)) {
                    isRelated = true;
                    break;
                }
            }


            // check sibling
            for(int i=0; i<this.parent.children.size(); i++) {
                if(!parent.tokenType.startsWith("NT") && this.parent.children.get(i).tokenType.equals(tokenType)) {
                    isRelated = true;
                    break;
                }
            }
        }

        return isRelated;
    }

    public boolean isRelatedToQuestionWord() {
        if(this.checkIsRelatedByForToken("CMT")
                || this.checkIsRelatedByForToken("WPT") || this .checkIsRelatedByForToken("HPT")) {
            return true;
        }
        if(this.parent.posValue.startsWith("VB") && this.parent.parent.isQuestionWord() && this.parent.parent.children.size() == 1) {
            for (ParseTreeNode child : this.parent.children) {
                if(child.tokenType.equals("NT") && !child.equals(this)) {
                    return false;
                }
            }
            // only NT child in the VB scenario
            return true;
        }
        return false;
    }

    public boolean isDirectChildOfQuestionWord() {
        if(this.parent.tokenType.equals("CMT")
                || this.parent.tokenType.equals("WPT") || this.parent.tokenType.equals("HPT")) {
            return true;
        }
        return false;
    }

    public boolean isQuestionWord() {
        return tokenType.equals("WPT") || tokenType.equals("CMT") || tokenType.equals("HPT");
    }


}
