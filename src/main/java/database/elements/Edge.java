package database.elements;

public class Edge {
    public SchemaElement right;
    public SchemaElement left;

    public Edge(SchemaElement right, SchemaElement left) {
        this.left = left;
        this.right =right;
    }

    public String printEdge() {
        String result = "";
        if (left.type == "fk") {
            result += left.relation.name + "." + left.name;
        }
        else {
            result += left.relation.name + "." + left.relation.pk.name;
        }

        result += " = ";

        if(right.type.equals("fk")) {
            result += right.relation.name + "." + right.name;
        }
        else {
            result += right.relation.name + "." + right.relation.pk.name;
        }
        return  result;
    }
}
