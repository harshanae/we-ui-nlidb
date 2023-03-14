package data_structures;

import database.SchemaGraph;
import database.elements.Edge;
import database.elements.SchemaElement;
import database.util.CommonFunctions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Stack;

public class Block {
    public int blockId;
    public ParseTreeNode blockRoot;
    public Block outerBlock;
    public ArrayList<Block> innerBlocks = new ArrayList<>();

    public ArrayList<ParseTreeNode> allNodes = new ArrayList<>();
    public ArrayList<Edge> edges = new ArrayList<>();
    public ArrayList<SQLElement> selectElements = new ArrayList<>();
    public ArrayList<Object> fromElements = new ArrayList<>();
    public  ArrayList<String> conditions = new ArrayList<>();
    public ArrayList<String> conditionLogic = new ArrayList<>();
    public ArrayList<ParseTreeNode> orderedConditions = new ArrayList<>();
    public Stack<String []> conditionStack = new Stack<>();
    HashMap <Integer, String[]> conditionMap = new HashMap<>();
    public  ArrayList<SQLElement> groupByElements = new ArrayList<>();

    public String SQL = "";

    public Block(int blockId, ParseTreeNode blockRoot) {
        this.blockId = blockId;
        this.blockRoot = blockRoot;
    }

    public void nodeEdgeGen(Block mainBlock, ParseTree queryTree, SchemaGraph graph)
    {
        for(int i = 0; i < this.innerBlocks.size(); i++)
        {
            // Updated 04/06/2018 (cjbaik): prevent recursion
            if (this.innerBlocks.get(i).equals(this)) continue;

            this.innerBlocks.get(i).nodeEdgeGen(mainBlock, queryTree, graph);
        }

        ArrayList<ParseTreeNode> list = new ArrayList<ParseTreeNode>();
        list.add(this.blockRoot);
        while(!list.isEmpty())
        {
            ParseTreeNode node = list.remove(0);
            this.allNodes.add(node);
            list.addAll(node.children);
        }
        for(int i = 0; i < this.innerBlocks.size(); i++)
        {
            this.allNodes.removeAll(this.innerBlocks.get(i).allNodes);
        }

        for(int i = 0; i < allNodes.size(); i++)
        {
            ParseTreeNode node = allNodes.get(i);
            if(!node.mappedSchemaElements.isEmpty())
            {
                SchemaElement left = node.mappedSchemaElements.get(node.choice).schemaElement.relation;
                boolean containsLeft = false;
                for(int j = 0; j < this.fromElements.size(); j++)
                {
                    if(((SchemaElement)this.fromElements.get(j)).elementID == left.elementID)
                    {
                        containsLeft = true;
                        break;
                    }
                }
                if(!containsLeft)
                {
                    this.fromElements.add(left);
                }

                SchemaElement right = null;
                if(!node.parent.mappedSchemaElements.isEmpty())
                {
                    right = node.parent.mappedSchemaElements.get(node.parent.choice).schemaElement.relation;
                }
                else if(node.parent.tokenType.equals("OT") && node.parent.parent != null && !node.parent.parent.mappedSchemaElements.isEmpty())
                {
                    right = node.parent.parent.mappedSchemaElements.get(node.parent.parent.choice).schemaElement.relation;
                }
                if(right != null)
                {
                    edges.addAll(graph.getJoinPath(left, right));
                }
            }
        }
    }

