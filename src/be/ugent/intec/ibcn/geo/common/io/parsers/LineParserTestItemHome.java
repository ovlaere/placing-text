package be.ugent.intec.ibcn.geo.common.io.parsers;

import be.ugent.intec.ibcn.geo.common.datatypes.DataItem;
import be.ugent.intec.ibcn.geo.common.datatypes.DataItemHome;
import be.ugent.intec.ibcn.geo.common.datatypes.Point;
import be.ugent.intec.ibcn.geo.common.interfaces.AbstractLineParserDataItem;
import be.ugent.intec.ibcn.geo.common.interfaces.LineParserDataItem;
import be.ugent.intec.ibcn.geo.common.interfaces.LineParserDataItemHome;
import java.util.ArrayList;
import java.util.List;

/**
 * Extended implementation for reading input data for test, with potential clues
 * about the home location of the user.
 * 
 * AbstractLineParserDataItem provides a Map of features to map the tags
 * to IDs. Input data is converted to DataItem objects with ID, lat, lon
 * and an Object [] of tags (by means of numeric IDs) after loading.
 * 
 * If the item has no tags, we still load this items, as we cannot discard this
 * item, in contrast to training data.
 * 
 * @see AbstractLineParserDataItem
 * @see LineParserTrainingItem
 * @see LineParserDataItem
 * @see LineParserDataItemHome
 * 
 * @author Olivier Van Laere <oliviervanlaere@gmail.com>
 */
public class LineParserTestItemHome extends AbstractLineParserDataItem 
                                    implements LineParserDataItemHome {

    /**
     * Constructor.
     */
    public LineParserTestItemHome() {
        super();
    }

    /**
     * Actual parse implementation.
     * @param line The String input line from file
     * @return An instantiated DataItem with the test data
     */
    @Override
    public DataItemHome parse(String line) {
        // Prepare the return result
        DataItemHome item = null;
        try {
            // Parse the line
            String [] values = pattern_comma.split(line.toLowerCase());
            int id = Integer.parseInt(values[0]);
            double lat = Double.parseDouble(values[2]);
            double lon = Double.parseDouble(values[3]);
            
            // Split the tags
            String [] data = null;
            if (values.length >= 5)
                data = pattern_space.split(values[4]);
            
            // Prevent empty tags
            if (data == null || (data.length == 1 && data[0].equals("")))
                data = new String[0];
            // In case of no feature selection
            if (features == null)
                item = new DataItemHome(id, lat, lon, data);
            else {
                // Prepare a List of selected features, by ID
                List<Integer> newdata = new ArrayList<Integer>();
                for (String s : data)
                    if (features.containsKey(s))
                        newdata.add(features.get(s));
                // Get the result ready
                item = new DataItemHome(id, lat, lon, newdata.toArray(new Integer[0]));
            }
            
            // Fetch lat/lon
            if (values.length >= 8 &&
                    values[6].length() > 0 && values[7].length() > 0) {
                double home_lat = Double.parseDouble(values[6]);
                double home_lon = Double.parseDouble(values[7]);
                Point home = new Point(-1, home_lat, home_lat);
                item.setHomeLocation(home);
            }
        }
        catch (Exception e) {
            this.errors++;
        }
        this.processed++;
        return item;
    }
}