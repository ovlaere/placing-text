package be.ugent.intec.ibcn.geo.clustering;

import be.ugent.intec.ibcn.geo.common.datatypes.DataItem;
import be.ugent.intec.ibcn.geo.common.interfaces.LineParserPoint;

/**
 * This class provides a default implementation of LineParserPoint.
 * 
 * In this case, we provide a simple parser that fetches the ID, lat and lon
 * from each of the training items.
 * 
 * @author Olivier Van Laere <oliviervanlaere@gmail.com>
 */
public class LineParserClusterInputDefault extends LineParserPoint {
    
    /**
     * Constructor.
     */
    public LineParserClusterInputDefault() {
        super();
    }
        
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
