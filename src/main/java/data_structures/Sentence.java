package data_structures;

import java.util.ArrayList;
import java.util.HashMap;

public class Sentence {

    public ArrayList<String> tokenList;
    public String [] outputWords;
    public String NLQ;
    public HashMap<Integer, Boolean> isQoutedHashMap = new HashMap<>();

    public Sentence(String inputQuery) {
        tokenList = new ArrayList<String>();
        System.out.println(inputQuery);
        NLQ = inputQuery;
        tokenize(inputQuery);

        outputWords = new String[tokenList.size()];
        for(int i=0; i<tokenList.size(); i++) {
            outputWords[i] = tokenList.get(i);
        }
    }

    public void tokenize(String query) {
        // remove leading and trailing whitespaces

        query = query.trim();
        // remove unwanted escape characters at the end
        while(query.charAt(query.length()-1) == '.' || query.charAt(query.length()-1) == '?') {
            query = query.substring(0, query.length()-1);
        }

        query+=" ";

        String currentWord = "";
        boolean isQuoted = false;
        for(int i=0; i<query.length(); i++) {
            char c = query.charAt(i);

            if(c=='\t' || c=='\n' || c==' ') { // if white space break to a token
                if(!isQuoted) {
                    if(!isQoutedHashMap.containsKey(tokenList.size())) {
                        isQoutedHashMap.put(tokenList.size(), false);
                    }
                    tokenList.add(currentWord);
                    currentWord="";

                    while(i<query.length()-1 && // check if it's not the last char and ignore any trailing unwanted chars
                            (query.charAt(i+1) == '\t' || query.charAt(i+1) == '\n'
                             || query.charAt(i+1) == ' ' || query.charAt(i+1) == ',')) {
                        i++;
                    }
                } else { // Quoted we need to continue until end of the quotation
                    currentWord += query.charAt(i);
                }
            }
            else if (c=='\'') {
                if(!isQuoted) {
                    if(query.charAt(i+1) == 't') {
                        currentWord += query.charAt(i);
                    } else {
                        if(!isQoutedHashMap.containsKey(tokenList.size())) {
                            isQoutedHashMap.put(tokenList.size(), false);
                        }
                        tokenList.add(currentWord);

                        if(!isQoutedHashMap.containsKey(tokenList.size())) {
                            isQoutedHashMap.put(tokenList.size(), false);
                        }
                        tokenList.add("\'s");

                        currentWord="";
                        if(i<query.length()-1 && query.charAt(i+1) == 's') {
                            i++;
                        }
                        i++;
                    }
                } else {
                    currentWord+=query.charAt(i);
                }
            } else if (c=='\"') { // handle quotations ""
                if(!isQuoted) {
                    isQuoted = true;
                } else {
                    isQoutedHashMap.put(tokenList.size(), true);
                    isQuoted = false;
                }
            } else {
                currentWord += query.charAt(i);
            }
        }
    }

    public String printTokens() {
        String res = "";
        for(int i=0; i<outputWords.length; i++) {
            res += "\"" + outputWords[i] + "\" => isQuoted: "+isQoutedHashMap.get(i)+"\n";
        }
        System.out.println(res);
        return res;
    }

}
