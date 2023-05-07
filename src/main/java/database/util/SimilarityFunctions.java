package database.util;

import data_structures.ParseTreeNode;
import database.elements.MappedSchemaElement;
import database.elements.MappedValue;
import database.elements.SchemaElement;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.*;
import org.apache.commons.lang3.math.NumberUtils;
import settings.Parameters;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Scanner;
import java.util.regex.Pattern;


public class SimilarityFunctions implements Serializable {
    private static Word2Vec word2Vec;

    public static PreprocessPipeline preprocessPipeline;
    private static StanfordCoreNLP pipeline;

    public static Pattern pattern = Pattern.compile("((?<=[a-z])[A-Z]|[A-Z](?=[a-z]))|_", Pattern.DOTALL);

    public SimilarityFunctions(int word2vecPort) {
        this.word2Vec = new Word2Vec(word2vecPort);
        preprocessPipeline = PreprocessPipeline.init_Preprocess_Pipeline();
        pipeline = preprocessPipeline.getPreProcessPipeline();
    }

    public static void main(String args[]) {
        String w1 = "Beautiful";
        String w2 = "Understand";
        String sentence = "";
        Scanner scan = new Scanner(System.in);

        preprocessPipeline = PreprocessPipeline.init_Preprocess_Pipeline();
        pipeline = preprocessPipeline.getPreProcessPipeline();

        while (!sentence.equals("exit")) {
            System.out.println("Enter the query");
            sentence = scan.nextLine();
            // print sentence
            System.out.println("Sentence: " + sentence);

            SimilarityFunctions.nerTest(sentence);

            if (sentence.equals("exit")) {
                break;
            }
        }
//        SimilarityFunctions similarityFunctions = new SimilarityFunctions(10000);
//        similarityFunctions.lemmaTest(w1, w2);

//        String word1 = "citationNum";
//        String word2 = "hello_worlds";
//        String ww = pattern.matcher(word1).replaceAll(" $1").trim();

//        String[] word1Split = word1.split(" |_|((?<=[a-z])[A-Z]|[A-Z](?=[a-z]))");
//        String[] word2Split = word2.split(" |_");

//        System.out.println("split1: "+ww + " split2: "+String.join(" ", word2Split));
    }

    public static double similarity(SchemaElement schemaElement, String word) throws Exception {
        String elementName = schemaElement.name;
        elementName = elementName.toLowerCase();
        word = word.toLowerCase();

        if(elementName.equals(word)) {
            return 1.0;
        }
        elementName = pattern.matcher(elementName).replaceAll(" $1").trim();
        String[] elementNameSplit = elementName.split(" ");
        String[] wordSplit = word.split(" ");

        if(elementNameSplit.length == 1 && wordSplit.length == 1) {
            CoreDocument document1 = pipeline.processToCoreDocument(elementNameSplit[0]);
            CoreDocument document2 = pipeline.processToCoreDocument(wordSplit[0]);
            if(document1.tokens().get(0).lemma().equals(document2.tokens().get(0).lemma())) {
                return 1.0 - Parameters.LEMMA_PENALTY; // reduce some penalty ********************
            }
        }
        DecimalFormat df = new DecimalFormat("#.####");
        Double d = Double.valueOf(df.format(SimilarityFunctions.word2Vec.getSimilarity(elementName, word)));
        return d;
    }

    // test function to test out NER
    public static void nerTest(String x) {
        CoreDocument doc = new CoreDocument(x);
        pipeline.annotate(doc);

        for(CoreEntityMention em: doc.entityMentions()) {
            System.out.println("Entity mention: "+em.text()+"\t"+em.entityType());
        }

        for (CoreLabel token: doc.tokens()) {
            System.out.println("Token word: "+token.word()+ " token word order: "+token.index() + "\t"+ "NER: " + token.get(CoreAnnotations.NamedEntityTagProbsAnnotation.class));
        }

        System.out.println("word: "+doc.tokens().get(0).word()+ " NER: "+doc.tokens());
    }

    //test function to test out Lemma
    public static void lemmaTest(String w1, String w2) {
        CoreDocument document1 = pipeline.processToCoreDocument(w1);
        CoreDocument document2 = pipeline.processToCoreDocument(w2);

        System.out.println("word1: "+document1.tokens().get(0).word()+" Lemma: "+document1.tokens().get(0).lemma());
        System.out.println("word2: "+document2.tokens().get(0).word()+" Lemma: "+document2.tokens().get(0).lemma());

    }

    public static boolean isSchemaSimilar(SchemaElement schemaElement, String word2) throws Exception {
        double similarity = SimilarityFunctions.similarity(schemaElement, word2);
//        System.out.print(schemaElement.name+" "+word2+": =>\t");
//        System.out.println("sim score: "+ similarity);
        if(similarity > Parameters.SIMILARITY_THRESHOLD) {
            return true;
        }else {
            return false;
        }
    }

    public static void getSimilarity(ParseTreeNode node, MappedSchemaElement mappedSchemaElement) {
        if(mappedSchemaElement.similarityScore > 0) {
            return;
        }

        String nodeValue = node.label;
        if(NumberUtils.isCreatable(nodeValue) && (mappedSchemaElement.schemaElement.type.equals("number") ||
                mappedSchemaElement.schemaElement.type.equals("double") || mappedSchemaElement.schemaElement.type.equals("int"))) {
            int sum =0, i = 0;
            for(MappedValue mappedVal: mappedSchemaElement.mappedValues) {
                sum += Double.parseDouble(mappedVal.value);
                i++;
            }
            int size = (int) (Double.parseDouble(nodeValue)*mappedSchemaElement.mappedValues.size());
            mappedSchemaElement.similarityScore = 1-(double)Math.abs(sum-size)/(double)size;
        } else {
            ArrayList<MappedValue> mappedValues = mappedSchemaElement.mappedValues;
            for (int i = 0; i < mappedValues.size(); i++) {
                mappedValues.get(i).similarityScore = LevensteinDistanceDP.getLevenSteinDistance(nodeValue, mappedValues.get(i).value);
            }

            Collections.sort(mappedValues);
            mappedSchemaElement.choice = 0;
            mappedSchemaElement.similarityScore = mappedValues.get(0).similarityScore;
        }
    }

    public static String getLemma(String label) {
        CoreDocument document = pipeline.processToCoreDocument(label);
        if (document.tokens().size()>0) {
            return document.tokens().get(0).lemma();
        } else {
            return label;
        }
    }

    public static String getFullWordLemma(String word) {
        String multiWordLemma = "";

        String [] words = multiWordLemma.split(" ");

        if(words.length==1) {
            return getLemma(word);
        }
        for (String w: words) {
            multiWordLemma += getLemma(w);
            multiWordLemma+=" ";
        }

        return multiWordLemma.trim();
    }


//    int sum =0, i = 0;
//    double [] seq = new double[mappedSchemaElement.mappedValues.size()];
////            System.out.println("Mapped Values: ");
//            for(MappedValue mappedVal: mappedSchemaElement.mappedValues) {
//        sum += Double.parseDouble(mappedVal.value);
////                System.out.print(mappedVal.value);
////                seq[i] = Double.parseDouble(mappedVal.value);
//        i++;
//    }
//    //            System.out.println();
////            double score = EucledianDistance.normalizeScore(Double.parseDouble(nodeValue), seq);
//    int size = (int) (Double.parseDouble(nodeValue)*mappedSchemaElement.mappedValues.size());
//    mappedSchemaElement.similarityScore = 1-(double)Math.abs(sum-size)/(double)size;
////            mappedSchemaElement.similarityScore = score;
}
