package components;

import data_structures.*;
import database.Database;
import database.SchemaGraph;
import database.util.CommonFunctions;

import java.util.*;

public class IntermediateTreeHandler {

    public static void handleIntermediateTree(Query query, Database db) {
        // initialize candidate trees
        query.candidateTrees = new ArrayList<>();
        // keep tree hashNum and cost
        Hashtable<Integer, Integer> initialTrees = new Hashtable<>();

        //print query parseTree
        System.out.println("Query Parse Tree:");
        System.out.println(query.parseTree.parseTreeToString());
        System.out.println();

        handle(query, db, false, initialTrees);
        // candidate tree is empty or the first tree has cost > 3
        if(query.candidateTrees.size() == 0 || (query.candidateTrees.size() > 0 && query.candidateTrees.get(0).cost > 3)) {
            boolean maxMin = false;

            // if max or min function exits within the pare tree
            for (ParseTreeNode parseTreeNode : query.parseTree.allNodes) {
                if (parseTreeNode.function.equals("max") || parseTreeNode.function.equals("min")) {
                    maxMin = true;
                }
            }

            // if max min exists handle the tree with equal node true
            if(maxMin) {
                handle(query, db, true, initialTrees);
            }
        }

        // make the candidate trees as finalized trees
        ArrayList<IntermediateTree> finalizedTrees = query.candidateTrees;

        // sort the finalized trees according to the cost
        Collections.sort(finalizedTrees);

        // sort the children of each node in the tree for each node  RECALCULATE HASHNUM
        for (IntermediateTree tree : finalizedTrees) {
            for (IntermediateTreeNode treeNode : tree.allTreeNodes) {
                Collections.sort(treeNode.children);
            }
            tree.getTreeHashNumber();
        }

        // remove duplicate trees
        LinkedList<Integer> list = new LinkedList<>();
        for (int i=0; i<finalizedTrees.size(); i++) {
            IntermediateTree finalizedTree = finalizedTrees.get(i);
            if (list.contains(finalizedTree.hashNum)) {
                finalizedTrees.remove(finalizedTree);
                i--;
            } else {
                list.add(finalizedTree.hashNum);
            }
        }

        // build
        buildAdjustedTrees(query);
    }

    public static void preHandle(IntermediateTree tree, Query query) {
        for (IntermediateTreeNode treeNode : tree.allTreeNodes) {
            // move up functions and operators if they don't have any children
            if(treeNode.function.equals("avg") || treeNode.function.equals("sum")) {
                if(treeNode.children.isEmpty()) {
                    tree.moveSubtree(treeNode, treeNode.parent, query);
                }
            } else if (treeNode.tokenType.equals("OT") && treeNode.children.isEmpty()) {
                tree.moveSubtree(treeNode, treeNode.parent, query);
            }
        }
    }

    public static void handle(Query query, Database database, boolean addEqualNode, Hashtable<Integer, Integer> initialTrees) {
        ParseTree parseTree = query.parseTree;
        ArrayList<IntermediateTree> handledTrees = query.candidateTrees;
        query.NLSentences = new ArrayList<>();

        IntermediateTree intermediateTree = new IntermediateTree(parseTree);
        preHandle(intermediateTree, query);

        if(addEqualNode) {
            intermediateTree.addEqualsOperatorNode();
        }

        // initialize weights for all nodes and evaluate them  check for invalid nodes (upValidity -> invalid++) and get tree weight by multiplying each node's weight
        intermediateTree.evaluateTree(database.schemaGraph, query);
//        System.out.println("After initial eval Intermediate tree");
//        intermediateTree.printIntermediateTree();

        // if no invalid nodes finish handling
        if(intermediateTree.invalid == 0) {
            handledTrees.add(intermediateTree);
        }

        // add tree to queue
        ArrayList<IntermediateTree> treeQueue = new ArrayList<>();
        treeQueue.add(intermediateTree);

        // add tree to initialTrees hash table with its cost
        initialTrees.put(intermediateTree.hashNum, intermediateTree.cost);
        int k = 0;
        // while queue is not empty and size is less than 100
        while (!treeQueue.isEmpty() && treeQueue.size() < 100) {
            // remove first tree from queue
            IntermediateTree currentTree = treeQueue.remove(0);

            System.out.println(k+" entry: "+ currentTree.invalid);
            k++;
            // get extended trees
            ArrayList<IntermediateTree> extendedTrees = extendTree(currentTree, database.schemaGraph, query);

            // add cost and
            for (int i = 0; i < extendedTrees.size(); i++) {
                IntermediateTree treeToAdd = extendedTrees.get(i);
                if(initialTrees.containsKey(treeToAdd.hashNum)) {
                    // check and if only the cost is less update the cost
                    if(initialTrees.get(treeToAdd.hashNum) > treeToAdd.cost) {
                        initialTrees.remove(treeToAdd.hashNum);
                        initialTrees.put(treeToAdd.hashNum, treeToAdd.cost);
                    }
                }
                // if tree is not in initial trees add it to the queue and add to initial trees
                else {
                    treeQueue.add(treeToAdd);
                    initialTrees.put(treeToAdd.hashNum, treeToAdd.cost);
                    if(treeToAdd.invalid == 0) {
                        handledTrees.add(treeToAdd);
                    }
                }
            }
        }
    }

