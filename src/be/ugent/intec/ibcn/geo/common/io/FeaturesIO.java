package be.ugent.intec.ibcn.geo.common.io;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class provides the necessary IO methods to feature related operations.
 * 
 * @author Olivier Van Laere <oliviervanlaere@gmail.com>
 */
public class FeaturesIO {
    
    /**
     * Export a Set of features to file. This set should originate from a
     * sorted HashMap, because the idea of feature ranking is that order is
     * important. In general, the exportFeaturesToFile method should be used
     * that uses a list of features.
     * @param features (Ordered) set of features to write to file with their IDs
     * @param filename The filename of the output
     */
    public static void exportFeaturesToFile(Set<Object> features, 
            String filename) {
        System.out.println("Exporting features to file " + filename);
        try {
            PrintWriter file = new PrintWriter(new FileWriter(filename));
            int index = 0;
            for (Object feature : features) {
                file.println(index++ + "\t" + feature);
            }
            file.close();
        }
        catch (IOException e) {
            System.err.println("Error writing selected features to file: " 
                    + e.getMessage());
        }
    }
    
    /**
     * Export a List of features to file.
     * @param features (Ordered) set of features to write to file with their IDs
     * @param filename The filename of the output
     */
    public static void exportFeaturesToFile(List<Object> features, 
            String filename) {
        System.out.println("Exporting features to file " + filename);
        try {
            PrintWriter file = new PrintWriter(new FileWriter(filename));
            int index = 0;
            for (Object feature : features) {
                file.println(index++ + "\t" + feature);
            }
            file.close();
        }
        catch (IOException e) {
            System.err.println("Error writing selected features to file: " 
                    + e.getMessage());
        }
    }
    
    /**
     * Load the selected features from file.
     * @param filename Filename to load the features from
     * @param featuresToRetain Number of features to retain.
     * @return A Map containing the features along with their ID.
     */
    public static Map<Object, Integer> loadFeaturesFromFile(String filename, 
            int featuresToRetain) {
        System.out.println("Loading selected features from file " + filename);
        Map<Object, Integer> features = new HashMap<Object, Integer>();
        try {
            // Read from file
            BufferedReader in = new BufferedReader(new FileReader(filename));
            String line = in.readLine();
            int skipped = 0;
            int id = 0;
            // While there is input
            while (line != null) {
                // Split on tab
                String [] fields = line.split("\t");
                // If the feature is already known - in lowercase
                if (features.get(fields[1].toLowerCase()) != null)
                    // Skip it
                    skipped++;
                else
                    // Add the feature - IDs are ignored - the IDs on file 
                    // are just for visual debug
                    features.put(fields[1].toLowerCase(), id++);
                // Break if we reached the amound of features needed
                if (features.size() == featuresToRetain)
                    break;
                line = in.readLine();
            }
            in.close();
            System.out.println("Loaded features: " + features.size() + 
                    " skipped: "+ skipped +" from " + filename);
        }
        catch (IOException e) {
            System.err.println("Error reading selected features from file: " 
                    + e.getMessage());
        }
        return features;
    }
}
