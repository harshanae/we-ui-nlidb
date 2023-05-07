package database;

import data_structures.ParseTreeNode;
import database.elements.MappedSchemaElement;
import database.elements.SchemaElement;
import org.apache.commons.lang3.math.NumberUtils;

import java.io.FileNotFoundException;
import java.sql.*;
import java.util.ArrayList;

public class Database {
    public SchemaGraph schemaGraph;
    public Connection conn;
    public String name;

    public Database(String host, int port, String user, String password, String dbName) throws FileNotFoundException {
        String url = "jdbc:mysql://" + host + ":" + port + "/" + dbName;
        this.name = dbName;

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            this.conn = DriverManager.getConnection(url, user, password);
            System.out.println("Database connected!");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        schemaGraph = new SchemaGraph(dbName);
    }

    public boolean isSchemaExist(ParseTreeNode treeNode) throws Exception {
        ArrayList<SchemaElement> attributes = schemaGraph.getSchemaElementsByType("text number double int", true);

        for(int i=0; i<attributes.size(); i++) {
            MappedSchemaElement mappedElement = attributes.get(i).isSchemaExist(treeNode.label);
            if(mappedElement!=null) {
                treeNode.mappedSchemaElements.add(mappedElement);
            }
        }
        if(treeNode.mappedSchemaElements.isEmpty()) {
           return false;
        }else {
            return true;
        }
    }

    public boolean isValueExist(ParseTreeNode node) throws SQLException {
        ArrayList<SchemaElement> textAttributes = schemaGraph.getSchemaElementsByType("text varchar char", false);
        for(int i=0; i<textAttributes.size(); i++) {
            MappedSchemaElement textAttribute = textAttributes.get(i).isValueExist(node.label, this.conn);
            if(textAttribute!=null) {
                node.mappedSchemaElements.add(textAttribute);
            }
        }
        if(!node.mappedSchemaElements.isEmpty()) {
            return true;
        }
        return false;
    }

    public boolean isNumberValueExist(ParseTreeNode node, String operator) throws SQLException {
        if(!NumberUtils.isCreatable(node.label)) {
            return false;
        }
        ArrayList<SchemaElement> numberAttributes = schemaGraph.getSchemaElementsByType("number double int", false);
        for(int i=0; i<numberAttributes.size(); i++) {
            MappedSchemaElement numberAttribute = numberAttributes.get(i).isNumberValueExist(node.label, operator, conn);
            if (numberAttribute!=null) {
                node.mappedSchemaElements.add(numberAttribute);
            }
        }
        if(!node.mappedSchemaElements.isEmpty()) {

            return true;
        }
        return false;
    }

    public boolean isProjectedValueExist(ParseTreeNode node) throws SQLException {
        ArrayList<SchemaElement> attributes = schemaGraph.getSchemaElementsByType("text varchar char", false);
        boolean isProjAttributeMapExist = false;
        for(int i=0; i<attributes.size(); i++) {
            if(attributes.get(i).isProjected) {
                MappedSchemaElement attribute = attributes.get(i).isExactValueExist(node.label, this.conn);
                if(attribute!=null) {
                    boolean isElementExist = false;
                    isProjAttributeMapExist = true;
                    for (MappedSchemaElement mse: node.mappedSchemaElements) {
                        if(mse.schemaElement.elementID == attribute.schemaElement.elementID) {
                            isElementExist = true;
                            mse.isRelationMatch = attribute.isRelationMatch;
                            mse.isExactValueExist = attribute.isExactValueExist;
                            mse.similarityScore = attribute.similarityScore;
                            mse.mappedValues = attribute.mappedValues;
                            mse.choice = attribute.choice;
                            mse.noValueExist = attribute.noValueExist;
                            break;
                        }
                    }
                    if(!isElementExist) node.mappedSchemaElements.add(attribute);
                }
            }
        }
        if(!node.mappedSchemaElements.isEmpty()) {
            return true && isProjAttributeMapExist;
        }
        return false;
    }


    public SchemaElement getProjectedAttribute(String relationName, String attName) {
        int schemaElementId = schemaGraph.searchAttribute(relationName, attName);
        for (SchemaElement schemaElement: schemaGraph.schemaElements) {
            if(schemaElement.elementID == schemaElementId) {
                return schemaElement;
            }
        }
        return null;
    }





}