    public void translate(Block mainBlock, ParseTree queryTree, Query query) {
        // not needed
        for (Block innerBlock : innerBlocks) {
             if(innerBlock.equals(this)) continue;

             innerBlock.translate(mainBlock, queryTree, query);
        }

        // SELECT
        if(this.blockRoot.tokenType.equals("NT")) {
            SQLElement sqlElement = new SQLElement(this, this.blockRoot);
            selectElements.add(sqlElement);
        } else if (this.blockRoot.tokenType.equals("FT")) {
            if(blockRoot.children.size() == 1 && this.blockRoot.children.get(0).tokenType.equals("NT")) {
                SQLElement sqlElement = new SQLElement(this, this.blockRoot.children.get(0));
                selectElements.add(sqlElement);
            } else if (this.blockRoot.children.size() == 1 && this.blockRoot.children.get(0).tokenType.equals("FT")){
                if (this.blockRoot.children.get(0).children.size() == 1 && this.blockRoot.children.get(0).children.get(0).tokenType.equals("NT")) {
                    SQLElement sqlElement = new SQLElement(this.innerBlocks.get(0), this.blockRoot.children.get(0).children.get(0));
                    selectElements.add(sqlElement);
                }
            }
        }

        for (Block innerBlock : innerBlocks) {
             if(!this.blockRoot.equals(innerBlock.blockRoot.parent)) {
                 this.selectElements.add(innerBlock.selectElements.get(0));
             }
        }

        if(this.outerBlock != null && this.outerBlock.equals(mainBlock)) {
            ParseTreeNode relatedInnerNode = this.findRelatedNodeFromSelf(mainBlock);
            if(relatedInnerNode != null) {
                SQLElement sqlElement = new SQLElement(this, relatedInnerNode);
                this.selectElements.add(sqlElement);
            }
        }

        for (ParseTreeNode node: allNodes) {
            if(node.QT.equals("each")) {
                SQLElement sqlElement = new SQLElement(this, node);
                selectElements.add(sqlElement);
            }
        }

        if(queryTree.root.children.size() > 1 && queryTree.root.children.get(1).children.size() == 2
            && queryTree.root.children.get(1).children.get(1).function.equals("max")) {
            ParseTreeNode node = queryTree.root.children.get(1).children.get(1).children.get(0);
            SQLElement sqlElement = new SQLElement(this, node);
            selectElements.add(sqlElement);
        }

        // FROM
        for (Edge edge : edges) {
            boolean left = false;
            boolean right = false;
            for (int i = 0; i < fromElements.size(); i++) {
                if(((SchemaElement)this.fromElements.get(i)).elementID == edge.left.relation.elementID) {
                    left = true;
                    break;
                }
            }

            for (int i = 0; i < fromElements.size(); i++) {
                if(((SchemaElement)this.fromElements.get(i)).elementID == edge.right.relation.elementID) {
                    right = true;
                    break;
                }
            }

            if(!left) {
                fromElements.add(edge.left.relation);
            }
            if (!right) {
                fromElements.add(edge.right.relation);
            }
        }
        this.fromElements.addAll(this.innerBlocks);


        //WHERE
        // may not need
        if(this.equals(mainBlock) && queryTree.root.children.size() > 1 && this.innerBlocks.size()>0) {
            for (int i = 1; i < queryTree.root.children.size(); i++) {
                ParseTreeNode complexCondition = queryTree.root.children.get(i);
                ParseTreeNode right = complexCondition.children.get(1);

                String condition = "";
                condition += this.innerBlocks.get(0).selectElements.get(0).toString(this, "");
                condition += " " + complexCondition.function + " ";
                if(this.innerBlocks.size() > 1) {
                    condition += this.innerBlocks.get(1).selectElements.get(0).toString(this, "");
                } else {
                    condition += right.label;
                }

                this.conditions.add(condition);
            }
        }

        for (Edge edge : edges) {
            conditions.add(edge.printEdge());
        }

        conditions = CommonFunctions.removeDuplicates(conditions);

        for (String cond : conditions) {
            conditionLogic.add("AND");
        }
        LinkedList<Integer> conjunctionWordOrder = new LinkedList<Integer>();
        for (String conj : query.conjunctionTable) {
            int firstOperand = Integer.parseInt(conj.split(" ")[0]);
            int secondOperand = Integer.parseInt(conj.split(" ")[1]);
            if(!conjunctionWordOrder.contains(firstOperand)) {
                conjunctionWordOrder.add(firstOperand);
            }
            if(!conjunctionWordOrder.contains(secondOperand)) {
                conjunctionWordOrder.add(secondOperand);
            }
        }


        for (ParseTreeNode currentNode : allNodes) {
             if(!currentNode.tokenType.equals("NT") && !currentNode.mappedSchemaElements.isEmpty()) {
                 String condition = "";
                 condition += currentNode.mappedSchemaElements.get(currentNode.choice).schemaElement.relation.name + "." + currentNode.mappedSchemaElements.get(currentNode.choice).schemaElement.name;
                 if(currentNode.parent.tokenType.equals("OT")) {
                     condition += " " + currentNode.parent.function + " ";
                 } else if (currentNode.mappedSchemaElements.get(currentNode.choice).choice == -1){
                     condition += "LIKE \"%";
                 }else {
                     condition += " = ";
                 }

                 if(currentNode.tokenType.equals("VTNUM")) {
                     condition+= currentNode.label;
                 } else {
                     if (currentNode.mappedSchemaElements.get(currentNode.choice).choice == -1) {
                            condition += currentNode.label + "%\"";
                    } else {
                            condition += "\""+ currentNode.mappedSchemaElements.get(currentNode.choice).mappedValues.get(currentNode.mappedSchemaElements.get(currentNode.choice).choice).value + "\"";
                     }
                 }

                 // print out the condition

                 if(currentNode.isNegated) {
                     System.out.println("NOT "+ condition + " condition logic: "+ currentNode.leftRel);
                     condition = "NOT "+condition;
                 } else {
                     System.out.println(condition + " condition logic: "+ currentNode.leftRel);
                 }


                 if(!currentNode.leftRel.isEmpty()) {
                     if(!currentNode.leftRel.equals(",")) {
                        conditionLogic.add(currentNode.leftRel.toUpperCase());
                        conditionMap.put(currentNode.wordOrder, new String[]{condition, currentNode.leftRel.toUpperCase()});
                     } else {
                        conditionLogic.add("AND");
                        conditionMap.put(currentNode.wordOrder, new String[]{condition, "AND"});
                     }
                 } else {
                     conditionLogic.add("AND");
                     conditionMap.put(currentNode.wordOrder, new String[]{condition, "AND"});
                 }

                 if (!conjunctionWordOrder.contains(currentNode.wordOrder)) {
                     this.conditions.add(condition);
                 }

             }



            if(this.equals(mainBlock))
            {
                for(int i = 0; i < this.innerBlocks.size(); i++)
                {
                    ParseTreeNode innerRelated = this.innerBlocks.get(i).findRelatedNodeFromSelf(mainBlock);
                    if(innerRelated != null && innerBlocks.get(i).allNodes.contains(innerRelated))
                    {
                        SQLElement left = new SQLElement(mainBlock, innerRelated);
                        SQLElement right = new SQLElement(innerBlocks.get(i), innerRelated);
                        String condition = left.toString(mainBlock, "") + " = " + right.toString(mainBlock, innerRelated.mappedSchemaElements.get(innerRelated.choice).schemaElement.name);
                        conditions.add(condition);
                    }
                    else if(innerRelated != null)
                    {
                        SQLElement left = new SQLElement(mainBlock, innerRelated);
                        SQLElement right = new SQLElement(innerBlocks.get(i).innerBlocks.get(0), innerRelated);
                        String condition = left.toString(mainBlock, "") + " = " + right.toString(mainBlock, innerRelated.mappedSchemaElements.get(innerRelated.choice).schemaElement.name);
                        conditions.add(condition);
                    }
                }
            }

//            for (Edge edge : edges) {
//                 conditions.add(edge.printEdge());
//            }

            // GROUP BY
            if(outerBlock != null && outerBlock.equals(mainBlock)) {
                for (ParseTreeNode node : allNodes) {
                    for (ParseTreeNode outerNode : outerBlock.allNodes) {
                        if(node.nodeId == outerNode.nodeId) {
                            SQLElement sqlElement = new SQLElement(this, node);
                            groupByElements.add(sqlElement);
                        }
                    }
                }
            }

            for (ParseTreeNode node : allNodes) {
                if(node.equals("each")) {
                    SQLElement sqlElement = new SQLElement(this, node);
                    groupByElements.add(sqlElement);
                }
            }

        }


//        handleValueConditionOrder(query);

        generateSQL(query);

    }

