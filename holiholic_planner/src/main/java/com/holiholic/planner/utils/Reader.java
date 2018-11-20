package com.holiholic.planner.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.StringTokenizer;

public class Reader {
    private static BufferedReader reader;
    private static StringTokenizer tokenizer;

    public static void init(InputStream input) {
        reader = new BufferedReader(new InputStreamReader(input));
        tokenizer = new StringTokenizer("");
    }

    public static String next() throws IOException {
        while (!tokenizer.hasMoreTokens()) {
            // check for eof
            String line = reader.readLine();
            if (line == null) {
                return null;
            }
            tokenizer = new StringTokenizer(line);
        }
        return tokenizer.nextToken();
    }

    public static String readLine() throws IOException {
        return reader.readLine();
    }

    public static Integer nextInt() throws IOException {
        String s = next();
        return s == null ? null : Integer.parseInt(s);
    }

    public static Double nextDouble() throws IOException {
        String s = next();
        return s == null ? null : Double.parseDouble(s);
    }
}
