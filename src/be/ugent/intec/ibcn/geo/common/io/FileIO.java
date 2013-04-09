package be.ugent.intec.ibcn.geo.common.io;

import be.ugent.intec.ibcn.geo.common.datatypes.Point;
import be.ugent.intec.ibcn.geo.common.io.parsers.LineParserMedoid;
import be.ugent.intec.ibcn.geo.common.Util;
import be.ugent.intec.ibcn.geo.common.datatypes.DataItem;
import be.ugent.intec.ibcn.geo.common.interfaces.LineParserDataItemSimilarity;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class provides the necessary IO methods to read data from file.
 *
 * @author Olivier Van Laere <oliviervanlaere@gmail.com>
 */
public class FileIO {
    
    /**
     * Find out the number of lines in the given file.
     * The linecount is supposed to be on the first line, if not, the file will
     * be iterated to count the number of lines.
     * @param filename Filename for which the linecount is requested,
     * @return the linecount of the given file
     */
    public static int getNumberOfLines(String filename) {
        // Default to -1;
        int lines = -1;
        try {
            BufferedReader in = new BufferedReader(new FileReader(filename));
            // Parse the linecount on the first line
            lines = Integer.parseInt(in.readLine());
            in.close();
        } catch (NumberFormatException e) {
            System.out.println("Linecount not found on first line. Will loop trough the file.");
        } catch (IOException e) {
            System.err.println("IOException: " + e.getMessage());
        }
        // Loop through the file to find out the number of lines
        if (lines < 0) {
            int counter = 0;
            try {
                BufferedReader in = new BufferedReader(new FileReader(filename));
                while (in.readLine() != null)
                    counter++;
                in.close();
                lines = counter;
            } catch (IOException e) {
                System.err.println("IOException: " + e.getMessage());
            }
        }
        // Throw an exception in case something went wrong
        if (lines < 0)
            throw new RuntimeException("Missing or invalid line count for " + filename);
        return lines;
    }
    
    /**
     * Load the cluster/class-centra/medoids from file.
     * @param filename Filename of the file that contains the medoids
     * @param parserClassName Input parser to use
     * @return A List of Points that represent the cluster centra
     */
    public static List<Point> loadMedoids(String filename, String parserClassName) {
        List<Point> medoids = new ArrayList<Point>();
        try {
            // Instantiate the parser
            LineParserMedoid parser = (LineParserMedoid)Util.getParser(parserClassName);
            BufferedReader in = new BufferedReader(new FileReader(filename));
            String line = in.readLine();
            while (line != null) {
                Point p = parser.parse(line);
                if (p != null)
                    medoids.add(p);
                line = in.readLine();
            }            
            in.close();
        } catch (IOException e) {
            System.err.println("IOException: " + e.getMessage());
        }
        System.out.println("Loading medoids from " + filename + ". Loaded medoids: " + medoids.size());
        return medoids;
    }
    
    /**
     * Recursive delete of files and folders.
     * @param f The file or folder to (recursively) delete.
     */
    public static void delete(File f) {
        if (f.isDirectory()) {
            for (File c : f.listFiles())
                delete(c);
        }
        if (!f.delete())
            System.err.println("Failed to delete file: " + f);
    }
}