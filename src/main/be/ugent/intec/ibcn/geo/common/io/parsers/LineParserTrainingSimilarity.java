package be.ugent.intec.ibcn.geo.common.io.parsers;

import be.ugent.intec.ibcn.geo.common.datatypes.DataItem;
import be.ugent.intec.ibcn.geo.common.interfaces.LineParserDataItemSimilarity;
import java.util.Set;

/**
 * Generic data input parser implementation for parsing DataItem objects used
 * for similarity purposes.
 * 
 * @author Olivier Van Laere <oliviervanlaere@gmail.com>
 */
public class LineParserTrainingSimilarity extends LineParserPoint
    implements LineParserDataItemSimilarity {
    
    /**
     * A Set of features to use when loading the data.
     * @param features 
     */
    protected Set<String> filter = null;
    
    /**
     * Set a Set of features to use when loading the data.
     * @param features 
     */
    @Override
    public void setFilter(Set<String> filter) {
        this.filter = filter;
    }
    
    /**
     * Actual parse implementation.
     * @param line The String input line from file
     * @return An instantiated DataItem or null if no features were present
     */
    @Override
    public DataItem parse(String line) {
        // Prepare the return result
        DataItem item = null;
        try {
            // Parse the line
            String [] values = pattern_comma.split(line.toLowerCase());
            int id = Integer.parseInt(values[0]);
            double lat = Double.parseDouble(values[2]);
            double lon = Double.parseDouble(values[3]);
            // Split the features
            String [] data = pattern_space.split(values[4]);
            // Prevent empty features
            if (data.length == 1 && data[0].equals(""))
                data = new String[0];
            // If there are features
            if (data.length > 0) {
                // In case of no feature selection
                if (filter == null)
                    item = new DataItem(id, lat, lon, data);
                else {
                    boolean hit = false;
                    // Search for at least 1 feature in common with the 
                    // featureset
                    for (Object f : data) {
                        if (filter.contains((String)f)) {
                            hit = true;
                            break;
                        }
                    }
                    if (hit)
                        item = new DataItem(id, lat, lon, data);
                }
            }
        }
        catch (Exception e) {
            this.errors++;
        }
        this.processed++;
        return item;
    }
}