package database.util;

import database.elements.Edge;
import database.elements.SchemaElement;

import java.util.ArrayList;

public class Dijkstra {
    public int prevElement[][];
    public double shortestDistance[][];

    public static void main(String args[]) {
        double graph[][] = new double[][] { { 0, 0, 1, 2, 0, 0, 0 },
                                            { 0, 0, 2, 0, 0, 3, 0 },
                                            { 1, 2, 0, 1, 3, 0, 0 },
                                            { 2, 0, 1, 0, 0, 0, 1 },
                                            { 0, 0, 3, 0, 0, 2, 0 },
                                            { 0, 3, 0, 0, 2, 0, 1 },
                                            { 0, 0, 0, 1, 0, 1, 0 } };

        double inverse_graph[][] = decrease(graph, 4);

        Dijkstra dijkstra = new Dijkstra();
        dijkstra.computeShortestPath(graph);
        dijkstra.printPrevElement();

//        System.out.println("--------------------------------------------------");
//        // computer shortest path for inverse graph and prev element
//        dijkstra.computeShortestPath(inverse_graph);
//        dijkstra.printPrevElement();

    }

    // function to decrease every matrix element by 3
    public static double[][] decrease(double[][] matrix, double highest) {
        double newMatrix[][] = new double[matrix.length][matrix.length];
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix.length; j++) {
                newMatrix[i][j] = highest - matrix[i][j];
            }
        }
        return  newMatrix;
    }

    public void computeShortestPath(double weights[][]) {
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
            dijkstra(i, weights, weights.length);
        }

    }

    public void dijkstra(int src, double weights[][], int size) {
        // local distance from src for each node
        double[] localDistance = new double[size];

        // assign default weights as shortest distances
        for (int i = 0; i < localDistance.length; i++) {
            localDistance[i] = weights[src][i];
        }

        // print localDistance array
        System.out.println("Local Distance Array");
        for (int i = 0; i < localDistance.length; i++) {
            System.out.print(localDistance[i] + "\t");
        }
        System.out.println();


        // assign prev element as src for each node from src
        for (int i = 0; i < prevElement.length; i++) {
            prevElement[src][i] = src;
        }

        // initialize visited array and assign false to all
        boolean[] visited = new boolean[size];
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

        System.out.println("Local Distance Array after Dijkstra");
        for (int i = 0; i < localDistance.length; i++) {
            System.out.print(localDistance[i] + "\t");
        }
        System.out.println();

        for (int i = 0; i < localDistance.length; i++) {
            shortestDistance[src][i] = localDistance[i];
        }
    }

    // function to print out prevElement matrix
    public void printPrevElement() {
        System.out.println("Prev Element Matrix");
        for (int i = 0; i < prevElement.length; i++) {
            for (int j = 0; j < prevElement.length; j++) {
                System.out.print((prevElement[i][j]+1) + "\t");
            }
            System.out.println();
        }
    }

}
