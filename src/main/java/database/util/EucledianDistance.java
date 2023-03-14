package database.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;

public class EucledianDistance {

    public static void main(String args[]) {
        double number = 200;
        double[] sequence = { 2079, 2000, 2002, 2005, 2006, 2004, 2854, 6581};

        double score = normalizeScore(number, sequence);
        System.out.println(1-score);
    }

    public static double normalizeScore(double number, double[] sequence) {
        double sequence_mean = 0.0;
        System.out.println("Sequence: ");
        for (double x : sequence) {
            System.out.print(x+", ");
            sequence_mean += x;
        }
        System.out.println();
        double [] distances = new double [sequence.length];
        sequence_mean /= sequence.length;
        int i=0;
        double distance = 0.0;
        for (double x : sequence) {
            double d = Math.pow(x-sequence_mean, 2);
            distances[i] = d;
            distance += d;
            i++;
        }

        double min = Arrays.stream(distances).min().getAsDouble();
        double max = Arrays.stream(distances).max().getAsDouble();
        distance = Math.sqrt(distance);

//        double max_distance = Math.sqrt(sequence.length * Math.pow(100 - 0, 2));

//        double normalized_score = distance / max_distance;

        double normalized_score = ((distance - min) / (max - min));
        System.out.println("sequence mean: "+ sequence_mean+ " distance: "+distance+" max distance: "+ max+" min distance: "+min+ " norm score: "+(1-round(normalized_score, 4)));
        return round(normalized_score, 4);
    }

    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();
//        System.out.print(" val: "+value+": -> ");
        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

}
