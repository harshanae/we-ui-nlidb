package database.elements;

import java.io.Serializable;
import java.util.ArrayList;

public class MappedSchemaElement implements Comparable<MappedSchemaElement>, Serializable {

    public SchemaElement schemaElement;
    public double similarityScore = -1;

    public boolean noValueExist = false;

    public boolean isExactValueExist = false;

//    public ArrayList<String> mappedValues = new ArrayList<String>();

    public boolean isRelationMatch = false;
    public ArrayList<MappedValue> mappedValues = new ArrayList<>();
    public int choice;

    public MappedSchemaElement(SchemaElement schemaElement) {
        this.schemaElement = schemaElement;
    }

    public int compareTo (MappedSchemaElement mappedSchemaElement) {
        if(this.similarityScore > mappedSchemaElement.similarityScore) {
            return -1;
        } else if (mappedSchemaElement.similarityScore > this.similarityScore) {
            return 1;
        }
        return 0;
    }

    public String printMappedSchemaElement() {
        String result = "";
        result += schemaElement.relation.name+"."+schemaElement.name+"("+(double)Math.round(this.similarityScore*100)/100 +")"+":";

        if(mappedValues.size()>0 & choice>=0) {
            for(int i=0; i<mappedValues.size() && i<3; i++) {
                String val = mappedValues.get(i).value;
                if(val.length() > 20) {
                    val = val.substring(0, 20); // if value is longer trim from back
                }
                result += val + "; ";
            }
        }
        return result;
    }


}
