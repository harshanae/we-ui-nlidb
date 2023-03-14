package components;

import data_structures.ParseTree;
import data_structures.ParseTreeNode;
import data_structures.Query;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.SentenceUtils;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.trees.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class StanfordNLParser {

//    public static void parse(Query query, LexicalizedParser lexicalizedParser) {
//        StanfordParse(query, lexicalizedParser);
//    }

    public static void StanfordParse(Query query, LexicalizedParser lexicalizedParser) {
        List<CoreLabel> rawWords = SentenceUtils.toCoreLabelList(query.sentence.outputWords); // use of Stanford parser to parse the sentence
        Tree parseTree = lexicalizedParser.apply(rawWords);
        TreebankLanguagePack treebankLanguagePack = new PennTreebankLanguagePack();
        GrammaticalStructureFactory grammaticalStructureFactory = treebankLanguagePack.grammaticalStructureFactory();
        GrammaticalStructure grammaticalStructure = grammaticalStructureFactory.newGrammaticalStructure(parseTree);
        List<TypedDependency> dependencyList = grammaticalStructure.typedDependencies(GrammaticalStructure.Extras.NONE); // do not include any additional edges

        Iterator<Tree> treeIterator = parseTree.iterator();
        ArrayList<String> allNodes = new ArrayList<String>();
        while (treeIterator.hasNext()) {
            allNodes.add(treeIterator.next().nodeString());
        }

        ArrayList<String []> allWords = new ArrayList<String[]>(); // All words in the format of <word, pos>
        String [] word = {"ROOT", "ROOT"};
        allWords.add(word);

        for(int i=0; i<allNodes.size(); i++) {
            if(query.sentence.tokenList.contains(allNodes.get(i))) {  // parse tree that contains terminals as strings and NTs (S, VP...)
                word = new String [2];
                word[0] = allNodes.get(i);
//                System.out.println(allNodes.get(i) + " -> " + allNodes.get(i-1)); // once we get the terminal token we then backtrack for pos
                word[1] = allNodes.get(i-1).split(" ")[0];
                if(word[0].split(" ").length > 2) {
                    word[1] = "NNP";
                }
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
                TypedDependency governorDep = dependencyList.get(currentDep.gov().index());
                if(allWords.get(currentDep.dep().index())[1].startsWith("NN") || allWords.get(currentDep.gov().index())[1].startsWith("NN")
                        || allWords.get(currentDep.dep().index())[1].startsWith("CD") || allWords.get(currentDep.gov().index())[1].startsWith("CD")){
                    // conj => if A and B we get the index of A, B tokens
                    if ((allWords.get(currentDep.dep().index())[1].startsWith("NN") || (allWords.get(currentDep.dep().index())[1].startsWith("CD"))
                            && (allWords.get(currentDep.gov().index())[1].startsWith("NN")) || allWords.get(currentDep.gov().index())[1].startsWith("CD"))) {
                        String conj = "";
                        conj += governorIndex;
                        conj += " ";
                        conj += dependencyIndex;
                        query.conjunctionTable.add(conj);
                    } else if (allWords.get(currentDep.dep().index())[1].startsWith("VB") || allWords.get(currentDep.gov().index())[1].startsWith("VB")) {
                        if (allWords.get(currentDep.dep().index())[1].startsWith("VB")) {
                            String conj = "";
                            String dep2 = "";
                            for (int j=0; j<dependencyList.size(); j++) {
                                TypedDependency dep = dependencyList.get(j);
                                if(dep.gov().index() == Integer.parseInt(dependencyIndex)
                                        && (allWords.get(dep.dep().index())[1].startsWith("NNP") || allWords.get(dep.dep().index())[1].startsWith("CD"))) {
                                    dep2 += dep.dep().index();
                                    break;
                                }
                            }
                            conj += governorIndex;
                            conj += " ";
                            conj += dep2;
                            query.conjunctionTable.add(conj);
                        } else {
                            String conj = "";
                            String gov2 = "";
                            for (int j=0; j<dependencyList.size(); j++) {
                                TypedDependency dep = dependencyList.get(j);
                                if(dep.gov().index() == Integer.parseInt(governorIndex)
                                        && (allWords.get(dep.dep().index())[1].startsWith("NNP") || allWords.get(dep.dep().index())[1].startsWith("CD"))) {
                                    gov2 += dep.dep().index();
                                    break;
                                }
                            }
                            conj += gov2;
                            conj += " ";
                            conj += dependencyIndex;
                            query.conjunctionTable.add(conj);
                        }
                    } else {
                        String conj = "";
                        conj += governorIndex;
                        conj += " ";
                        conj += dependencyIndex;
                        query.conjunctionTable.add(conj);
                    }
                } else if (allWords.get(currentDep.dep().index())[1].startsWith("VB") && allWords.get(currentDep.gov().index())[1].startsWith("VB")) {
                    // conj => if A and B we get the index of A, B tokens
                    String conj = "";
                    String value1 = "";
                    String value2 = "";
//                    if (allWords.get(governorDep.dep().index())[1].startsWith("VB")) {
                        for (int j=0; j<dependencyList.size(); j++) {
                            TypedDependency dep = dependencyList.get(j);
                             if(dep.gov().index() == Integer.parseInt(governorIndex)
                                     && (allWords.get(dep.dep().index())[1].startsWith("NNP") || allWords.get(dep.dep().index())[1].startsWith("CD"))) {
                                 conj += dep.dep().index();
                                 value1 += dep.dep().index();
                                 conj += " ";
                                 break;
                             }
                        }
                        if(value1.isEmpty()) {
                            for (int j=0; j<dependencyList.size(); j++) {
                                TypedDependency dep = dependencyList.get(j);
                                if(dep.gov().index() == Integer.parseInt(governorIndex)
                                        && (allWords.get(dep.dep().index())[1].startsWith("NN") || allWords.get(dep.dep().index())[1].startsWith("CD"))) {
                                    conj += dep.dep().index();
                                    value1 += dep.dep().index();
                                    conj += " ";
                                    break;
                                }
                            }
                        }
                        for (int j=0; j<dependencyList.size(); j++) {
                            TypedDependency dep = dependencyList.get(j);
                            if(dep.gov().index() == Integer.parseInt(dependencyIndex)
                                    && (allWords.get(dep.dep().index())[1].startsWith("NNP") || allWords.get(dep.dep().index())[1].startsWith("CD"))) {
                                conj += dep.dep().index();
                                value2 += dep.dep().index();
                                break;
                            }
                        }
                        if(value2.isEmpty()) {
                            for (int j=0; j<dependencyList.size(); j++) {
                                TypedDependency dep = dependencyList.get(j);
                                if(dep.gov().index() == Integer.parseInt(dependencyIndex)
                                        && (allWords.get(dep.dep().index())[1].startsWith("NN") || allWords.get(dep.dep().index())[1].startsWith("CD"))) {
                                    conj += dep.dep().index();
                                    value2 += dep.dep().index();
                                    break;
                                }
                            }
                        }
                        // produced and written by VALUE
                        if(value1.isEmpty() || value2.isEmpty()) {
                            if(value1.isEmpty()) {
                                conj = value2+" "+value2;
                            } else {
                                conj = value1+" "+value1;
                            }
                        }
//                    }
//                    String conj = "";
//                    conj += governorIndex;
//                    conj += " ";
//                    conj += dependencyIndex;
                    query.conjunctionTable.add(conj);
                }

            }
            // treeTableEntry => dependencyIndex, dependencyValue, POS, governorIndex, dependencyRelation
            String [] treeTableEntry = {dependencyIndex, currentDep.dep().value(), allWords.get(currentDep.dep().index())[1],
                    governorIndex, currentDep.reln().toString(), query.sentence.isQoutedHashMap.get(Integer.parseInt(dependencyIndex)-1).toString()};
            query.treeTable.add(treeTableEntry);
        }
    }

    public static void buildParseTree(Query query) {
        // build the dependency parse tree
        query.parseTree = new ParseTree();
        boolean[] isItemDone = new boolean[query.treeTable.size()]; // is tree table item done

        for(int i=0; i<isItemDone.length; i++) {
            isItemDone[i] = false;
        }

        for(int i=0; i<query.treeTable.size(); i++) { // find the first node and create the root
            if(query.treeTable.get(i)[3].equals("0")) {
                query.parseTree.addNode(query.treeTable.get(i));
                isItemDone[i] = true;
            }
        }

        boolean isBuildFinished = false;

        while (!isBuildFinished) {
            for(int i=0; i<query.treeTable.size(); i++) { // loop to find a undoneItem and add to parseTree
                if(!isItemDone[i]) {
                    if(query.parseTree.addNode(query.treeTable.get(i))) {
                        isItemDone[i] = true;
                        break;
                    }
                }
            }

            for(int i=0; i<isItemDone.length; i++) {
                if(isItemDone[i] == false) {
                    isBuildFinished=false;
                    break;
                } else {
                    isBuildFinished = true;
                }
            }
        }
        // print after building the parse tree
        System.out.println("Parse Tree after building:");
        System.out.println(query.parseTree.parseTreeToString());
    }


    public static void handleConjunctions(Query query) {
        if(query.conjunctionTable.isEmpty()) {
            return;
        }

        for(int i=0; i<query.conjunctionTable.size(); i++) {
            String conjunction = query.conjunctionTable.get(i);
            int governorIndex = Integer.parseInt(conjunction.split(" ")[0]);
            int dependencyIndex = Integer.parseInt(conjunction.split(" ")[1]);

            ParseTreeNode governorNode = query.parseTree.searchNodeByOrder(governorIndex);
            ParseTreeNode dependencyNode = query.parseTree.searchNodeByOrder(dependencyIndex);

            String logic = ",";
            if(dependencyNode.parent.posValue.startsWith("VB")) {
                if(query.parseTree.searchNodeByOrder(dependencyNode.wordOrder-1)!=null) { // get the conjunction word and, but, or which is in between
                    ParseTreeNode n = query.parseTree.searchNodeByOrder(dependencyNode.wordOrder-1);
                    if(n.posValue.startsWith("VB")) {
                        logic = query.parseTree.searchNodeByOrder(dependencyNode.wordOrder-2).label;
                    } else if (n.posValue.startsWith("IN")) {
                        logic = query.parseTree.searchNodeByOrder(dependencyNode.wordOrder-3).label;
                    }
                }
            } else {
                if(query.parseTree.searchNodeByOrder(dependencyNode.wordOrder-1)!=null) { // get the conjunction word and, but, or which is in between
                    ParseTreeNode n = query.parseTree.searchNodeByOrder(dependencyNode.wordOrder-1);
                    if(n.posValue.startsWith("IN")) {
                        logic = query.parseTree.searchNodeByOrder(dependencyNode.wordOrder-2).label;
                    } else {
                        logic = n.label;
                    }
                }
            }

            // add logic to left Rel of a node and consider if there are additional values to be checked connected by , (first consider , and then last)
            if(logic.equalsIgnoreCase("or")) {
                query.conjunctionTable.set(i, query.conjunctionTable.get(i));
                dependencyNode.leftRel = "or";
                for(int j=0; j<governorNode.parent.children.size(); j++) {
                    if(governorNode.parent.children.get(j).leftRel.equals((","))) {
                        governorNode.parent.children.get(j).leftRel = "or";
                    }
                }
            } else if (logic.equalsIgnoreCase("and") || logic.equalsIgnoreCase("but")) {
                query.conjunctionTable.set(i, query.conjunctionTable.get(i));
                dependencyNode.leftRel = "and";
                for(int j=0; j<governorNode.parent.children.size(); j++) {
                    if(governorNode.parent.children.get(j).leftRel.equals((","))) {
                        governorNode.parent.children.get(j).leftRel = "and";
                    }
                }
            } else if (logic.equalsIgnoreCase(",")) {
                dependencyNode.leftRel = ",";
            }
//            System.out.println("gover => "+governorNode.label+" parent => "+governorNode.parent.label);
//            System.out.println("depen => "+dependencyNode.label+" parent => "+dependencyNode.parent.label);

            if(dependencyNode.parent.posValue.startsWith("VB") && governorNode.parent.posValue.startsWith("VB")) {
                dependencyNode.parent.parent = governorNode.parent.parent;
                governorNode.parent.parent.children.add(dependencyNode.parent);
                governorNode.parent.children.remove(dependencyNode.parent);
                dependencyNode.parent.relationship = governorNode.parent.relationship;
            } else {
                dependencyNode.parent = governorNode.parent;
                governorNode.parent.children.add(dependencyNode);
                governorNode.children.remove(dependencyNode);
                dependencyNode.relationship = governorNode.relationship;
            }
//            System.out.println(query.parseTree.searchNodeByOrder(dependencyIndex).parent.label);

//            System.out.println(query.parseTree.parseTreeToString());

//            System.out.println(query.parseTree.searchNodeByOrder(dependencyIndex).parent.label);
//            System.out.println("Parse Tree after conjunctions:");
//            System.out.println(query.parseTree.parseTreeToString());
        }
    }

    public static void initiateParse(Query query, LexicalizedParser lexicalizedParser) {
        StanfordParse(query, lexicalizedParser);
        buildParseTree(query);
        handleConjunctions(query);
    }


}
