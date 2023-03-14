package database.util;

import java.io.*;
import java.net.Socket;

public class Word2Vec {
    public static void main(String args[]) throws Exception {
        Word2Vec word2Vec = new Word2Vec(10000);
        double out = word2Vec.getSimilarity("author", "organization");
        System.out.println("Similarity test for: author, organization = "+out);
    }


    private static int MAX_RETRY =5;
    private int portNo;

    public Word2Vec(int portNo) {
        this.portNo = portNo;
    }

    private double getSimilarity(String word1, String word2, int retries) throws Exception {
        if(word1.equals(word2)) {
            return 1.0;
        }

        if(retries > MAX_RETRY) {
            throw new RuntimeException("Get similarity failed for the inputted words: \n\t<"
                    +word1+", "+word2+">\n After  "+retries+" retries.");
        }

        double similarityScore;

        try {
            Socket socket = new Socket("localhost", this.portNo);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter printWriter = new PrintWriter(socket.getOutputStream(), true);
//            System.out.println("inside try3");
//            System.out.println("inside try4");
            printWriter.println(word1.toLowerCase()+", "+word2.toLowerCase());
//            System.out.println("inside try4");
//            while (in.ready()) {
                similarityScore = Double.parseDouble(in.readLine());
                similarityScore = (similarityScore+1)/2;
//            }
//            System.out.println("inside try5");
            socket.close();
//            DataOutputStream dout = new DataOutputStream(socket.getOutputStream());
//            DataInputStream din = new DataInputStream(socket.getInputStream());
//
//            dout.writeUTF(word1.toLowerCase()+", "+word2.toLowerCase());
//            dout.flush();
//
//            String str = din.readUTF();
//            System.out.println(str);
//
//            dout.close();
//            din.close();
//            socket.close();


            // normalize to 0-1
        } catch (Exception e) {
            System.out.println("Exception: "+e.toString() );
//            throw new Exception(e);
            return this.getSimilarity(word1, word2, retries+1);
        }

        return similarityScore;
    }

    public double getSimilarity(String word1, String word2) throws Exception {
        return this.getSimilarity(word1, word2, 0);
    }
}
