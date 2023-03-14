package database.util;

import com.google.gson.Gson;
import com.google.gson.JsonArray;

import java.io.FileNotFoundException;
import java.io.FileReader;

public class InferAttributeNER {

    public static void main(String args[]) throws FileNotFoundException {
        String databaseName = "imdb";
        Gson gson = new Gson();
        JsonArray relations = gson.fromJson(new FileReader("src/source-files/"+databaseName+"Relations.json"),  JsonArray.class);

    }
    public static void inferNER() {

    }
}