    public void generateSQL(Query query) {
        this.SQL = "SELECT ";

        if (outerBlock == null) {
            this.SQL += "DISTINCT ";
        }

        for (int i = 0; i < selectElements.size(); i++) {
            if (i!=0) {
                this.SQL += ", ";
            }

            if(selectElements.get(i).block.equals(this) && this.selectElements.get(i).node.parent.tokenType.equals("FT")) {
                this.SQL += this.selectElements.get(i).node.parent.function + "(";
            } else if (selectElements.get(i).block.outerBlock != null && this.selectElements.get(i).block.outerBlock.equals(this)
                && selectElements.get(i).node.parent.parent != null && selectElements.get(i).node.parent.parent.tokenType.equals("FT")) {
                this.SQL += selectElements.get(i).node.parent.parent.function + "(";
            }

            SQL += selectElements.get(i).toString(this, "");

            if(this.selectElements.get(i).block.equals(this) && this.selectElements.get(i).node.parent.tokenType.equals("FT")) {
                this.SQL += ")";
            } else if (this.selectElements.get(i).block.outerBlock != null && this.selectElements.get(i).block.outerBlock.equals(this)
                && this.selectElements.get(i).node.parent.parent != null && this.selectElements.get(i).node.parent.tokenType.equals("FT")) {
                this.SQL += ")";
            }

            if ( i==0 && this.outerBlock != null) {
                this.SQL += " AS " + this.selectElements.get(i).node.parent.label;
            }
        }

        if(this.outerBlock == null) {
            this.SQL += "\n";
        } else {
            this.SQL += " ";
        }

        this.SQL += "FROM ";

        for (int i = 0; i < this.fromElements.size(); i++) {
            if(i!=0) {
                this.SQL += ", ";
                if(this.fromElements.get(i-1).getClass().equals(this.getClass())) {
                    this.SQL += "\n";
                }
            }

            if(this.fromElements.get(i).getClass().equals(this.getClass())) {
                this.SQL += "(";
                this.SQL += ((Block) this.fromElements.get(i)).SQL;
                this.SQL += ") block_";
                this.SQL += ((Block) this.fromElements.get(i)).blockId;
            }
            else {
                this.SQL += ((SchemaElement) this.fromElements.get(i)).name;
            }
        }
        String conjunctionConditions = handleValueConditionOrder(query);
        if(!this.conditions.isEmpty() || !conjunctionConditions.equals("")) {
            if(this.outerBlock == null) {
                this.SQL += "\n";
            } else {
                this.SQL += " ";
            }

            conditions = CommonFunctions.removeDuplicates(conditions);
            // print conditions array
            System.out.print("Conditions: ");
            for (int i = 0; i < conditions.size(); i++) {
                System.out.print(i+": " + conditions.get(i)+"\t");
            }
            System.out.println();

            this.SQL += "WHERE ";
            for (int i = 0; i < this.conditions.size(); i++) {
                if(i!=0) {
                    this.SQL += " AND ";
                }
                this.SQL += conditions.get(i);
            }

            if(conditions.isEmpty()) {
                this.SQL += handleValueConditionOrder(query);
            } else if (!conjunctionConditions.equals("")){
                this.SQL += " AND " + conjunctionConditions;
            }
        }

        if(!this.groupByElements.isEmpty()) {
            if(this.outerBlock == null) {
                this.SQL += "\n";
            } else {
                this.SQL += " ";
            }

            this.SQL += "GROUP BY ";
            for (int i = 0; i < this.groupByElements.size(); i++) {
                if(i!=0) {
                    this.SQL += ", ";
                }
                this.SQL += groupByElements.get(i).toString(this, "");
            }
        }
    }

