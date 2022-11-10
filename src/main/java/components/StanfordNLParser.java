package components;

import data_structures.Query;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.SentenceUtils;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.trees.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class StanfordNLParser {

    public static void parse(Query query, LexicalizedParser lexicalizedParser) {
        StanfordParse(query, lexicalizedParser);
    }

    public static void StanfordParse(Query query, LexicalizedParser lexicalizedParser) {
        List<CoreLabel> rawWords = SentenceUtils.toCoreLabelList(query.sentence.outputWords); // use of Stanford parser to parse the sentence
        Tree parse = lexicalizedParser.apply(rawWords);
        TreebankLanguagePack treebankLanguagePack = new PennTreebankLanguagePack();
        GrammaticalStructureFactory grammaticalStructureFactory = treebankLanguagePack.grammaticalStructureFactory();
        GrammaticalStructure grammaticalStructure = grammaticalStructureFactory.newGrammaticalStructure(parse);
        List<TypedDependency> dependencyList = grammaticalStructure.typedDependencies(GrammaticalStructure.Extras.NONE); // do not include any additional edges

        Iterator<Tree> treeIterator = parse.iterator();
        ArrayList<String> allNodes = new ArrayList<String>();
        while (treeIterator.hasNext()) {
            allNodes.add(treeIterator.next().nodeString());
        }

        ArrayList<String []> allWords = new ArrayList<String[]>(); // All words in the format of <word, pos>
        String [] word = {"ROOT", "ROOT"};
        allWords.add(word);

        for(int i=0; i<allNodes.size(); i++) {
            if(query.sentence.tokenList.contains(allNodes.get(i))) {
                word = new String [2];
                word[0] = allNodes.get(i);
                word[1] = allNodes.get(i-1).split(" ")[0];
                allWords.add(word);
            }
        }

        for(int i=0; i<dependencyList.size(); i++) {
            TypedDependency currentDep = dependencyList.get(i);
            String dependencyIndex = "";
            dependencyIndex += currentDep.dep().index();
            String governorIndex = "";
            governorIndex += currentDep.gov().index();

            if(currentDep.reln().toString().startsWith("conj")) {
                String conj = "";
                conj += governorIndex;
                conj += " ";
                conj += dependencyIndex;
                query.conjunctionTable.add(conj);
            }
            // treeTableEntry => dependencyIndex, dependencyValue, POS, governorIndex
            String [] treeTableEntry = {dependencyIndex, currentDep.dep().value(), allWords.get(currentDep.dep().index())[1],
                    governorIndex, currentDep.reln().toString()};
            query.treeTable.add(treeTableEntry);
        }
    }


}
