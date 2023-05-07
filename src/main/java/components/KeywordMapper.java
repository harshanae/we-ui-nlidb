package components;

import data_structures.ParseTree;
import data_structures.ParseTreeNode;
import data_structures.Query;
import database.Database;
import database.elements.MappedSchemaElement;
import database.elements.MappedValue;
import database.util.CommonFunctions;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.CoreEntityMention;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import org.apache.commons.lang3.math.NumberUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import settings.Parameters;
import database.util.PreprocessPipeline;
import database.util.SimilarityFunctions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Stack;

public class KeywordMapper {
    private static Stack<ParseTreeNode> phraseCombDeletedStack = new Stack<>();
    public static void mapKeywords(Query query, Database db, Document tokens) throws Exception {
        System.out.println("Starting Keyword Mapping...");
//        identifyValuesNER(query);
        identifyKeyTokens(query, db, tokens);
//        considerValueVerbs(query);
//        considerVerbNTMentions(query);
        handleOperatorConjunctions(query);
        deleteUselessNodes(query);
        mapDBElements(query, db);
        handleNegations(query);
        deleteNoMatch(query);
        deleteSecondaryTokens(query);
        rankMappedElements(query);
        rankAsGroup(query, db);
        printMappedSchemaElements(query);
    }



    public static void identifyKeyTokens(Query query, Database db, Document tokens) {
        ParseTree queryParseTree = query.parseTree;
        queryParseTree.root.tokenType = "ROOT"; // make the root of the parse tree
        for(int i=0; i<queryParseTree.root.children.size(); i++) {
            ParseTreeNode rootChild = queryParseTree.root.children.get(i);
            if(checkType(queryParseTree, rootChild, "CMT_V", null, tokens)) {
                System.out.println("Word: "+rootChild.label+" => identified Token: Command (CMT_V)\n "+rootChild.label+" -assigned-> CMT");
                System.out.println();
                rootChild.tokenType = "CMT";
                if(rootChild.wordOrder == 1) {
                    query.queryType = Parameters.QUERY_TYPE.DECLARATIVE;
                }
            }
        }
        System.out.println(queryParseTree.allNodes.size());
        for (int i=0; i<queryParseTree.allNodes.size(); i++) {
            // add wh logics
            ParseTreeNode node = queryParseTree.allNodes.get(i);
            if(checkType(queryParseTree, node, "W_V", null, tokens)) {
                if(node.wordOrder == 1) {
                    node.tokenType = "WPT";
                    System.out.println("Word: "+node.label+" => identified Token: Command (W_V)\n "+node.label+" -assigned-> WPT");
                    System.out.println();
                    query.queryType = Parameters.QUERY_TYPE.W_QUESTION;
                } else if (node.wordOrder > 1) {
                    node.tokenType = "WST";
                    System.out.println("Word: "+node.label+" => identified Token: Command (W_V)\n "+node.label+" -assigned-> WST");
                    System.out.println();
                }
            }

            if(checkType(queryParseTree, node, "H_V", null, tokens)) {
                if(node.wordOrder == 1) {
                    System.out.println("Word: "+node.label+" => identified Token: Command (H_V)\n "+node.label+" -assigned-> HPT");
                    System.out.println();
                    node.tokenType = "HPT";
                    query.queryType = Parameters.QUERY_TYPE.H_QUESTION;
                } else if (node.wordOrder > 1) {
                    node.tokenType = "HST";
                    System.out.println("Word: "+node.label+" => identified Token: Command (H_V)\n "+node.label+" -assigned-> HST");
                    System.out.println();
                }
            }
        }

        // find put negated tokens
        for(int i=0; i<queryParseTree.allNodes.size(); i++) {
            ParseTreeNode currentNode = queryParseTree.allNodes.get(i);
            if(currentNode.tokenType.equals("NA") && checkType(queryParseTree, currentNode, "NEG", null, tokens)) { // search for negation tokens
                System.out.println("Word: "+currentNode.label+" => identified Token: NEGATION (NEG)\n "+currentNode.label+" -assigned-> NEG");
                System.out.println();
                currentNode.tokenType = "NEG";
            }
        }

        for(int i=0; i<queryParseTree.allNodes.size(); i++) {  // merge multi word expressions into a single node (more than)
            ParseTreeNode currentNode = queryParseTree.allNodes.get(i);
            if(currentNode.tokenType.equals("NA") &&
                    ((currentNode.relationship.equals("flat")) || currentNode.relationship.equals("fixed") || currentNode.relationship.equals("compound"))) { // maybe add other multi-word expressions compound and fixed also??
//                System.out.println("MWE pos of curr: "+currentNode.label+" -> "+currentNode.posValue+" pos of parent: "+ currentNode.parent.posValue);
                // logic
                boolean isCombine = true;
                if(currentNode.posValue.startsWith("NNP") && (!currentNode.parent.posValue.startsWith("NNP") && currentNode.parent.posValue.startsWith("NN")) ||
                        currentNode.parent.posValue.startsWith("NNP") && (!currentNode.posValue.startsWith("NNP") && currentNode.posValue.startsWith("NN"))) {
                    if(currentNode.posValue.startsWith("NNP")) {
                        currentNode.valuePredicate = currentNode.parent;
                    }
                    isCombine = false;
                }

                if (currentNode.isQuoted || currentNode.parent.isQuoted) {
                    isCombine = false;
                }

                if (isCombine){
                    if (currentNode.wordOrder > currentNode.parent.wordOrder) {
                        System.out.println("Word: " + currentNode.label + " => identified Token: MWE (" + currentNode.relationship + ")\nNode Label(parent): " + currentNode.parent.label + " -assigned->" + currentNode.parent.label + " " + currentNode.label);
                        System.out.println();
                        currentNode.parent.label = currentNode.parent.label + " " + currentNode.label;
                    } else {
                        System.out.println("Word: " + currentNode.label + " => identified Token: MWE (" + currentNode.relationship + ")\nNode Label(parent): " + currentNode.parent.label + " -assigned->" + currentNode.label + " " + currentNode.parent.label);
                        System.out.println();
                        currentNode.parent.label = currentNode.label + " " + currentNode.parent.label;
                    }

                    queryParseTree.deleteNode(currentNode);
                    i--;
                }

            }
        }

//What are the movies that have budgets greater than or equal to 50000
        for(int i=0; i<queryParseTree.allNodes.size(); i++) {
            KeywordMapper.phraseCombDeletedStack = new Stack<>();
            ParseTreeNode currentNode= queryParseTree.allNodes.get(i);
            // function token FT => Adjective
            if(currentNode.tokenType.equals("NA") && checkType(queryParseTree, currentNode, "FT", "function", tokens )) {
                System.out.println("Word: "+currentNode.label+" => identified Token: Function (FT)\n "+currentNode.label+" -assigned-> FT tokenType");
                System.out.println();
                currentNode.tokenType = "FT";
            } else if (currentNode.tokenType.equals("NA") && checkType(queryParseTree, currentNode, "OT", "operator", tokens )) {
                // Operator token => Adjective
                if((currentNode.label.equalsIgnoreCase("from") || currentNode.label.equalsIgnoreCase("since") || currentNode.label.equalsIgnoreCase("until"))
                        && !currentNode.parent.posValue.equals("CD")) {
                    currentNode.tokenType = "NA";
                    continue;
                }
                System.out.println("Word: "+currentNode.label+" => identified Token: Operator (OT)\n "+currentNode.label+" -assigned-> OT tokenType ("+currentNode.function+")");
                System.out.println();
                currentNode.tokenType = "OT";

            } else if (currentNode.tokenType.equals("NA") && checkType(queryParseTree, currentNode, "OBT", null, tokens )) {
                // Order by token
                System.out.println("Word: "+currentNode.label+" => identified Token: Order by (OBT)\n "+currentNode.label+" -assigned-> OBT tokenType");
                System.out.println();
                currentNode.tokenType = "OBT";
            } else if (NumberUtils.isCreatable(currentNode.label)) { // check is numeric
                System.out.println("Word: "+currentNode.label+" => identified Token: Number (VT)\n "+currentNode.label+" -assigned-> VT tokenType");
                System.out.println();
                currentNode.tokenType = "VT";
            } else if (currentNode.tokenType.equals("NA") &&  (currentNode.posValue.startsWith("NN") || currentNode.posValue.equals("CD"))) { //Noun or cardinal digit

                if(currentNode.posValue.startsWith("NNP") || currentNode.posValue.startsWith("CD")) {
                    System.out.println("Word: "+currentNode.label+" => identified Token: Proper Noun (VT)\n "+currentNode.label+" -assigned-> VT tokenType");
                    System.out.println();
                    currentNode.tokenType = "VT";
                } else {
                    System.out.println("Word: "+currentNode.label+" => identified Token: Noun (NT)\n "+currentNode.label+" -assigned-> NT tokenType");
                    System.out.println();
                    if (query.queryType != Parameters.QUERY_TYPE.DECLARATIVE) {
                        if((currentNode.relationship.startsWith("obl") || (currentNode.relationship.startsWith("obj") && currentNode.parent.posValue.startsWith("VB") && currentNode.parent.tokenType.equals("NA")))
                                && !CommonFunctions.stopwords.contains(currentNode.parent.label.toLowerCase())
                                && (!currentNode.isQuoted && !currentNode.parent.isQuoted)) {
//                            currentNode.parent.label = currentNode.parent.label + " " + currentNode.label;
                            currentNode.tokenType = "NTVT";
//                            System.out.println("Verb combination[obl obj]: "+currentNode.parent.label+" "+ currentNode.label);
//                            queryParseTree.deleteNode(currentNode);
//                            i--;
                        } else if (currentNode.relationship.startsWith("obl")  && currentNode.parent.tokenType.equals("NA")
                                && CommonFunctions.stopwords.contains(currentNode.parent.label.toLowerCase())
                                && (!currentNode.isQuoted && !currentNode.parent.isQuoted)) {
//                            currentNode.parent.label = currentNode.label;
                            currentNode.tokenType = "NTVT";
//                            System.out.println("Verb combination[obl obj]: "+currentNode.parent.label+" "+ currentNode.label);
//                            queryParseTree.deleteNode(currentNode);
//                            i--;
                        } else {
                            currentNode.tokenType = "NTVT";
                        }
                    } else {
                        currentNode.tokenType = "NTVT";
                    }
                }
            } else if (currentNode.tokenType.equals("NA") && currentNode.posValue.startsWith("JJ")) {
                System.out.println("Word: "+currentNode.label+" => identified Token: Adjective (JJ)\n "+currentNode.label+" -assigned-> JJ tokenType");
                System.out.println();
                if(!query.parseTree.searchNodeByOrder(currentNode.wordOrder+1).posValue.startsWith("NNP") && query.parseTree.searchNodeByOrder(currentNode.wordOrder+1).posValue.startsWith("NN")) {
                    currentNode.tokenType = "VT";
                }
            } else if (currentNode.tokenType.equals("NA") && checkType(queryParseTree, currentNode, "QT", "quantity", tokens)) {
                System.out.println("Word: "+currentNode.label+" => identified Token: Qualifier (QT)\n "+currentNode.label+" -assigned-> QT tokenType");
                // Qualifier token
                System.out.println();
                currentNode.tokenType = "QT";
            }

            while(!phraseCombDeletedStack.isEmpty()) {
                i--;
                phraseCombDeletedStack.pop();
            }
        }
    }



