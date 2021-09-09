/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rami.prf;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Rami Manaf Abdullah
 */
public class Utils {

    public static List<String[]> loadCSV(File file) throws IOException, MalformedDataException {
        try ( BufferedReader reader = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
            String entery;
            ArrayList<String[]> result = new ArrayList<>();
            while ((entery = reader.readLine()) != null) {
                if(entery.equals("sep=;")){
                    continue;
                }
                while (countOccurrences(entery, '"') % 2 != 0) {
                    String temp = reader.readLine();
                    if (temp == null) {
                        throw new MalformedDataException("Malformed CSV");
                    }
                    entery += ("\n" + temp);
                }
                entery = entery.replaceAll("\"\"", "\"");
                StringBuilder buffer = new StringBuilder();
                boolean openedText = false;
                String[] values = new String[6];
                int i = 0;
                for (char c : entery.toCharArray()) {
                    if (c == '"') {
                        openedText = !openedText;
                        if (openedText && buffer.length() > 0) {
                            buffer.append('"');
                        }
                    } else if (c == ';' && !openedText) {
                        values[i] = buffer.toString();
                        buffer.setLength(0);
                        i++;
                    } else {
                        buffer.append(c);
                    }
                }
                if (i < values.length) {
                    values[i] = buffer.toString();
                }
                result.add(values);
            }
            if(!result.isEmpty()){
                if(!"PMID".equals(result.get(0)[0])){
                    result.add(0, new String[]{"PMID","Rate","Clinical Trial","Phase","Number of Patients","Findings"});
                }
            }
            return result;
        }
    }

    public static void saveCSV(List<String[]> data, File to) throws IOException {
        try ( BufferedWriter writer = new BufferedWriter(new FileWriter(to, StandardCharsets.UTF_8, false))) {
            writer.write("sep=;\r\n");
            for (String[] values : data) {
                String[] result = new String[values.length];
                for (int i = 0; i < values.length; i++) {
                    if (values[i] == null || values[i].isEmpty()) {
                        result[i] = "";
                    } else {
                        result[i] = "\"" + values[i].replaceAll("\"", "\"\"") + "\"";
                    }
                }
                writer.write(String.join(";", result) + "\r\n");
            }
        }
    }

    public static int countOccurrences(String haystack, char needle) {
        int count = 0;
        for (int i = 0; i < haystack.length(); i++) {
            if (haystack.charAt(i) == needle) {
                count++;
            }
        }
        return count;
    }

}
