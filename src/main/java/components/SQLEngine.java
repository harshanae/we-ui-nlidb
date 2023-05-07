package components;

import data_structures.Block;
import data_structures.ParseTree;
import data_structures.ParseTreeNode;
import data_structures.Query;
import database.Database;
import database.util.CommonFunctions;

import java.util.ArrayList;

public class SQLEngine {

    public static void generateSQL(Query query, Database database) {
        preStructureAdjust(query);

        if(query.queryTree.allNodes.size() < 2) {
            System.out.println("Failed to generate SQL (query tree incomplete).");
            return;
        }

        query.blocks = new ArrayList<>();
        blockSplit(query);
        query.blocks.get(0).nodeEdgeGen(query.mainBlock, query.queryTree, query.graph);
        query.blocks.get(0).translate(query.mainBlock, query.queryTree, query);
        query.translatedSQL = query.blocks.get(0).SQL;
        System.out.println(query.translatedSQL);


    }

    public static void preStructureAdjust(Query query) {
        if(query.queryTree.allNodes.get(0) != null && query.queryTree.allNodes.get(0).children.size() > 1) {
            for(int i=1; i<query.queryTree.allNodes.get(0).children.size(); i++) {
                ParseTreeNode OT = query.queryTree.allNodes.get(0).children.get(i);
                if(OT.children.size() == 2) {
                    ParseTreeNode left = OT.children.get(0);
                    ParseTreeNode right = OT.children.get(1);

                    if(right.function.equals("max") || right.function.equals("min")) {
                        if(right.children.size() == 0) {
                            addASubTree(query.queryTree, right, left);
                        }
                    }
                }
            }
        }
    }

    public static void blockSplit(Query query) {
        ParseTree queryTree = query.queryTree;

        ArrayList<ParseTreeNode> nodeList = new ArrayList<>();
        nodeList.add(queryTree.allNodes.get(0));

        while(!nodeList.isEmpty()) {
            ParseTreeNode currentNode= nodeList.remove(nodeList.size()-1);
            Block newBlock = null;
            if(currentNode.parent != null && (currentNode.parent.isQuestionWord())) {
                newBlock = new Block(query.blocks.size(), currentNode);
                query.blocks.add(newBlock);
            } else if (currentNode.tokenType.equals("FT") && !currentNode.function.equals("max")) {
                newBlock = new Block(query.blocks.size(), currentNode);
                query.blocks.add(newBlock);
            }

            for (int i = currentNode.children.size()-1; i>=0 ; i--) {
                nodeList.add(currentNode.children.get(i));
            }
        }
        ArrayList<Block> blocks = query.blocks;

        if(blocks.size() == 0) {
            return;
        }
// Who are the actors born after 1990 and starred in "Avengers"?
        Block mainBlock = blocks.get(0);
        for (Block block : blocks) {
            ParseTreeNode currentBlockRoot = block.blockRoot;
            while (currentBlockRoot.parent != null) {
                if (currentBlockRoot.parent.isQuestionWord()) {
                    mainBlock = block;
                    break;
                }
                currentBlockRoot = currentBlockRoot.parent;
            }
        }

        query.mainBlock = mainBlock;

        for (Block block : query.blocks) {
            if(block.blockRoot.parent.equals("OT")) {
                block.outerBlock = mainBlock;
                mainBlock.innerBlocks.add(block);
            } else if (block.blockRoot.parent.tokenType.equals("FT")) {
                for (Block block2 :
                        blocks) {
                    if(block2.blockRoot.equals(block.blockRoot.parent)) {
                        block.outerBlock = block2;
                        block2.innerBlocks.add(block);
                    }
                }
            }
        }

    }




    public static void addASubTree(ParseTree parseTree, ParseTreeNode newParent, ParseTreeNode child)
    {
        ParseTreeNode added = (ParseTreeNode) CommonFunctions.depthClone(child);
        newParent.children.add(added);
        added.parent = newParent;

        ArrayList<ParseTreeNode> nodeList = new ArrayList<ParseTreeNode>();
        nodeList.add(added);
        while(!nodeList.isEmpty())
        {
            ParseTreeNode curNode = nodeList.remove(0);
            parseTree.allNodes.add(curNode);
            nodeList.addAll(curNode.children);
        }
    }


}
