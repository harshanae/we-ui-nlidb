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
        ArrayList<SchemaElement> attributes = schemaGraph.getSchemaElementsByType("text number double int");

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
        ArrayList<SchemaElement> textAttributes = schemaGraph.getSchemaElementsByType("text");
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
        ArrayList<SchemaElement> numberAttributes = schemaGraph.getSchemaElementsByType("number double int");
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



}
