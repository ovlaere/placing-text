package org.multimediaeval.placing;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.net.URLDecoder;

public class Normalizer {

    public static void main(String[] args) {
        String infile = args[0];
        String outfile = args[1];
        int idCounter = 1;
        try {
            BufferedReader in = new BufferedReader(new FileReader(infile));
            PrintWriter out = new PrintWriter(new FileWriter(outfile));
            String line = in.readLine();
            while (line != null) {
                String[] pieces = line.split(",");
                String processed = "";
                // Check to see if this is not just the line with the line count
                if (pieces.length > 1) {

                    String id = pieces[1];
                    String lat = "";
                    String lon = "";
                    String normalized = "";
                    
                    // Check to see if there are tags and coordinates
                    if (pieces.length > 4) {
                    
                        lat = pieces[2];
                        lon = pieces[3];
                        normalized = "";
                        
                        // Check to see if there are tags
                        if (pieces.length == 5) {
                            String tags = pieces[4];
                            for (String t : tags.split(" ")) {
                                String tag = t;
                                // URL Decode
                                tag = URLDecoder.decode(tag, "UTF-8");
                                // Replace whitespaces
                                tag = tag.replaceAll("\\s+", "");
                                // Normalize
                                tag = java.text.Normalizer.normalize(tag, java.text.Normalizer.Form.NFD).replaceAll("\\p{IsM}+", "");
                                // alpha only
                                tag = tag.replaceAll("[^\\p{L}\\p{N}]", "");
                                // Add
                                normalized += tag + " ";
                            }
                            normalized = normalized.trim();
                        }
                    }
                    processed = idCounter++ + "," + id + "," + lat + "," + lon + "," + normalized;
                } else {
                    processed = line;
                }
                out.println(processed);
                line = in.readLine();
            }
            in.close();
            out.close();
        } catch (Exception e) {
            System.err.println("Exception: " + e.getMessage());
        }
    }
}