package settings;

public class Parameters {
    public enum QUERY_TYPE {
        DECLARATIVE,
        W_QUESTION,
        H_QUESTION,
        Y_N_QUESTION
    }

    public static double LEMMA_PENALTY = 0.0001;

    public static double EXACT_MATCH = 1 - LEMMA_PENALTY;

    public static double SIMILARITY_THRESHOLD = 0.55;

    public static int CANDIDATE_NUM = 5;

    public static boolean IS_AUTOMATED = true;

    public static  double PROJECTED_PENALTY = 0.05;


}
