package data_structures;

import database.SchemaGraph;
import database.elements.SchemaElement;

import javax.swing.tree.TreeNode;
import java.io.Serializable;
import java.util.ArrayList;

public class IntermediateTreeNode implements Serializable, Comparable<IntermediateTreeNode> {
    public int nodeId;
    public String label;
    public String tokenType = "NA";
    public String function = "NA";
    public IntermediateTreeNode parent;
    public ArrayList<IntermediateTreeNode> children = new ArrayList<IntermediateTreeNode>();

    public SchemaElement mappedElement;

    public boolean hasQuantifier = false;
    public boolean upValid = true;
    public ArrayList<Boolean> haveChildren = new ArrayList<Boolean>();
    public double weight = 0.98;

    public String relationship = "NA";

    // for empty instantiation
    public IntermediateTreeNode() {}

    public IntermediateTreeNode(ParseTreeNode parseTreeNode) {
        this.nodeId = parseTreeNode.nodeId;
        this.label = parseTreeNode.label;
        this.tokenType = parseTreeNode.tokenType;
        this.function = parseTreeNode.function;
        this.relationship = parseTreeNode.relationship;

        if(!parseTreeNode.QT.isEmpty()) {
            hasQuantifier = true;
        }

        if(parseTreeNode.choice >= 0 && (parseTreeNode.mappedSchemaElements.size() > (parseTreeNode.choice-1)) && !parseTreeNode.mappedSchemaElements.isEmpty()) {
            this.mappedElement = parseTreeNode.mappedSchemaElements.get(parseTreeNode.choice).schemaElement;
        } else {
            this.mappedElement = null;
        }
    }

    public void evaluateNode(SchemaGraph schemaGraph, Query query) {
        testNodeValidity();
        computeNodeWeight(schemaGraph, query);
    }

    public void testNodeValidity() {
        // check if node equals ROOT
        if(tokenType.equals("ROOT")) {
            if(parent != null) {
                upValid= false;
            }
            for(int i=0; i<children.size(); i++) {
                // if child i doesnt equal to CMT AND child i doesnt equal to Operator up valid is false
                if(!(children.get(i).tokenType.equals("CMT") || children.get(i).tokenType.equals("WPT")) && !children.get(i).tokenType.equals("OT")) {
                    children.get(i).upValid = false;
                }
            }
        }
        // if node equals CMT
        if(tokenType.equals("CMT") || tokenType.equals("WPT")) {
            int name_value_function_token_num = 0;
            for (IntermediateTreeNode child : children) {
                // if child equals a function that is (not min or max) or child is a node with mapped elements
                if ((child.tokenType.equals("FT") && !child.function.equals("min") && !child.function.equals("max")) || child.mappedElement != null) {
                    name_value_function_token_num++;
                } else {
                    upValid = false;
                }
            }
            if(name_value_function_token_num == 0) {
                haveChildren.add(false);
                // there should be only one child for CMT/WPT
            } else if (name_value_function_token_num > 1) {
                for (int i = 0; i < children.size(); i++) {
                    children.get(i).upValid = false;
                }
            }
            // node token-type equals Function Token
        } else if (tokenType.equals("FT")) {
            // if it's a min or max function
            if(function.equals("max") || function.equals("min")) {
                // if parent is not an operator or =
                if(!parent.tokenType.equals("OT") || !parent.function.equals("=")) {
                    // TODO: can parent be without any operator?
                    upValid = false;
                }
                // set all children up-validity to false
                for (IntermediateTreeNode child : children) {
                    child.upValid = false;
                }
                // if it's function is a sum or avg function or other function: count
            } else {
                // if function is sum or avg
                if(function.equals("sum") || function.equals("avg")) {
                    // if parent is not a operator or not a command upValid is false
                    if(!parent.tokenType.equals("OT") && !(parent.tokenType.equals("CMT") || parent.tokenType.equals("WPT"))) {
                        upValid = false;
                    }
                } else { // other function ie: count
                    // if parent is operator, sum, avg or command up is valid
                    if (parent.tokenType.equals("OT") || (parent.tokenType.equals("CMT") || parent.tokenType.equals("WPT")) || parent.function.equals("sum") || parent.function.equals("avg")) {
                        upValid = true;
                    }  else {
                        upValid = false;
                    }
                }
                // if children size of FUNCTION count, avg, sum is 0
                if(children.size() == 0) {
                    haveChildren.add(false);
                } else if (children.size() == 1) {
                    IntermediateTreeNode child = children.get(0);
                    // child is a value or name node or if child is a function node and function is count
                    if(child.mappedElement != null || child.function.equals("count")) {
                        // TODO: Figure out what to do or why empty
                    } else {
                        child.upValid = false;
                    }
                    // if more than one child all have false up-validity
                } else {
                    for (int i = 0; i < children.size(); i++) {
                        children.get(i).upValid = false;
                    }
                }
            }
            // if node tokentype equals Noun Token
        } else if (tokenType.equals("NT")) {
//            if(children.size() == 0) {
//                if(!parent.tokenType.equals("CMT") && !parent.tokenType.equals("QT") && !this.hasQuantifier == true) {
//                    haveChildren.add(false);
//                }
//            } else {
                for (IntermediateTreeNode child : children) {
                    // if child token type is not operator token AND child mapped element is null
                     if(!child.tokenType.equals("OT") && child.mappedElement == null ){
                         child.upValid = false;
                     }
                }
//            }

//            if(relationship.startsWith("nsubj") && parent.tokenType.equals("NT")) {
//                System.out.println("nsubj detected: "+ label+ "parent: "+parent.label);
//                upValid = false;
//            }
            // if node tokentype equals Value token TEXT or NUM
        } else if (tokenType.equals("VTTEXT") || tokenType.equals("VTNUM")) {
            // all children up valid is false : values leaf wenna oneh
            for (IntermediateTreeNode child : children) {
                child.upValid = false;
            }
            // if parent is not NOUN or parent is not a Operator up is not valid
            if (!parent.tokenType.equals("NT") && !parent.tokenType.equals("OT")) {
                upValid = false;
                haveChildren.add(false);
                haveChildren.add(false);
            }
            // if token type is Operator token
        } else if (tokenType.equals("OT")) {
            // if operator has more than 2 children
           if(children.size() > 2) {
               // mark each child's up as invalid
               for (IntermediateTreeNode child : children) {
                    child.upValid = false;
               }
               // if operator doesn't have children
           } else if (children.size() == 0) {
               haveChildren.add(false);
               haveChildren.add(false);
               haveChildren.add(false);
               // if operator has one child
           } else if (children.size() == 1) {
               IntermediateTreeNode child = children.get(0);
               // if child is a NUMBER VALUE token and child has a mapped element
               if(child.tokenType.equals("VTNUM") && child.mappedElement!=null) {
                   // if parent is not a Noun token up is not valid
                   if(!parent.tokenType.equals("NT")) {
                       upValid = false;
                   }
                   // if child is a value token, noun token, value token or function token
               } else if (child.tokenType.equals("VTNUM") || child.tokenType.equals("NT") || child.tokenType.equals("VTTEXT") || child.tokenType.equals("FT")) {
                   // if parent is not root up is false
                    if (!parent.tokenType.equals("ROOT")) {
                        upValid = false;
                    }
                    haveChildren.add(false);
                    // else
               } else {
                   // if parent is not root or noun token up is false up is false
                   if (!parent.tokenType.equals("ROOT") && !parent.tokenType.equals("NT")) {
                       upValid = false;
                   }
                    haveChildren.add(false);
                    haveChildren.add(false);
                    haveChildren.add(false);
               }
               // if operator has two children
           } else if (children.size() == 2) {
               int leftRight = 0;
               int right = 0;
               // add each value to right and each noun or function(min max only) to leftRight count
               for (IntermediateTreeNode child : children) {
                    if(child.tokenType.equals("VTNUM") || child.tokenType.equals("VTTEXT") || child.function.equals("max") || child.function.equals("min")) {
                        right++;
                    } else if (child.tokenType.equals("NT") || child.tokenType.equals("FT")) {
                        leftRight++;
                    } else {
                        child.upValid = false;
                    }
               }
               if ((leftRight+right) == 0) {
                   // if parent is not root or noun token up is false
                   if(!parent.tokenType.equals("ROOT") && !parent.tokenType.equals("NT")) {
                       upValid = false;
                   }
                   haveChildren.add(false);
                   haveChildren.add(false);
                   haveChildren.add(false);
               } else  if ((leftRight+right) == 1) {
                   // if parent is not root or noun token up is false
                   if(!parent.tokenType.equals("ROOT")) {
                       upValid = false;
                   }
                   haveChildren.add(false);
               } else {
                   // if right is 2 and has values or funcs
                   if(right == 2) {
                       haveChildren.add(false);
                   }
                   // if parent is not root or noun token up is false
                   if (!parent.tokenType.equals("ROOT")) {
                       upValid = false;
                   }
               }
           }
        }
//        printIntermediateNode();
    }

