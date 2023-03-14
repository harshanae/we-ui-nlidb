package database.elements;

import database.util.SimilarityFunctions;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

public class SchemaElement implements Serializable {
    public int elementID = 0;
    public String name = "";
    public String type = ""; // type relation, entity, attribute, fk, pk ...

    public SchemaElement relation; // for attributes

    public ArrayList<SchemaElement> attributes = new ArrayList<SchemaElement>(); // relations and entities

    public SchemaElement pk; // for entities
    public SchemaElement defaultAttribute; // for relations and entities
    public ArrayList<SchemaElement> inElements = new ArrayList<SchemaElement>();

    public SimilarityFunctions similarityFunctions;


    public SchemaElement(int elementID, String name, String type) {
        this.elementID = elementID;
        this.name = name;
        this.type = type;
        similarityFunctions = new SimilarityFunctions(10000);
    }

    public String  printSchemaElement() {
        String result = "";
        result += relation+"."+name+";";
        return result;
    }

    public MappedSchemaElement isSchemaExist(String tag) throws Exception {
        if(this.equals(this.relation.defaultAttribute)) {
            if(similarityFunctions.isSchemaSimilar(this.relation, tag) || similarityFunctions.isSchemaSimilar(this, tag)) {
                MappedSchemaElement mappedSchemaElement = new MappedSchemaElement(this);
                mappedSchemaElement.similarityScore = similarityFunctions.similarity(this.relation, tag);
//                mappedSchemaElement.similarityScore = 1-(1-mappedSchemaElement.similarityScore)*(1-similarityFunctions.similarity(this, tag));
                // logic
                return  mappedSchemaElement;
            }
        } else if (similarityFunctions.isSchemaSimilar(this, tag)) { // if not default attribute
            MappedSchemaElement mappedSchemaElement = new MappedSchemaElement(this);
            mappedSchemaElement.similarityScore = similarityFunctions.similarity(this, tag);
            return  mappedSchemaElement;
        }
        return null;
    }

    public MappedSchemaElement isValueExist(String value, Connection connection) throws SQLException {
        Statement statement = connection.createStatement();
        String noRecSqlStmt = "SELECT * FROM size WHERE size.relation = '"+this.relation.name+"'";
        ResultSet noOfRecordsResult = statement.executeQuery(noRecSqlStmt);
        noOfRecordsResult.next();
        int noOfRecords = noOfRecordsResult.getInt(1);

        String sql = "";
        if(noOfRecords < 2000) {
            sql = "SELECT "+this.name+" FROM "+this.relation.name;
        } else if (noOfRecords >= 2000 && noOfRecords < 100000) {
            sql = "SELECT "+this.name+" FROM "+this.relation.name + " WHERE "+this.name+" LIKE '%"+value + "%' LIMIT 0, 2000";
        } else {
            sql = "SELECT "+this.name+" FROM "+this.relation.name + " WHERE MATCH("+this.name + ") AGAINST ('"+value+"') LIMIT 0, 2000";
        }

        ResultSet result = statement.executeQuery(sql);

        MappedSchemaElement mappedSchemaElement = new MappedSchemaElement(this);

        while(result.next()) {
            MappedValue mappedValue = new MappedValue(result.getString(1));
            mappedSchemaElement.mappedValues.add(mappedValue);
        }

        if(!mappedSchemaElement.mappedValues.isEmpty()) {
            return mappedSchemaElement;
        }
        return  null;
    }

    public MappedSchemaElement isNumberValueExist(String number, String operator, Connection connection) throws SQLException {

        Statement statement = connection.createStatement();
        String query = "SELECT "+this.name+" FROM "+this.relation.name+" WHERE "+this.name+operator+" "+number+ " LIMIT 0, 100";
//        System.out.println(query);
        ResultSet resultSet = statement.executeQuery(query);
        MappedSchemaElement mappedSchemaElement = new MappedSchemaElement(this);

//        System.out.println("SQL results: ");
        while (resultSet.next()) {
            int numberVal = resultSet.getInt(1);
            String numberValue = ""+numberVal;
//            System.out.print(numberVal+", ");
            MappedValue mappedValue = new MappedValue(numberValue);
            mappedSchemaElement.mappedValues.add(mappedValue);
        }
//        System.out.println();

        if(!mappedSchemaElement.mappedValues.isEmpty()) {
            return  mappedSchemaElement;
        }
        return null;
    }
 

}
