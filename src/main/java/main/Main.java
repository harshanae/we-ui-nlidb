package main;

import configuration.DBConfig;
import database.Database;

import java.io.FileNotFoundException;

public class Main {
    public static void main(String[] args) {
        System.out.println("Welcome to WE-UI NLIDB...");

        try {
            Database db = new Database(DBConfig.getProperty("dbHost"),
                    DBConfig.getIntegerProperty("dbPort"),
                    DBConfig.getProperty("dbUser"),
                    DBConfig.getProperty("dbPassword"), "mas");
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

    }
}