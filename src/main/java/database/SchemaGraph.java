package database;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import database.elements.Edge;
import database.elements.SchemaElement;


import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class SchemaGraph {

    public static double KeyEdge = 0.99;
    public static double relEdge = 0.995;
    public static double AttEdge = 0.995;
    public ArrayList<SchemaElement> schemaElements =  new ArrayList<SchemaElement>();
    public double  [][] weights;
    public double  [][] shortestDistance;
    public int  [][] prevElement;

    public SchemaGraph(String databaseName) throws FileNotFoundException {
        Gson gson = new Gson();
        JsonArray relations = gson.fromJson(new FileReader("src/source-files/"+databaseName+"Relations.json"),  JsonArray.class);

        System.out.println("Relations Names in "+databaseName+" database.");
        for (int i=0; i < relations.size(); i++) {
            JsonObject relationObj = (JsonObject) relations.get(i);
            SchemaElement relation =  new SchemaElement(schemaElements.size(),
                    relationObj.get("name").getAsString(),
                    relationObj.get("type").getAsString());
            schemaElements.add(relation);
            relation.relation=relation; //relation of the particular relation is the relation itself

            System.out.print("("+relation.name + "\t" + relation.type+")" +"\t");
            if(i!=0 && i%5==0) {
                System.out.println();
            }

            JsonArray attributes = relationObj.getAsJsonArray("attributes");
            for(int j=0;j<attributes.size();j++) {
                JsonObject attributeObj = (JsonObject) attributes.get(j);
                SchemaElement attribute = new SchemaElement(schemaElements.size(),
                        attributeObj.get("name").getAsString(), attributeObj.get("type").getAsString());
                attribute.relation = relation;
                relation.attributes.add(attribute);
                schemaElements.add(attribute);


                if(attributeObj.get("importance") != null) {
                    relation.defaultAttribute = attribute;
                }
                if(attribute.type.equals("pk")) {
                    relation.pk = attribute;
                }

//                if(attributeObj.has("pk")) {
//                    relation.pk = attribute;
//                }
            }
        }

        weights = new double [schemaElements.size()][schemaElements.size()];

        for(int i=0; i<weights.length; i++) {
            for(int j=0; j<weights.length; j++) {
                weights[i][j] =0;
            }
        }

        ArrayList<SchemaElement> rels = this.getSchemaElementsByType("relationship entity");

        for (int i = 0; i < rels.size(); i++) {
            SchemaElement rel = rels.get(i);
            for (int j = 0; j < rel.attributes.size(); j++) {
                weights[rel.elementID][rel.attributes.get(j).elementID]= AttEdge;
            }
        }

        readEdges(databaseName);

        System.out.println("\n....Schema Graph created....");
        System.out.println();
    }

    public void readEdges(String databaseName) throws FileNotFoundException {
        Gson gson = new Gson();
        JsonArray edges = gson.fromJson(new FileReader("src/source-files/"+databaseName+"Edges.json"),  JsonArray.class);

        for (int i = 0; i < edges.size(); i++) {
            JsonObject edgeObj = (JsonObject) edges.get(i);

            String leftRelationName = edgeObj.get("foreignRelation").getAsString();
            String leftAttributeName = edgeObj.get("foreignAttribute").getAsString();
            String rightRelationName = edgeObj.get("primaryRelation").getAsString();

            int foreignKeyAttributeID = this.searchAttribute(leftRelationName, leftAttributeName);
            int primaryRelation = this.searchRelation(rightRelationName);

            if(this.schemaElements.get(foreignKeyAttributeID).relation.type.equals("relationship")) {
                weights[foreignKeyAttributeID][primaryRelation] = relEdge;
            } else {
                weights[foreignKeyAttributeID][primaryRelation] = KeyEdge;
            }

            this.schemaElements.get(primaryRelation).inElements.add(schemaElements.get(foreignKeyAttributeID));
        }
//        System.out.println("Weights Matrix before computing shortest path");
//        for(int i=0; i<weights.length; i++) {
//            for(int j=0; j<weights.length; j++) {
//                System.out.print(weights[i][j] + "\t");
//            }
//            System.out.println();
//        }

        double weightsBef[][] = weights;

        computeShortestPath();

        // print weights matrix
//        System.out.println("Weights Matrix after computing shortest path");
//        for(int i=0; i<weights.length; i++) {
//            for(int j=0; j<weights.length; j++) {
//                System.out.print(weights[i][j] + "\t");
//            }
//            System.out.println();
//        }

    }

    public void computeShortestPath() {
        shortestDistance = new double[weights.length][weights.length];
        prevElement = new int[weights.length][weights.length];

        // assign symmetric weights and assign 0,0 to 0
        for(int i=0; i<weights.length; i++) {
            for(int j=0; j<weights.length; j++) {
               if(weights[i][j] > weights[j][i]) {
                   weights[j][i] = weights[i][j];
               }
            }
            weights[i][i] = 1;
        }

        // assign default weights as shortest distances
        for(int i=0; i<weights.length; i++) {
            for(int j=0; j<weights.length; j++) {
                shortestDistance[i][j] = weights[i][j];
            }
        }

        // compute dijkstra for each node
        for (int i = 0; i < weights.length; i++) {
            dijkstra(i);
        }

    }

    public void dijkstra(int src) {
        // local distance from src for each node
        double[] localDistance = new double[schemaElements.size()];

        // assign default weights as shortest distances
        for (int i = 0; i < localDistance.length; i++) {
            localDistance[i] = weights[src][i];
        }

        // print localDistance array
//        System.out.println("Local Distance Array");
//        for (int i = 0; i < localDistance.length; i++) {
//            System.out.print(localDistance[i] + "\t");
//        }
//        System.out.println();


        // assign prev element as src for each node from src
        for (int i = 0; i < prevElement.length; i++) {
            prevElement[src][i] = src;
        }

        // initialize visited array and assign false to all
        boolean[] visited = new boolean[schemaElements.size()];
        for (int i = 0; i < visited.length; i++) {
            visited[i] = false;
        }

        // mark src as visited
        visited[src] = true;
        boolean allVisited = false;

        // print weights matrix
//        System.out.println("Weights Matrix");
//        for (int i = 0; i < weights.length; i++) {
//            for (int j = 0; j < weights.length; j++) {
//                System.out.print(weights[i][j] + "\t");
//            }
//            System.out.println();
//        }
//        System.out.println();

        while (!allVisited) {
            // initialize max distance and max distance index as 0
            double maxDistance = 0;
            int maxDistanceIndex = -1;


            // find max distance node and index assign it to max distance and max distance index
            for(int i=0; i<weights.length; i++) {
                if(visited[i]==false && (localDistance[i] > maxDistance)) {
                    maxDistance = localDistance[i];
                    maxDistanceIndex = i;
                }
            }
            visited[maxDistanceIndex] = true;

            for(int i=0; i< weights.length; i++) {
                if(visited[i]==false && localDistance[maxDistanceIndex]*weights[maxDistanceIndex][i] > localDistance[i]) {
                    localDistance[i] = localDistance[maxDistanceIndex]*weights[maxDistanceIndex][i];
                    prevElement[src][i] = maxDistanceIndex;
                }
            }

            allVisited = true;

            for (int i = 0; i < visited.length; i++) {
                if(visited[i]==false) {
                    allVisited = false;
                }
            }
        }

//        System.out.println("Local Distance Array after Dijkstra");
//        for (int i = 0; i < localDistance.length; i++) {
//            System.out.print(localDistance[i] + "\t");
//        }
//        System.out.println();

        for (int i = 0; i < localDistance.length; i++) {
            shortestDistance[src][i] = localDistance[i];
        }

    }


    public int searchRelation(String relationName) {
        for (int i = 0; i < schemaElements.size(); i++) {
            SchemaElement rel = schemaElements.get(i);
            if((rel.type.equals("entity") || rel.type.equals("relationship"))
                    && rel.name.equals(relationName)) {
                return i;
            }
        }
        return -1;
    }

    public int searchAttribute(String relationName, String attributeName) {
        for(int i=0; i<schemaElements.size(); i++) {
            SchemaElement rel = schemaElements.get(i);
            if((rel.type.equals("entity") || rel.type.equals("relationship"))
                    && rel.name.equals(relationName)) {
                for (int j = 0; j< rel.attributes.size(); j++) {
                    SchemaElement att = rel.attributes.get(j);
                    if(att.name.equals(attributeName)) {
                        return att.elementID;
                    }
                }
            }
        }
        return  -1;
    }


    public ArrayList<SchemaElement> getSchemaElementsByType(String typeList) {
        String [] types = typeList.split(" ");
        ArrayList<SchemaElement> relatons = new ArrayList<SchemaElement>();
        for(int i=0; i<schemaElements.size(); i++){
            for(int j=0;j<types.length; j++) {
                if(schemaElements.get(i).type.equals(types[j])) {
                    relatons.add(schemaElements.get(i));
                }
            }
        }

        return relatons;
    }


    public static void main(String [] args) throws IOException
    {
        SchemaGraph graph = new SchemaGraph("imdb");
        System.out.println();

    }

    // function to check equality of two matrices
    public boolean checkMatrixEquality(double[][] matrix1, double[][] matrix2) {
        if(matrix1.length != matrix2.length) {
            return false;
        }
        for(int i=0; i<matrix1.length; i++) {
            if(matrix1[i].length != matrix2[i].length) {
                return false;
            }
            for(int j=0; j<matrix1[i].length; j++) {
                if(matrix1[i][j] != matrix2[i][j]) {
                    return false;
                }
            }
        }
        return true;
    }

    public ArrayList<Edge> getJoinPath(SchemaElement relation1, SchemaElement relation2) {
        ArrayList<Edge> joinPath = new ArrayList<Edge>();
        int current = relation2.elementID;
        int prev = relation2.elementID;

        while (schemaElements.get(current).relation.elementID != relation1.elementID) {
            prev = this.prevElement[relation1.elementID][current];
            if(schemaElements.get(current).relation.elementID != schemaElements.get(prev).relation.elementID) {
                joinPath.add(new Edge(schemaElements.get(current), schemaElements.get(prev)));
            }
            current = prev;
        }

        return joinPath;
    }

    public double getDistance(SchemaElement source, SchemaElement destination) {
        return this.shortestDistance[source.elementID][destination.elementID];
    }


    // TODO: add get neighbours?

}
