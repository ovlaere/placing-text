package be.ugent.intec.ibcn.geo.common.interfaces;

import be.ugent.intec.ibcn.geo.common.datatypes.DataItem;
import java.util.Map;

/**
 * Generic data input parser interface for parsing DataItem objects used
 * for training and testing a classifier.
 * 
 * @author Olivier Van Laere <oliviervanlaere@gmail.com>
 */
public interface LineParserDataItem extends LineParser {
    
    /**
     * Set the map of features to use when loading the data.
     * @param features 
     */
    public void setFeatures(Map<Object, Integer> features);
    
    /**
     * Require implementations to implement a parse method
     * @param line Content from the input line
     * @return A DataItem that was parsed from the input
     */
    @Override
    public DataItem parse(String line);

}
