package data_structures;

import database.SchemaGraph;
import settings.Parameters;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;

public class IntermediateTree implements Serializable, Comparable<IntermediateTree>{

    public IntermediateTreeNode root;
    public ArrayList<IntermediateTreeNode> allTreeNodes = new ArrayList<>();

    public int hashNum = 1;
    public int cost = 0;
    public double weight = 1;
    public int invalid = 0;
    // test remove later
    public int haveCInvalid = 0;
    public ArrayList<String> cinv = new ArrayList<>();

    public IntermediateTree(ParseTree parseTree) {
        // add each parsetree node to intermediate tree
        for (ParseTreeNode parseTreeNode: parseTree.allNodes) {
            allTreeNodes.add(new IntermediateTreeNode(parseTreeNode));
        }

        // assign root as root
        root = allTreeNodes.get(0);

        for (int i = 0; i < allTreeNodes.size(); i++) {
            // assign parents to intermediate tree nodes according to parse tree
            IntermediateTreeNode treeNode = allTreeNodes.get(i);
            ParseTreeNode parseTreeNode = parseTree.allNodes.get(i);
            ParseTreeNode parseTreeNodeParent = parseTreeNode.parent;
            int parseTreeNodeParentPos = parseTree.allNodes.indexOf(parseTreeNodeParent);
            if(parseTreeNodeParentPos >= 0) {
                treeNode.parent = allTreeNodes.get(parseTreeNodeParentPos);
            } else {
                treeNode.parent = null;
            }

            // add children of each intermediate node
            for (ParseTreeNode childParseTreeNode : parseTreeNode.children) {
                // retrieve position since it may vary from parse tree all nodes to intermediate all nodes
                int pTchildPos = parseTree.allNodes.indexOf(childParseTreeNode);
                treeNode.children.add(allTreeNodes.get(pTchildPos));
            }
        }
        // calculate hash value for tree
        getTreeHashNumber();
    }

    public void evaluateTree(SchemaGraph schemaGraph, Query query) {
        // initialize node weights, haveChildren and upValidity
        for (IntermediateTreeNode treeNode: allTreeNodes) {
            treeNode.haveChildren = new ArrayList<>();
            treeNode.upValid = true;
            treeNode.weight = 1;
        }

        // tree weight -> 1 , invalid nodes -> 0
        weight = 1;
        invalid = 0;

        // evaluate each node
        for (IntermediateTreeNode treeNode: allTreeNodes) {
            treeNode.evaluateNode(schemaGraph, query);
        }

        // check if each node is invalid oj upvalidity and increment invalid
        for (IntermediateTreeNode treeNode: allTreeNodes) {
            if(!treeNode.upValid) {
                invalid ++;
            }
            for (int i = 0; i < treeNode.haveChildren.size(); i++) {
                if(!treeNode.haveChildren.get(i)) {
                    invalid++;
                    haveCInvalid++;
                    cinv.add("("+i+")"+treeNode.label);
                }
            }
            // update weight by multiplying with each node weight
            this.weight *= treeNode.weight;
//            System.out.println("node: "+treeNode.label+ "inv: "+treeNode.upValid+" ch: "+treeNode.haveChildren.size());
        }
    }