    public static ArrayList<IntermediateTree> extendTree(IntermediateTree tree, SchemaGraph schemaGraph, Query query) {
        ArrayList<IntermediateTree> extendedTrees = new ArrayList<>();
        // if tree cost > 4  return empty list
        if(tree.cost > 4) {
            return extendedTrees;
        }

        // for each node try and extend tree
        for ( int i=1; i < tree.allTreeNodes.size(); i++) {
            IntermediateTreeNode treeNode = tree.allTreeNodes.get(i);
            extendedTrees.addAll(extendNode(tree, treeNode, schemaGraph, query));
        }

        // for each extended trees calculate the hashNumber
        for (IntermediateTree exTree : extendedTrees) {
            exTree.getTreeHashNumber();
        }

        return extendedTrees;
    }

    public static ArrayList<IntermediateTree> extendNode(IntermediateTree intermediateTree, IntermediateTreeNode treeNode, SchemaGraph schemaGraph, Query query) {
        ArrayList<IntermediateTree> extendedTrees = new ArrayList<>();
        // consider all nodes of the intermediate tree
        for (int i=0; i<intermediateTree.allTreeNodes.size(); i++) {
            // clone the tree
            IntermediateTree newTree = (IntermediateTree) CommonFunctions.depthClone(intermediateTree);
//            System.out.println("new Tree for "+ treeNode.label );
//            newTree.printIntermediateTree();
//            System.out.println("intermediate tree invalid: "+intermediateTree.invalid + "new Tree invalid: "+ newTree.invalid);
            // each time increment the cost of the newly created tree
            newTree.cost++;

            // if nodes are not equal try and add the node as a subtree under the ith node of the new tree
            if(newTree.allTreeNodes.get(i).nodeId != treeNode.nodeId) {
                boolean ifAdded = newTree.moveSubtree(newTree.allTreeNodes.get(i), newTree.searchIntermediateNodeByID(treeNode.nodeId), query);
//                System.out.println("If Added: "+ifAdded);
//                newTree.printIntermediateTree();
                // if add successful evaluate the tree and
                // add it to the list of extended trees if the invalid nodes is lower and if equal consider wight and cost
                if(ifAdded) {
                    newTree.evaluateTree(schemaGraph, query);
                    if(newTree.invalid < intermediateTree.invalid ||
                            ((newTree.invalid == intermediateTree.invalid) &&
                                    ((newTree.weight*1000 - newTree.cost) > (intermediateTree.weight*1000 - intermediateTree.cost)))) {
                        extendedTrees.add(newTree);
                    }
                }
            }
        }
        return extendedTrees;
    }

    public static void buildAdjustedTrees(Query query) {
        // get trees that need to be adjusted
        ArrayList<IntermediateTree> adjustingTrees = query.candidateTrees;

        // initialize adjusted trees <PARSE TREES>
        query.adjustedTrees = new ArrayList<>();
        ArrayList<ParseTree> adjustedTrees = query.adjustedTrees;

        // for each tree that needs to be adjusted
        for (IntermediateTree adjustingTree : adjustingTrees) {
            // clone the parse tee
            ParseTree adjustedTree = (ParseTree) CommonFunctions.depthClone(query.parseTree);
            // consider all the nodes within the selected candidate tree
            for (IntermediateTreeNode missingNode : adjustingTree.allTreeNodes) {
                // if the node is not in the cloned parse tree add it
                if (adjustedTree.searchNodeByID(missingNode.nodeId) == null) {
                    ParseTreeNode newNode = new ParseTreeNode(missingNode.nodeId, missingNode.label, "", "", "false", null);
                    newNode.tokenType = missingNode.tokenType;
                    newNode.nodeId = missingNode.nodeId;
                    newNode.function = missingNode.function;
                    adjustedTree.allNodes.add(newNode);
                }
            }

            // for each node in the adjusted tree set initialize children array list
            for (ParseTreeNode parseTreeNode : adjustedTree.allNodes) {
                parseTreeNode.children = new ArrayList<>();
            }

            // for each node in the adjusted tree set parent and children
            for (IntermediateTreeNode currIntNode : adjustingTree.allTreeNodes) {
                ParseTreeNode currParseTreeNode = adjustedTree.searchNodeByID(currIntNode.nodeId);

                if(!currIntNode.label.contains("ROOT")) {
                    currParseTreeNode.parent = adjustedTree.searchNodeByID(currIntNode.parent.nodeId);
                }

                for (IntermediateTreeNode currIntNodeChild : currIntNode.children) {
                    currParseTreeNode.children.add(adjustedTree.searchNodeByID(currIntNodeChild.nodeId));
                }
            }
            // print adjusted tree
            System.out.println("Adjusted Tree");
            System.out.println(adjustedTree.parseTreeToString());
            System.out.println();

            adjustedTrees.add(adjustedTree);
        }

        // if adjusted trees are not empty set the query tree to the first tree
        if(query.adjustedTrees.size()>0) {
            query.queryTreeID = 0;
            query.queryTree = adjustedTrees.get(query.queryTreeID);
        }

        // for each node in query tree
        for (ParseTreeNode operatorNode : query.queryTree.allNodes) {
            // if there exist a OT and its children == 2
            if(operatorNode.tokenType.equals("OT") && operatorNode.children.size() == 2 ) {
                ParseTreeNode left = operatorNode.children.get(0);
                ParseTreeNode right = operatorNode.children.get(1);
                // get the values for the RHS
                if(left.tokenType.equals("VTNUM")) {
                    operatorNode.children.set(1, left);
                    operatorNode.children.set(2, right);
                }
            }
        }
        System.out.println("QUERY TREEEEEE");
        System.out.println(query.queryTree.parseTreeToString());
        // print each deleted nodes of the parse tree
        for (ParseTreeNode deletedNode : query.parseTree.deletedNodes) {
            System.out.println("Deleted Node: " + deletedNode.nodeId + " " + deletedNode.label);
        }
    }

}
