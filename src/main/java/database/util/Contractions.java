package database.util;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Hashtable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Contractions {
    public static void main(String[] args) throws FileNotFoundException {
        Gson gson = new Gson();
        Hashtable<String, String> contractions;

        JsonReader reader = new JsonReader(new FileReader("files/contractions.json"));
            contractions = gson.fromJson(reader, Hashtable.class);


        Pattern contraction_pattern = Pattern.compile("("+String.join(" | ", contractions.keySet())+")", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher matcher = contraction_pattern.matcher("Who isn't the president?");



        while (matcher.find()) {
            System.out.println("match: "+ matcher.group(1) + contractions.get(matcher.group(1).trim()));
        }


    }
}
