package be.ugent.intec.ibcn.geo.common.io.parsers;

import be.ugent.intec.ibcn.geo.common.datatypes.DataItem;

/**
 * This class provides a dummy implementation of LineParserPoint for
 * loading data points for clustering.
 * 
 * The sample file format used is ID,...,lat,lon,...
 * 
 * @author Olivier Van Laere <oliviervanlaere@gmail.com>
 */
public class LineParserClusterPoint extends LineParserPoint {
  
    /**
     * Implementation of parse.
     */
    @Override
    public DataItem parse(String line) {
        // Prepare an empty DataItem.
        DataItem item = null;
        try {
            // Split the line
            String [] values = pattern_comma.split(line.toLowerCase());
            // Get the id from the first item
            int id = Integer.parseInt(values[0]);
            // Parse latitude from index 2
            double lat = Double.parseDouble(values[2]);
            // Parse latitude from index 3
            double lon = Double.parseDouble(values[3]);
            // Instantiate the DataItem
            item = new DataItem(id, lat, lon, null);
        }
        // In case a parse error occurs, ignore the error but mark this error.
        // At the end of the data loading, this error count will be presented
        // and provided feedback about the errors during parsing. Error items
        // will not be instantiated.
        catch (Exception e) {
            this.errors++;
        }
        // Keep track of a processed count
        this.processed++;
        return item;
    }
}