    public ParseTreeNode findRelatedNodeFromSelf(Block mainBlock) {
        ParseTreeNode outerNT = mainBlock.blockRoot;
        if(mainBlock.blockRoot.equals("FT")) {
            outerNT = mainBlock.blockRoot.children.get(0);
        }

        LinkedList<ParseTreeNode> nodeList = new LinkedList<>();
        nodeList.add(outerNT);
        while(!nodeList.isEmpty()) {
            ParseTreeNode innerNT = nodeList.removeLast();

            if(innerNT.tokenType.equals("NT")) {
                if(innerNT.nodeId == outerNT.nodeId) {
                    return innerNT;
                }
            }
            nodeList.addAll(innerNT.children);
        }

        return null;
    }

    public String handleValueConditionOrder(Query query) {
        if(query.conjunctionTable.isEmpty()) {
            return "";
        }
        LinkedList<Integer> handledValues = new LinkedList<>();
        for (String conj : query.conjunctionTable) {
            int firstOperand = Integer.parseInt(conj.split(" ")[0]);
            int secondOperand = Integer.parseInt(conj.split(" ")[1]);


            String[] firstOpCondition = conditionMap.get(firstOperand);
            String[] secondOpCondition = conditionMap.get(secondOperand);
            if(firstOperand == secondOperand) {
                handledValues.add(firstOperand);
                conditionStack.push(firstOpCondition);
                conditionStack.push(secondOpCondition);
            } else {
                if(!handledValues.contains(firstOperand)) {
                    handledValues.add(firstOperand);
                    conditionStack.push(firstOpCondition);
                }
                if(!handledValues.contains(secondOperand)) {
                    handledValues.add(secondOperand);
                    conditionStack.push(secondOpCondition);
                }
            }

        }

        String cond = " )";

        while(!conditionStack.isEmpty()) {
            String[] operandDetails = conditionStack.pop();
            String op = operandDetails[0];
            if (conditionStack.size() == 0) {
                cond = "( "+op+cond;
            } else {
                String logic = operandDetails[1];
                cond = " "+logic+" "+op+cond;
            }
        }

        System.out.println("Condition test: "+cond);
        return cond;
    }





}
