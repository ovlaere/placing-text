package be.ugent.intec.ibcn.geo.common.io.parsers;

import be.ugent.intec.ibcn.geo.common.datatypes.DataItem;
import be.ugent.intec.ibcn.geo.common.interfaces.AbstractLineParserDataItem;
import java.util.ArrayList;
import java.util.List;

/**
 * Default implementation for reading input data for training.
 * 
 * AbstractLineParserDataItem provides a Map of features to map the features
 * to IDs. Input data is converted to DataItem objects with ID, lat, lon
 * and an Object [] of features (by means of numeric IDs) after loading.
 * 
 * If the item has no features, it is discarded and null is returned. 
 * Please note that we cannot do this for test!
 * 
 * @see AbstractLineParserDataItem
 * 
 * @author Olivier Van Laere <oliviervanlaere@gmail.com>
 */
public class LineParserTrainingItem extends AbstractLineParserDataItem{

    /**
     * Constructor.
     */
    public LineParserTrainingItem() {
        super();
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

            // In case of no feature selection
            if (features == null)
                item = new DataItem(id, lat, lon, data);
            else {            
                // Prepare a List of selected features, by ID
                List<Integer> newdata = new ArrayList<Integer>();
                for (String s : data)
                    if (features.containsKey(s))
                        newdata.add(features.get(s));
                // If there are features
                if (newdata.size() > 0) {
                    item = new DataItem(id, lat, lon, newdata.toArray(
                            new Integer[0]));
                }
            }
            // Sanity check
            if (item != null && item.getData().length == 0)
                item = null;
        }
        catch (Exception e) {
            this.errors++;
        }
        this.processed++;
        return item;
    }
}