    public static boolean checkType(ParseTree parseTree, ParseTreeNode parseTreeNode, String token,  String tag, Document tokenTypes) {
        String label ="";
         // case logic  goes here

        label = parseTreeNode.label.toLowerCase();
        Element tokenElement = (Element) (tokenTypes.getElementsByTagName(token)).item(0); // find token type
        NodeList phraseList = tokenElement.getElementsByTagName("phrase"); // get possible phrases for a particular token => CMT_V -> return tell select give

        for(int i=0; i<phraseList.getLength(); i++) {
            String phraseText = phraseList.item(i).getFirstChild().getNodeValue().trim();
            if(phraseText.split(" ").length==1 && !label.contains(" ")) { //label is a single word without spaces
                if(label.equals(phraseText)) {
                    parseTreeNode.tokenType = token;
                    if(tag!=null) {
                        // find functions/operators -> >,<,=, min, max, count
                        String attText = ((Element)phraseList.item(i)).getElementsByTagName(tag)
                                .item(0).getFirstChild().getNodeValue().trim();
                        parseTreeNode.function=attText;
                        System.out.println("Function/Operator Detected: "+parseTreeNode.label+": "+attText);
                    }
                    return true;
                }
            } else if (phraseText.split(" ").length==1 && label.contains(" ")) { // label contains space seperated words
                // label contains phrase
                if(label.contains(phraseText+" ")) {
                    parseTreeNode.tokenType=token;
                    if(tag!=null) {
                        // find functions/operators -> >,<,=, min, max, count
                        String attText = ((Element)phraseList.item(i)).getElementsByTagName(tag)
                                .item(0).getFirstChild().getNodeValue().trim();
                        parseTreeNode.function=attText;
                    }
                    return true;
                }
            } else if (phraseText.contains(label)) { // phrase text is not a single word, and it contains the label
                if(phraseText.equals(label)) { // complete match with the label
                    return true;
                }

                String [] phraseWords = phraseText.split(" ");
                int j=0;
                while(j<phraseWords.length) {  // find the word index that equals the label => j
                    if(phraseWords[j].equals(label)) {
                        break;
                    }
                    j++;
                }

                int index = parseTreeNode.wordOrder;  // wordOrder of the node in the NLQ
                if(index-j > 1 || (parseTreeNode.label.toLowerCase().equals("how") && ((index-j) > 0))) { // check if we can add the additional phrase text as a prefix after the command or root
                    String wholePhrase = "";
                    // find the node upward current and add its label to whole phrase and append the label
                    for(int x=0; x<phraseWords.length-1 && (parseTree.searchNodeByOrder(index-j+x)!=null); x++) {
                        if(j==x) {
                            wholePhrase +=label+" ";
                        } else {
                            wholePhrase+=parseTree.searchNodeByOrder(index-j+x).label+ " ";
                        }
                    }
                    if(parseTree.searchNodeByOrder(index-j+phraseWords.length-1)!=null) {
                        wholePhrase+= parseTree.searchNodeByOrder(index-j+phraseWords.length-1).label;
                    }

                    if(wholePhrase.contains(phraseText)) {
                        parseTreeNode.tokenType = token;
                        if(tag!=null) {
                            String attText = ((Element)phraseList.item(i)).getElementsByTagName(tag)
                                    .item(0).getFirstChild().getNodeValue().trim();
                            parseTreeNode.function=attText;
                        }
                        parseTreeNode.label=phraseText;

                        for(int x=0; x<phraseWords.length; x++) {
                            if(j!=x) {
                                if(parseTree.searchNodeByOrder(index-j+x)!=null) {
                                    ParseTreeNode nodeToDelete = parseTree.searchNodeByOrder(index-j+x);
                                    if(nodeToDelete.tokenType.equals("OT") && nodeToDelete.relationship.equals("conj")) {
                                        nodeToDelete.label = phraseText;
                                        nodeToDelete.function = parseTreeNode.function;
                                        phraseCombDeletedStack.push(parseTreeNode);
                                        parseTree.deleteNode(parseTreeNode);
                                    } else if(nodeToDelete.tokenType.equals("OT") && parseTreeNode.posValue.equals("IN")) {
                                        nodeToDelete.label = phraseText;
                                        nodeToDelete.function = parseTreeNode.function;
                                        phraseCombDeletedStack.push(parseTreeNode);
                                        parseTree.deleteNode(parseTreeNode);
                                    }else {
                                        phraseCombDeletedStack.push(nodeToDelete);
                                        parseTree.deleteNode(parseTree.searchNodeByOrder(index-j+x));
                                    }
                                }
                            }
                        }
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static void checkExactValueMentions(ParseTreeNode treeNode, Database db) throws Exception {
        if(db.isProjectedValueExist(treeNode)) {
            boolean isProjectedMatch = false;
            ParseTreeNode clonedNode = (ParseTreeNode) CommonFunctions.depthClone(treeNode);
            ArrayList<MappedSchemaElement> projAttList = new ArrayList<>();
            for (int j = 0; j<treeNode.mappedSchemaElements.size(); j++) {
                MappedSchemaElement mse = treeNode.mappedSchemaElements.get(j);
                if(mse.schemaElement.isProjected) {
                    MappedSchemaElement projectedSchemaElement = new MappedSchemaElement(db.getProjectedAttribute(mse.schemaElement.projectedRelation, mse.schemaElement.projectedAttribute));
                    projectedSchemaElement.similarityScore = 1 - Parameters.PROJECTED_PENALTY;
                    projAttList.add(projectedSchemaElement);
                    isProjectedMatch = true;
                }
                if(mse.mappedValues.size()>0) {
                    treeNode.mappedSchemaElements.remove(mse);
                    j--;
                }
            }
            ArrayList<MappedSchemaElement> projToRemove = new ArrayList<>();
            for (MappedSchemaElement mse: treeNode.mappedSchemaElements) {
                for (MappedSchemaElement pmse: projAttList) {
                    if(mse.schemaElement.elementID == pmse.schemaElement.elementID) {
                        if (mse.mappedValues.isEmpty()) {
                            mse.isRelationMatch = pmse.isRelationMatch;
                            mse.isExactValueExist = pmse.isExactValueExist;
                            mse.similarityScore = pmse.similarityScore;
                            mse.mappedValues = pmse.mappedValues;
                            mse.choice = pmse.choice;
                            mse.noValueExist = pmse.noValueExist;
                        }
                        projToRemove.add(pmse);
                    }
                }
            }
            projAttList.removeAll(projToRemove);
            treeNode.mappedSchemaElements.addAll(projAttList);
            if(isProjectedMatch) {
                clonedNode.tokenType = "VT";
//                clonedNode.isCandidateSelectNT = false;
                clonedNode.isProjectedAttribute = true;
                treeNode.correspondingProjectedValueNode = clonedNode;
            }
        }
    }

    public static void mapDBElements(Query query, Database db) throws Exception {
        ParseTree parseTree = query.parseTree;
        ArrayList<ParseTreeNode> allNodes = parseTree.allNodes;

        for(int i=0; i<allNodes.size(); i++) {
            ParseTreeNode treeNode = allNodes.get(i);
            if(treeNode.tokenType.equals("NTVT") || treeNode.tokenType.equals("JJ")) {
                // noun / cardinal number or adjective all values

                System.out.println("Noun identified: "+treeNode.label);
                db.isSchemaExist(treeNode);
                System.out.println("\n");
                // consider nouns with values as well
                if(treeNode.isQuoted) {
                    db.isValueExist(treeNode);
                }

                if(treeNode.mappedSchemaElements.isEmpty()) {
                    treeNode.tokenType = "NA";
                }


            } else if (treeNode.tokenType.equals("VT")) { // Value token
                String operator = "=";
                ParseTreeNode operatorNode;
                if(treeNode.parent.tokenType.equals("OT")) { // operator mentioned in the parent
//                    System.out.println(treeNode.parent.label + treeNode.parent.function + " parent"); // testing
                    operator = treeNode.parent.function;
                    operatorNode = treeNode.parent;
                } else if (treeNode.children.size()==1 && treeNode.children.get(0).tokenType.equals("OT")) { // || thibila && kara operator mentioned in the child
                    operator = treeNode.children.get(0).function;
                    operatorNode = treeNode.children.get(0);
                    if(operator.equals("NA") && treeNode.children.get(0).label.equalsIgnoreCase("at least")) { // at least mentioned in one children
                        operator=">=";
                    }
                }

                if(!NumberUtils.isCreatable(treeNode.label)) {
                    if(db.isValueExist(treeNode)) {
                        treeNode.tokenType = "VTTEXT";
                    }

                }

                // isNumExist
                if(db.isNumberValueExist(treeNode, operator)) {
                    treeNode.tokenType = "VTNUM";
                }

                System.out.println("Operator->"+operator+"associated with: "+treeNode.label);
            }
        }
    }

    public static void identifyValuesNER(Query query) {
        ParseTree parseTree = query.parseTree;
        ArrayList<ParseTreeNode> nodes = parseTree.allNodes;
        PreprocessPipeline preprocessPipeline;
        StanfordCoreNLP pipeline;
        preprocessPipeline = PreprocessPipeline.init_Preprocess_Pipeline();
        pipeline = preprocessPipeline.getPreProcessPipeline();
        scanNLQforNER(query, pipeline);

        for(int i=0; i<nodes.size(); i++) {
            ParseTreeNode node = nodes.get(i);
            // TODO: add logic to get date wordOrder - 1 -> pos == CASE ,  4 digit, check for CC (conjunctions) wordOrder+1
            CoreDocument document = new CoreDocument(nodes.get(i).label);
            pipeline.annotate(document);
            for(CoreEntityMention em: document.entityMentions()) {
                System.out.println("\nNER Recognition________");
                System.out.println("Entity mention: "+em.text()+"\t"+em.entityType()+"\n");
                System.out.println();
                node.nerVal = em.entityType();
                node.tokenType = "VT";

                // ORDINAL 1st, PERCENT %, SET?
                if(em.entityType().equals("DATE")) {
                    node.valueType = "date";
                } else if (em.entityType().equals("DURATION")) {
                    node.valueType = "date_time";
                } else if (em.entityType().equals("TIME")) {
                    node.valueType = "time";
                } else if (em.entityType().equals("MONEY")) {
                    node.valueType = "currency";
                } else if (em.entityType().equals("NUMBER")) {
                    node.valueType = "number";
                } else if (em.entityType().equals("ORDINAL")) {
                    node.valueType = "ordinal";
                } else if (em.entityType().equals("PERSON")  || em.entityType().equals("LOCATION") || em.entityType().equals("ORGANIZATION") || em.entityType().equals("MISC") ) {
                    node.valueType = "text";
                    node.posValue = "NNP";
                } else if (em.entityType().equals("TITLE")) {
                    node.valueType = "title";
                    node.tokenType = "NA";
                }
            }
        }
    }

    public static void scanNLQforNER(Query query, StanfordCoreNLP pipeline) {
        CoreDocument document = new CoreDocument(query.sentence.NLQ);
        pipeline.annotate(document);
        for(CoreEntityMention em: document.entityMentions()) {
            System.out.println("\nNER Recognition________");
            System.out.println("Entity mention: "+em.text()+"\t"+em.entityType()+"\n");
            ParseTreeNode node = query.parseTree.searchNodeByWord(em.text(), true);
            if(node != null) {
                System.out.println("Node: "+node.label);
                System.out.println();
            } else {
                if(em.text().length() > 1) {
                    String[] words = em.text().split(" ");
                    for(String word: words) {
                        node = query.parseTree.searchNodeByWord(word, true);
                        if(node != null) {
                            if(((node.relationship.equals("flat")) || node.relationship.equals("fixed") || node.relationship.equals("compound"))){
                                boolean isCombine = true;
                                if(node.posValue.startsWith("NNP") && (!node.parent.posValue.startsWith("NNP") && node.parent.posValue.startsWith("NN")) ||
                                        node.parent.posValue.startsWith("NNP") && (!node.posValue.startsWith("NNP") && node.posValue.startsWith("NN"))) {
                                    if(node.posValue.startsWith("NNP")) {
                                        node.valuePredicate = node.parent;
                                    }
                                    isCombine = false;
                                }

                                if(node.isQuoted || node.parent.isQuoted) {
                                    isCombine = false;
                                }

                                if (isCombine){
                                    if (node.wordOrder > node.parent.wordOrder) {
                                        System.out.println("Word: " + node.label + " => identified Token: MWE (" + node.relationship + ")\nNode Label(parent): " + node.parent.label + " -assigned->" + node.parent.label + " " + node.label);
                                        System.out.println();
                                        node.parent.label = node.parent.label + " " + node.label;
                                    } else {
                                        System.out.println("Word: " + node.label + " => identified Token: MWE (" + node.relationship + ")\nNode Label(parent): " + node.parent.label + " -assigned->" + node.label + " " + node.parent.label);
                                        System.out.println();
                                        node.parent.label = node.label + " " + node.parent.label;
                                    }

                                    query.parseTree.deleteNode(node);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public static void rankMappedElements(Query q) {
        ArrayList<ParseTreeNode> nodes = q.parseTree.allNodes;
        for(ParseTreeNode node: nodes) {
            if(node.mappedSchemaElements.isEmpty()) {
                continue;
            }

            ArrayList<MappedSchemaElement> mappedSchemaElements = node.mappedSchemaElements;
            for(MappedSchemaElement e: mappedSchemaElements) {
                SimilarityFunctions.getSimilarity(node, e);
            }
            Collections.sort(mappedSchemaElements);
        }

        nodes = q.parseTree.allNodes;

        for(ParseTreeNode node: nodes) {
            if(node.tokenType.equals("NTVT")) {
                continue;
            }
            ArrayList<MappedSchemaElement> deleteList = new ArrayList<>();
            ArrayList<MappedSchemaElement> mappedSchemaElements = node.mappedSchemaElements;
            for (int i = 0; i < mappedSchemaElements.size(); i++) {
                MappedSchemaElement NT = mappedSchemaElements.get(i);
                for (int j = i+1; j < mappedSchemaElements.size(); j++) {
                    MappedSchemaElement VT = mappedSchemaElements.get(j);
                    if(NT.mappedValues.isEmpty() && !VT.mappedValues.isEmpty() && NT.schemaElement.equals(VT.schemaElement)) {
                        if(NT.similarityScore >= VT.similarityScore) {
                            VT.similarityScore = NT.similarityScore;
                            VT.choice = -1;
                            int VTpostion = node.mappedSchemaElements.indexOf(VT);
                            node.mappedSchemaElements.set(node.mappedSchemaElements.indexOf(NT), VT);
                            node.mappedSchemaElements.set(VTpostion, NT);
                        }
                        deleteList.add(NT);
                    }
                }
            }
            node.mappedSchemaElements.removeAll(deleteList);
        }
    }

    public static void rankAsGroup(Query query, Database database) {
        // get the highest score for parse tree nodes
        // get first node of the parse tree nodes and assign a score of 0
        ParseTreeNode firstNode = query.parseTree.allNodes.get(0);
        double firstScore = 0;

        // get each element and check for scores
        for (ParseTreeNode treeNode : query.parseTree.allNodes) {
            double score =0;
            if(!treeNode.mappedSchemaElements.isEmpty()) {
                // if only one element exists for mapped schema elements then assign score of 1
                if(treeNode.mappedSchemaElements.size() == 1) {
                    score = 1;
                // if more than one element exists for mapped schema elements then assign score of 1 - (similarity score of second element / similarity score of first element)
                } else {
                    // get degree of dissimilarity between first two elements
                    score = 1- treeNode.mappedSchemaElements.get(1).similarityScore/ treeNode.mappedSchemaElements.get(0).similarityScore;
                }

                // get the highest scored node
                if(score >= firstScore) {
                    firstNode = treeNode;
                    firstScore = score;
                }
            }

        }
        // if the first node is the root node then return that means no mapped schema elements whatsoever
        if(firstNode.label.equals("ROOT")) {
            return;
        }

        // make the highest ranked node's choice 0
        firstNode.choice = 0;

        // initialize a boolean array to keep track of nodes that have been considered
        boolean[] done = new boolean[query.parseTree.allNodes.size()];
        for (int i = 0; i < done.length; i++) {
            done[i] = false;
        }

        // initialize a queue and enter the highest scored node twice
        ArrayList<ParseTreeNode> queue = new ArrayList<>();
        queue.add(firstNode);
        queue.add(firstNode);

        while (!queue.isEmpty()) {
            // first node is the parent and second node is the child
            ParseTreeNode parent = queue.remove(0);
            ParseTreeNode child = queue.remove(0);
            int in = query.parseTree.allNodes.indexOf(child);
            // if the child has not been considered then consider it
            if(done[query.parseTree.allNodes.indexOf(child)] == false) {
                // if child and parent are not the same
                if(!parent.equals(child)) {
                    int maxPosition = 0;
                    double maxScore = 0;
                    // get child's mapped schema elements
                    // get parents choice and compute the highest mapped element of child that maximized the score incorporating the distance between the two elements
                    ArrayList<MappedSchemaElement> mappedSchemaElements = child.mappedSchemaElements;
                    for (int i = 0; i < mappedSchemaElements.size(); i++) {
                        MappedSchemaElement parentElement = parent.mappedSchemaElements.get(parent.choice);
                        MappedSchemaElement childElement = child.mappedSchemaElements.get(i);
                        double distance = database.schemaGraph.getDistance(parentElement.schemaElement, childElement.schemaElement);
                        double currentScore= parentElement.similarityScore*childElement.similarityScore*distance;

                        if(currentScore > maxScore) {
                            maxScore = currentScore;
                            maxPosition = i;
                        }
                    }
                    // set child's choice to the highest scored index
                    child.choice = maxPosition;
                }

                // if child has no mapped schema elements then add its parent and children to the queue
                if(child.mappedSchemaElements.isEmpty()) {
                    for (int i = 0; i < child.children.size(); i++) {
                        queue.add(parent);
                        queue.add(child.children.get(i));
                    }
                    // if child parent is not null then add parent and child's parent to the queue
                    if(child.parent != null) {
                        queue.add(parent);
                        queue.add(child.parent);
                    }
                } else {
                    // if child has mapped schema elements then add child and its children to the queue
                    for (int i = 0; i < child.children.size(); i++) {
                        queue.add(child);
                        queue.add(child.children.get(i));
                    }

                    // if child parent is not null then add child and child's parent to the queue
                    if(child.parent != null) {
                        queue.add(child);
                        queue.add(child.parent);
                    }
                }
                done[query.parseTree.allNodes.indexOf(child)] = true;
            }
        }

        for (ParseTreeNode node:query.parseTree.allNodes) {
            if(node.tokenType.equals("NTVT") || node.tokenType.equals("JJ")) {
                if(node.mappedSchemaElements.size() > 0) {
                    if(node.mappedSchemaElements.get(node.choice).mappedValues.size() == 0 ||
                        node.mappedSchemaElements.get(node.choice).choice == -1) {
                        node.tokenType = "NT";
                    } else {
                        node.tokenType = "VTTEXT";
                    }
                }
            }

        }
    }



    public static void deleteUselessNodes(Query query) {
        ParseTree parseTree = query.parseTree;
        query.originalParseTree = parseTree;
        for(int i = 0; i < parseTree.allNodes.size(); i++) {
            ParseTreeNode currentNode = parseTree.allNodes.get(i);
            if(currentNode.tokenType.equals("NA") || currentNode.tokenType.equals("QT")) {
                if(currentNode.label.equals("on") || currentNode.label.equals("in") || currentNode.label.equals("of") || currentNode.label.equals("by")) {
                    if (!currentNode.children.isEmpty()) {
                        currentNode.children.get(0).prep = currentNode.label; // set preposition
                    }
                }
                if (currentNode.tokenType.equals("QT")) {
                    currentNode.parent.QT = currentNode.function;
                }
                parseTree.deleteNode(currentNode);
                i--;
            }
        }
    }

    public static void deleteNoMatch(Query query) {
        ParseTree parseTree = query.parseTree;
        for (int i = 0; i < parseTree.allNodes.size(); i++) {
            ParseTreeNode node = parseTree.allNodes.get(i);
            if (node.tokenType.equals("NA")) {
                if (node.label.equals("on") || node.label.equals("in")) {
                    node.parent.prep = node.label;
                }
                parseTree.deleteNode(node);
                i--;
            }
        }
    }

    public static void deleteSecondaryTokens(Query query) {
        ParseTree parseTree = query.parseTree;
        for (int i = 0; i < parseTree.allNodes.size(); i++) {
            ParseTreeNode node = parseTree.allNodes.get(i);
            if (node.tokenType.equals("WST")) {
                parseTree.deleteNode(node);
                i--;
            }
        }
    }

    public static void printMappedSchemaElements(Query query) {
        ParseTree parseTree = query.parseTree;
        for (ParseTreeNode node: query.parseTree.allNodes) {
            System.out.println("Node Label: " + node.label+"\t");
            if(!node.mappedSchemaElements.isEmpty()) {
                for (int i = 0; i<Parameters.CANDIDATE_NUM && i<node.mappedSchemaElements.size() ; i++) {
                    System.out.println(i+1+". Mapped schema element: "+node.mappedSchemaElements.get(i).schemaElement.relation.name+"."+node.mappedSchemaElements.get(i).schemaElement.name+ " => "+node.mappedSchemaElements.get(i).similarityScore);
                    if (!node.mappedSchemaElements.get(i).mappedValues.isEmpty() && (node.mappedSchemaElements.get(i).schemaElement.type.equals("text"))) {
                        for (int j = 0; j < node.mappedSchemaElements.get(i).mappedValues.size() && j < 5 ; j++) {
                            System.out.println("\t\t"+(j+1)+". Mapped Value: "+ node.mappedSchemaElements.get(i).mappedValues.get(j).value+ "" +
                                    " score: "+ node.mappedSchemaElements.get(i).mappedValues.get(j).similarityScore);
                        }
                    }
                }
            }
            System.out.println("\n\n");
        }
    }

    public static void considerValueVerbs(Query query) {
        for (ParseTreeNode node: query.parseTree.allNodes) {
            if(node.tokenType.startsWith("VT")) {
                if(node.relationship.startsWith("obl")  && node.parent.tokenType.equals("NA") && node.parent.posValue.startsWith("VB") &&
                !CommonFunctions.stopwords.contains(node.parent.label.toLowerCase())) {
                    System.out.println("Value verb assoc: "+ node.label+"->"+node.parent.label);
                    node.parent.tokenType = "NTVT";
                }
            }
        }
    }

    public static void handleOperatorConjunctions(Query query) {
        ParseTree queryParseTree = query.parseTree;
        for (int i = 0; i < queryParseTree.allNodes.size(); i++) {
            ParseTreeNode currentNode = queryParseTree.allNodes.get(i);
            int operatorWordOrder = query.sentence.NLQ.indexOf(currentNode.label);;
            if(currentNode.relationship.equals("conj") && currentNode.parent.tokenType.equals("OT")){
                String cc = queryParseTree.searchNodeByOrder(currentNode.wordOrder-1).label;
                if(currentNode.parent.function.equals("=")) {
                    currentNode.parent.function = currentNode.function+"=";
                } else if (currentNode.function.equals("=")){
                    String parentDFun = currentNode.parent.function;
                    currentNode.parent.function = parentDFun+"=";
                }
                currentNode.parent.label += " "+cc+" "+currentNode.label;
                System.out.println("Joined operator: "+currentNode.parent.label+" -assigned-> OT tokenType ("+currentNode.parent.function+")");
                queryParseTree.deleteNode(currentNode);
                i--;


            } else if (currentNode.tokenType.equals("OT") && currentNode.relationship.equals("conj") && currentNode.parent.posValue.equals("IN")) {
                String pLabel =  currentNode.label;
                String cc = queryParseTree.searchNodeByOrder(currentNode.wordOrder-1).label;
                currentNode.label = currentNode.parent.label + " "+cc+" " + pLabel;
                currentNode.function = currentNode.function + "=";
            }
        }
    }

    public static void handleNegations(Query query) {
        ParseTree queryParseTree = query.parseTree;
        for (int i=0; i<queryParseTree.allNodes.size(); i++) {
            ParseTreeNode node = queryParseTree.allNodes.get(i);
            if(node.tokenType.equals("NEG")) {
                if(node.parent.tokenType.startsWith("VT")) {
                    node.parent.isNegated = true;
                } else {
                    for (ParseTreeNode child: node.parent.children) {
                        if(child.tokenType.startsWith("VT")) {
                            child.isNegated = true;
                        }
                    }
                }
                queryParseTree.deleteNode(node);
                i--;
            }
        }
    }

    public static void considerVerbNTMentions(Query query) {
        for (ParseTreeNode node : query.parseTree.allNodes) {
            if (node.tokenType.equals("WPT") || node.tokenType.equals("WST") || node.tokenType.equals("HPT")) {
                if ((node.tokenType.equals("WPT") || node.tokenType.equals("HPT")) &&
                        (node.label.equalsIgnoreCase("who") || node.label.equalsIgnoreCase("when") || node.label.equalsIgnoreCase("where") || node.label.toLowerCase().startsWith("how"))
                        && node.parent.posValue.startsWith("VB") && node.parent.tokenType.equals("NA") &&
                        !CommonFunctions.stopwords.contains(node.parent.label)) {

                        System.out.println("WH verb consideration: " + node.label + " -> " + node.parent.label);
                        node.parent.tokenType = "NTVT";

                }
            }
        }
    }


}
