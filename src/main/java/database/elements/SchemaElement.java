package database.elements;

import java.util.ArrayList;

public class SchemaElement {
    public int elementID = 0;
    public String name = "";
    public String type = ""; // type relation, entity, attribute, fk, pk ...

    public SchemaElement relation; // for attributes

    public ArrayList<SchemaElement> attributes = new ArrayList<SchemaElement>(); // relations and entities

    public SchemaElement pk; // for entities
    public SchemaElement defaultAttribute; // for relations and entities
    public ArrayList<SchemaElement> inElements = new ArrayList<SchemaElement>();


    public SchemaElement(int elementID, String name, String type) {
        this.elementID = elementID;
        this.name = name;
        this.type = type;
    }

    public String  printSchemaElement() {
        String result = "";
        result += relation+"."+name+";";
        return result;
    }
 

}
