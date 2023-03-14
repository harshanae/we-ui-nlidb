package data_structures;

import java.io.Serializable;

public class EntityPair implements Serializable {
    public ParseTreeNode first;
    public ParseTreeNode second;

    public EntityPair(ParseTreeNode first, ParseTreeNode second) {
        this.first = first;
        this.second = second;
    }

    public boolean checkEnitity( int node1, int node2) {
        if(node1 == first.nodeId && node2 == second.nodeId) {
            return true;
        }
        else if(node1 == second.nodeId && node2 == first.nodeId) {
            return true;
        }
        return false;
    }

    public void print() {
        System.out.println("EntityPair: " + first.label + " and " + second.label);
    }


}