package data_structures;

import database.elements.MappedSchemaElement;

import java.util.ArrayList;

public class FromContext {

    ArrayList<MappedSchemaElement> relations =  new ArrayList<>();

    public FromContext(ArrayList<MappedSchemaElement> relationsList) {
        this.relations = relationsList;
    }

    @Override
    public String toString() {
        String fromContextString = "FROM ";

        return super.toString();
    }

}