    public void computeNodeWeight(SchemaGraph schemaGraph, Query query) {
        // default weight is 0.98
        weight = 0.98;
        // ONLY computed for nodes with mapped elements
        // if mapped element is null return
        if(mappedElement == null) {
            return;
        }
        // if parent is not null AND parent token type is operator token
        else if (parent != null && parent.tokenType.equals("OT")) {
            // if parent has only one child AND node token is a number value token
            if(parent.children.size()==1 && this.tokenType.equals("VTNUM")) {
                // if parent parent is not null AND parent parent mapped element is not null
                if(parent.parent != null && parent.parent.mappedElement != null) {
                   weight = schemaGraph.getDistance(this.mappedElement, parent.parent.mappedElement);
                   return;
                }
            }
            return;
            // if parent is null AND parent mapped element is null
        } else if (parent == null || parent.mappedElement == null) {
            return;
        }
        // if mapped element is not equal to parent mapped element
        if (!mappedElement.equals(parent.mappedElement)) {
            // compute weight as distance between mapped element and parent mapped element
            weight = schemaGraph.getDistance(mappedElement, parent.mappedElement);
        } else {
            // check if parent and node are entities if so return default
            for (EntityPair entityPair : query.entities) {
                 if(entityPair.checkEnitity(parent.nodeId, nodeId)) {
                     return;
                 }
            }
            // if not same entity weight is 0.95
            weight = 0.95;
        }
    }

    public int compareTo(IntermediateTreeNode node) {
        if (this.nodeId > node.nodeId) {
            return 1;
        } else if (this.nodeId < node.nodeId) {
            return -1;
        } else {
            return 0;
        }
    }

    public void printIntermediateNode() {
        String toPrint = "";
        toPrint += "Node ID: " + nodeId + " \n";
        toPrint += "Label: " + label + " \n";
        toPrint += "Token Type: " + tokenType + " \n";
        toPrint += "Valid: "+ upValid + " \n";
        for (IntermediateTreeNode child : children) {
            toPrint += "\tChild: " + child.nodeId + " \n";
        }
        if(mappedElement != null) {
            toPrint += "Mapped Element: " + mappedElement + " \n";
        }
        toPrint += "Weight: " + weight + " \n";
        System.out.println(toPrint);
    }

}
