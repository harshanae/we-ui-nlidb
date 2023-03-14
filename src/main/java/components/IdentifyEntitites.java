package components;

import data_structures.EntityPair;
import data_structures.ParseTreeNode;
import data_structures.Query;
import database.elements.SchemaElement;

import java.util.ArrayList;

public class IdentifyEntitites {

    public static void idEntities(Query query) {
        System.out.println("Identifying Entities");
        ArrayList<ParseTreeNode> queryNodes = query.parseTree.allNodes;

        for (int i = 0; i < queryNodes.size(); i++) {
            ParseTreeNode firstNode = queryNodes.get(i);

            if(firstNode.getNodeChoice() == null) {
                continue;
            }

            SchemaElement firstMap = firstNode.getNodeChoice().schemaElement;

            for (int j=i+1; j<queryNodes.size(); j++) {
                ParseTreeNode secondNode = queryNodes.get(j);

                    if(secondNode.getNodeChoice() == null) {
                        continue;
                    }

                    SchemaElement secondMap = secondNode.getNodeChoice().schemaElement;

                    if(firstMap.equals(secondMap)) {
                        if(firstNode.tokenType.equals("VTTEXT") && secondNode.tokenType.equals("VTTEXT")){
                            if(firstNode.label.equals(secondNode.label)) {
                                EntityPair entityPair = new EntityPair(firstNode, secondNode);
                                query.entities.add(entityPair);
                                // print out the entity pair
                                System.out.println("Entity Pair: "+ firstNode.tokenType + " " + secondNode.tokenType);
                                System.out.println("Entity Pair: " + entityPair.first.label + " " + entityPair.second.label);
                            }
                            else {
                                continue;
                            }
                        }
                    }
                    // user id (NT NT) intuition, Brad actor, actor Brad (VTTEXT NT || NT VTTEXT) intuition
                    if((firstNode.tokenType.equals("VTTEXT") && secondNode.tokenType.equals("NT"))
                       || (firstNode.tokenType.equals("NT") && secondNode.tokenType.equals("VTTEXT"))
                    || (firstNode.tokenType.equals("NT") && secondNode.tokenType.equals("NT"))) {
                        if(Math.abs(firstNode.wordOrder - secondNode.wordOrder) > 2) {
                            continue;
                        }
                        else {
                            EntityPair entityPair = new EntityPair(firstNode, secondNode);
                            System.out.println("Entity Pair: "+ firstNode.tokenType + " " + secondNode.tokenType);
                            System.out.println("Entity Pair: " + entityPair.first.label + " " + entityPair.second.label);
                            query.entities.add(entityPair);
                        }
                    }
            }


        }

    }
}
