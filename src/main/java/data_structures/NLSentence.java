package data_structures;

import java.util.ArrayList;

public class NLSentence {
    public ArrayList<ParseTreeNode> allNodes = new ArrayList<>();
    public ArrayList<String> words = new ArrayList<>();
    public ArrayList<Boolean> isImplicit = new ArrayList<>();



    public void addNode(ParseTreeNode node, String word, boolean isImplicit) {
        this.allNodes.add(node);
        this.words.add(word);
        this.isImplicit.add(isImplicit);
    }

    public void printGeneralSentence() {
        String result ="";
        for (int i = 0; i < words.size(); i++) {
            if(isImplicit.get(i)) {
                continue;
            } else {
                result+=words.get(i);
            }

            if(i!= words.size()-1) {
                result+=" ";
            } else {
                result+=".";
            }
        }

        System.out.println(result);
    }


    public ArrayList<String> Specific() {
        ArrayList<String> results = new ArrayList<>();
        String result = "";

        for (int i = 0; i < words.size(); i++) {
            if(isImplicit.get(i)) {
                result += "#implicit " + words.get(i);
            } else {
                result += "#explicit " + words.get(i);
            }

            if(i!= words.size()-1) {
                result+=" ";
            } else {
                result+=".";
            }

            result += "\n";
            results.add(result);
            result = "";
        }
        return results;

    }
}
