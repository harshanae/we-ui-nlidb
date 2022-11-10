package database;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import database.elements.SchemaElement;

import javax.xml.validation.Schema;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Array;
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

        for (int i=0; i < relations.size(); i++) {
            JsonObject relationObj = (JsonObject) relations.get(i);
            SchemaElement relation =  new SchemaElement(schemaElements.size(),
                    relationObj.get("name").getAsString(),
                    relationObj.get("type").getAsString());
            schemaElements.add(relation);
            relation.relation=relation; //relation of the particular relation is the relation itself
            System.out.println(relationObj.get("name").getAsString());

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
            }
        }

        weights = new double [schemaElements.size()][schemaElements.size()];

        for(int i=0; i<weights.length; i++) {
            for(int j=0; j<weights.length; j++) {
                weights[i][j] =0;
            }
        }

        ArrayList<SchemaElement> rels = this.getSchemaElementsByType("relationship entity");
        for(int i=0; i<rels.size(); i++) {
            SchemaElement relation = rels.get(i);
            for(int j=0;j<relation.attributes.size(); j++) {
                weights[relation.elementID][relation.attributes.get(j).elementID] =  AttEdge;
            }
        }

        this.readEdges(databaseName);



    }

    public void readEdges(String databaseName) throws FileNotFoundException {
        Gson gson = new Gson();
        JsonArray edges = gson.fromJson(new FileReader("src/source-files/"+databaseName+"Edges.json"),  JsonArray.class);
        for(int i=0; i<edges.size(); i++) {
            JsonObject edgeObject = (JsonObject) edges.get(i);
            String leftRelationName = edgeObject.get("foreignRelation").getAsString();
            String leftAttributeName = edgeObject.get("foreignAttribute").getAsString();
            String rightRelationName  = edgeObject.get("primaryRelation").getAsString();

            //find the nodes for fk and pk
            int fk = this.searchAttribute(leftRelationName, leftAttributeName);
            int pk = this.searchRelation(rightRelationName);

            if(this.schemaElements.get(fk).relation.type.equals("relationship")) {
                weights[fk][pk] = relEdge;
            } else {
                weights[fk][pk] = KeyEdge;
            }

            this.schemaElements.get(pk).inElements.add(schemaElements.get(fk));

        }

//        computeShortestDistances();
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

    public int searchRelation (String relationName) {
        for(int i=0; i<schemaElements.size(); i++) {
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

    public void computeShortestDistances() {
        shortestDistance = new double[weights.length][weights.length];
        prevElement = new int[weights.length][weights.length];

        for(int i=0; i<weights.length; i++) {
            for(int j=0; j< weights.length;  j++) {
                if (weights[i][j] > weights[j][i]) {
                    // substitute the largest weight among -> and <-
                    weights[j][i] = weights[i][j];
                }
            }
            weights[i][i] = 1;
        }

        for(int i=0; i< weights.length; i++) {
            for(int j=0; j<weights.length; j++) {
                // add default weights as shortest distances
                shortestDistance[i][j] = weights[i][j];
            }
        }

        for(int i=0; i< weights.length; i++) {
            dijkstra(i);
        }
    }

    public void dijkstra(int source) {
        double [] localDistance = new double[schemaElements.size()];

        System.out.println("local distances");
        //assign default local distances from source to every element
        for(int i=0; i<localDistance.length; i++) {
            localDistance[i] = weights[source][i];
            System.out.print(localDistance[i]+"\t");
            if(i!=0 && i%10 ==0) {
                System.out.println("\t\t "+i);
            }
        }

        System.out.println();
        System.out.println("Prev elements [source][i]: source: "+ source);
        //assign prev element as source for all
        for(int i=0; i<prevElement.length; i++) {
            prevElement[source][i] = source;
            System.out.print(prevElement[source][i]+"\t");
            if(i!=0 && i%10 ==0) {
                System.out.println("\t\t "+i);
            }
        }

        // boolean array to denote visited vertices
        boolean [] visited = new boolean[schemaElements.size()];
        for(int i=0; i< visited.length; i++) {
            visited[i] = false;
        }
        visited[source]=true;

        boolean finished = false;

        while(!finished) {
            double maxDistance = 0; // maximum distance vertex from source (out of local distances)
            int maxOrder = -1; // elementId of max vertex

            for(int i=0; i< weights.length; i++) {
                if(visited[i] == false && localDistance[i] > maxDistance) {
                    maxDistance = localDistance[i];
                    maxOrder = i;
                }
            }

            visited[maxOrder]=true;
            for(int i=0; i< weights.length; i++) {
                if(visited[i]==false && localDistance[maxOrder]*weights[maxOrder][i] > localDistance[i]) {
                    localDistance[i] = localDistance[maxOrder]*weights[maxOrder][i];
                    prevElement[source][i] = maxOrder;
                }
            }

            finished=true;
            for(int i=0;i<visited.length; i++) {
                if(visited[i] == false) {
                    finished=false;
                }
            }
        }

        for(int i=0;i<localDistance.length; i++) {
            shortestDistance[source][i] = localDistance[i];
        }

    }


    public void print() {
        for(int i=0; i<schemaElements.size(); i++) {
            System.out.println("" + schemaElements.get(i).elementID + schemaElements.get(i).name);
        }
    }

    public void printWeights() {
        System.out.println("weights");
        for (int i=0; i< weights.length; i++) {
            for (int j = 0; j < weights.length; j++) {
                System.out.print(weights[i][j]);
            }
            System.out.println();
        }
    }

    public void printPrev() {
        System.out.println("prev");
        for (int i=0; i< weights.length; i++) {
            for (int j = 0; j < prevElement.length; j++) {
                System.out.print(prevElement[i][j]);
            }
            System.out.println();
        }
    }

    public void printShortest() {
        System.out.println("Shortest dist");
        for (int i=0; i< shortestDistance.length; i++) {
            for (int j = 0; j < shortestDistance.length; j++) {
                System.out.print(shortestDistance[i][j]);
            }
            System.out.println();
        }
    }
    public static void main(String [] args) throws IOException
    {
        SchemaGraph graph = new SchemaGraph("mas");
        System.out.println();

        graph.print();

        graph.computeShortestDistances();

        graph.printShortest();

    }



}