    public boolean moveSubtree(IntermediateTreeNode newParent, IntermediateTreeNode treeNode, Query query) {
        // print new parent and tree node
//        System.out.print("\nnew parent: " + newParent.label+ "\t" + "tree node: " + treeNode.label);

        // cannot move in under the node itself
        if(newParent.equals(treeNode)){
//            System.out.print(": false(both equal)\n");
            return  false;
        // cannot move it under the same parent
        } else if (newParent.equals(treeNode.parent)) {
//            System.out.print(": false(new parent is already parent)\n");
            return false;
        }

        boolean isParent = false;
        IntermediateTreeNode t = newParent;
        // check if treeNode is the parent of new parent
        while(t!=null) {
            if(t.parent != null && t.equals(treeNode)) {
                isParent = true;
                break;
            }
            t = t.parent;
        }

        // if treeNode is not a parent of new parent simply move
        if(!isParent) {
            IntermediateTreeNode oldParent = treeNode.parent;
            oldParent.children.remove(treeNode);
            newParent.children.add(treeNode);
            treeNode.parent = newParent;
//            System.out.print(": true(not a parent)\n");
            return true;
        // if treeNode is a parent of new parent, AND new parent children are empty, AND new parent is an operator or function
        } else if(newParent.parent == treeNode && newParent.children.isEmpty()
                && (newParent.tokenType.equals("OT") || newParent.tokenType.equals("FT"))) {
            IntermediateTreeNode oldParent = treeNode.parent;
            oldParent.children.add(newParent);
            newParent.parent = oldParent;
            oldParent.children.remove(treeNode);
            treeNode.parent = newParent;
            treeNode.children.remove(newParent);
            newParent.children.add(treeNode);
//            System.out.print(": true(node is THE parent new parent children==0  && (ttype OT or FT)\n");
            return true;
        }
        else if (query.queryType == Parameters.QUERY_TYPE.W_QUESTION) {
            if (newParent.children.isEmpty()
                    && (newParent.tokenType.equals("WPT"))) {
                IntermediateTreeNode oldParent = treeNode.parent;
                oldParent.children.add(newParent);
                newParent.parent = oldParent;
                oldParent.children.remove(treeNode);
                treeNode.parent = newParent;
                treeNode.children.remove(newParent);
                newParent.children.add(treeNode);
//                System.out.print(": true(node is THE parent new parent children==0  && (ttype OT or FT)\n");
                return true;
            } else {
                return  false;
            }
        }
        else {
//            System.out.print(": false(none)\n");
            return false;
        }
    }


    public void addEqualsOperatorNode() {
        // adds equals operator under the root
        IntermediateTreeNode treeNode = new IntermediateTreeNode();
        treeNode.label = "equals";
        treeNode.nodeId = 9999;
        treeNode.tokenType = "OT";
        treeNode.function="=";
        treeNode.parent = allTreeNodes.get(0);
        allTreeNodes.get(0).children.add(treeNode);
        allTreeNodes.add(treeNode);
    }

    public IntermediateTreeNode searchIntermediateNodeByID(int Id) {
        for (IntermediateTreeNode treeNode : allTreeNodes) {
            if(treeNode.nodeId == Id) {
                return treeNode;
            }
        }
        return null;
    }

    public void getTreeHashNumber() {
        LinkedList<IntermediateTreeNode> nodeStack = new LinkedList<>();
        ArrayList<Integer> nodeIDList = new ArrayList<>();
        // preserve the order of the nodes BFS so hash value will remain same idempotent
        nodeStack.add(root);
        nodeStack.add(root);

        while (!nodeStack.isEmpty()) {
            IntermediateTreeNode rNode = nodeStack.removeLast();
            if(!nodeIDList.contains(rNode.nodeId)) {
                for (IntermediateTreeNode rChild: rNode.children) {
                    nodeStack.add(rChild);
                    nodeStack.add(rChild);
                }
                nodeIDList.add(rNode.nodeId);
            }
        }
        hashNum = nodeIDList.hashCode();
    }

    public int compareTo(IntermediateTree tree) {
        if((this.weight*100 - this.cost) > (tree.weight*100 - tree.cost)) {
            return -1;
        } else if ((this.weight*100 - this.cost) < (tree.weight*100 - tree.cost)) {
            return  1;
        }
        return 0;
    }


    public void printIntermediateTree() {
        String result = "";
        result += "HashNum: " + this.hashNum + "" +
                "\n invalid: "+ invalid + "" +
                "\n child_invalid: "+ haveCInvalid + "" +
                "\n weight: " + (double)Math.round(weight*100)/100 +
                "\n cost: " + cost + "\n";

        // print cinvalid
        result+="ChildInv: ";
        for (String s :
                cinv) {
            result+= s+"\t";
        }
        result+="\n";

        LinkedList<IntermediateTreeNode> nodeLinkedList = new LinkedList<>();
        nodeLinkedList.add(root);
        LinkedList<Integer> levelList = new LinkedList<>();
        levelList.add(0);

        while (!nodeLinkedList.isEmpty()) {
            IntermediateTreeNode currentNode = nodeLinkedList.removeLast();
            int currentLevel = levelList.removeLast();
            for (int i = 0; i < currentLevel; i++) {
                result += "\t";
            }

            result += "("+ currentNode.nodeId + ")";
            result += currentNode.label+"- "+ currentLevel +" have Child: "+currentNode.haveChildren.size() + "\n";

            for (int i = 0; i < currentNode.children.size(); i++) {
                nodeLinkedList.add(currentNode.children.get(currentNode.children.size()-i-1));
                levelList.add(currentLevel+1);
            }
        }
        System.out.print(result);
    }
}
