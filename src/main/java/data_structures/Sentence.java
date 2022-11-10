package data_structures;

import java.util.ArrayList;

public class Sentence {

    public ArrayList<String> tokenList;
    public String [] outputWords;

    public Sentence(String inputQuery) {
        tokenList = new ArrayList<String>();
        System.out.println(inputQuery+" inside Sentence");
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

            if(c=='\t' || c=='\n' || c==' ') {
                if(!isQuoted) {
                    tokenList.add(currentWord);
                    currentWord="";

                    while(i<query.length()-1 && // check if it's the last char and ignore any trailing unwanted chars
                            (query.charAt(i+1) == '\t' || query.charAt(i+1) == '\n'
                             || query.charAt(i+1) == ' ' || query.charAt(i+1) == ',')) {
                        i++;
                    }
                } else {
                    currentWord += query.charAt(i);
                }
            }

            else if (c=='\'') {
                if(!isQuoted) {
                    if(query.charAt(i+1) == 't') {
                        currentWord += query.charAt(i);
                    } else {
                        tokenList.add(currentWord);
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
            res += "\"" + outputWords[i] + "\" ";
        }
        System.out.println(res);
        return res;
    }

}
