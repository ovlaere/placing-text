package be.ugent.intec.ibcn.geo.common.io.parsers;

import be.ugent.intec.ibcn.geo.common.datatypes.DataItem;
import java.util.ArrayList;
import java.util.List;

/**
 * TODO Add comment
 * @author Olivier Van Laere <oliviervanlaere@gmail.com>
 */
public class LineParserTrainingDefault extends LineParserDataItem{

    public LineParserTrainingDefault() {
        super();
    }
        
    @Override
    public DataItem parse(String line) {
        DataItem item = null;
        try {
            String [] values = pattern_comma.split(line.toLowerCase());
            int id = Integer.parseInt(values[0]);
            double lat = Double.parseDouble(values[2]);
            double lon = Double.parseDouble(values[3]);
            String [] data = pattern_space.split(values[4]);
            // Prevent empty tags
            if (data.length == 1 && data[0].equals(""))
                data = new String[0];
            
            // Checking against a filter
            if (filter != null) {
                boolean hit = false;
                // Search for at least 1 tag in common with the filter set
                for (Object f : data) {
                    if (filter.contains((String)f)) {
                        hit = true;
                        break;
                    }
                }
                // Training item should be loaded
                if (hit) {
                    // without feature selection
                    if (features == null) {
                        item = new DataItem(id, lat, lon, data);
                    }
                    // with feature selection
                    else {
                        List<String> newdata = new ArrayList<String>();
                        for (String s : data)
                            if (features.containsKey(s))
                                newdata.add(s);
                        if (newdata.size() > 0) {
                            item = new DataItem(id, lat, lon, newdata.toArray(new String[0]));
                        }
                    }
                }
            }
            // No filtering
            else {
                // Load as String
                if (features == null) {
                    item = new DataItem(id, lat, lon, data);
                }
                // Load using feature selection
                else {
                    List<Integer> newdata = new ArrayList<Integer>();
                    for (String s : data)
                        if (features.containsKey(s))
                            newdata.add(features.get(s));
                    if (newdata.size() > 0) {
                        item = new DataItem(id, lat, lon, newdata.toArray(new Integer[0]));
                    }
                }
            }
            if (item.getData().length == 0)
                return null;
        }
        catch (Exception e) {
            this.errors++;
        }
        this.processed++;
        return item;
    }
}