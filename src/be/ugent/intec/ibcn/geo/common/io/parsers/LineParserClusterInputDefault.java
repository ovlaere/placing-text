package be.ugent.intec.ibcn.geo.common.io.parsers;

import be.ugent.intec.ibcn.geo.common.datatypes.Point;

/**
 * This class provides a default implementation for a LineParserPoint.
 * 
 * In this case, we provide a simple parser that fetches the ID, lat and lon
 * from each of the training items.
 * 
 * @author Olivier Van Laere <oliviervanlaere@gmail.com>
 */
public class LineParserClusterInputDefault extends LineParserPoint {
    
    /**
     * Implementation of parse.
     */
    @Override
    public Point parse(String line) {
        // Prepare an empty DataItem.
        Point item = null;
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
            item = new Point(id, lat, lon);
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
