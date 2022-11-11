package data_structures;

import java.util.ArrayList;
import java.util.LinkedList;

public class ParseTree {
    public ParseTreeNode root;
    public ArrayList<ParseTreeNode> allNodes = new ArrayList<ParseTreeNode>();
    public ArrayList<ParseTreeNode> deletedNodes = new ArrayList<ParseTreeNode>();

    public ParseTree() {
        root = new ParseTreeNode(0, "ROOT", "ROOT", "ROOT", null);
        allNodes.add(root);
        root.tokenType = "ROOT";
    }

    public boolean buildNode (String [] input) {
        ParseTreeNode node;
        if(root.children.isEmpty()) {
            node = new ParseTreeNode(Integer.parseInt(input[0]), input[1], input[2],input[4], root);
            root.children.add(node);
            allNodes.add(node);
            return true;
        } else {
            LinkedList<ParseTreeNode> list = new LinkedList<ParseTreeNode>();
            list.add(root);
            while(!list.isEmpty()) {
                ParseTreeNode parent = list.removeFirst();
                if (parent.wordOrder == Integer.parseInt(input[3])) {
                    node = new ParseTreeNode(Integer.parseInt(input[0]), input[1], input[2], input[4], parent);
                    parent.children.add(node);
                    allNodes.add(node);
                    return true;
                }
                list.addAll(parent.children);
            }
        }
        return  false;
    }

    public ParseTreeNode searchNodeByOrder(int order) {
        for(int i=0; i<this.allNodes.size(); i++) {
            if(this.allNodes.get(i).wordOrder == order) {
                return this.allNodes.get(i);
            }
        }
        return null;
    }

    public ParseTreeNode searchNodeByID(int id) {
        for(int i=0; i<this.allNodes.size(); i++) {
            if(this.allNodes.get(i).nodeId == id) {
                return this.allNodes.get(i);
            }
        }
        return null;
    }

    public void deleteNode(ParseTreeNode node) {
        ParseTreeNode parent = node.parent;
        node.parent = null;
        int position = parent.children.indexOf(node);
        parent.children.remove(node);

        if(!node.leftRel.isEmpty() && node.children.size() > 0) {
            node.children.get(0).leftRel = node.leftRel;
        }

        for (int i=0; i<node.children.size(); i++) {
            // add node's children to node's parent from nodes position onwards
            parent.children.add(position+i, node.children.get(i));
            node.children.get(i).parent = parent;
        }
        allNodes.remove(node);

        if(!node.tokenType.equals(("QT"))) {
            this.deletedNodes.add(node);
        }
    }

    public String parseTreeToString() {
        String result = "";
        LinkedList<ParseTreeNode> nodeList = new LinkedList<ParseTreeNode>();
        nodeList.add(root);

        LinkedList<Integer> levelList = new LinkedList<Integer>();
        levelList.add(0); // level of root is 0

        while (!nodeList.isEmpty()) {
            ParseTreeNode currentNode = nodeList.removeLast();
            int currentLevel = levelList.removeLast();
            for(int i=0; i<currentLevel; i++) {
                result += "    ";
            }
            result+= "("+currentNode.nodeId + ")";
            result += currentNode.label + "\n";

            // add all children of the current node and their levels -> all children are of the same level (currentNodeLevel + 1)
            for(int i=0; i<currentNode.children.size(); i++) {
                nodeList.add(currentNode.children.get(currentNode.children.size()-i-1)); // add from last so that the first node appears last because we remove last for printing
                levelList.add(currentLevel+1);
            }
        }

        return result;
    }
}
