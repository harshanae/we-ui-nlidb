package database.util;

import edu.stanford.nlp.pipeline.StanfordCoreNLP;

import java.util.Properties;

public class PreprocessPipeline {

    private static PreprocessPipeline pipeline_instance = null;
    private StanfordCoreNLP pipeline;

    private PreprocessPipeline(){
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize, pos, lemma, ner");
        pipeline = new StanfordCoreNLP(props);
    }

    public static PreprocessPipeline init_Preprocess_Pipeline() {
        if(pipeline_instance == null) {
            pipeline_instance = new PreprocessPipeline();
        }
        return pipeline_instance;
    }

    public StanfordCoreNLP getPreProcessPipeline() {
        return  pipeline;
    }


}
