package be.ugent.intec.ibcn.geo.common.interfaces;

import be.ugent.intec.ibcn.geo.common.datatypes.DataItem;
import be.ugent.intec.ibcn.geo.common.io.parsers.LineParserPoint;
import java.util.Map;

/**
 * Abstract implementation of a DataItem input parser. This class should provide
 * a map that contains the features to use when loading the training data.
 * @author Olivier Van Laere <oliviervanlaere@gmail.com>
 */
public abstract class AbstractLineParserDataItem extends LineParserPoint 
                                             implements LineParserDataItem {
    
    /**
     * Map used to store the features used for loading the input data.
     */
    protected Map<Object, Integer> features = null;
    
    /**
     * Set the map of features to use when loading the data.
     * @param features 
     */
    @Override
    public void setFeatures(Map<Object, Integer> features) {
        this.features = features;
    }
    
    /**
     * Abstract definition of the parse method with a DataItem as a result.
     * @param line Content from the input line
     * @return A DataItem that was parsed from the input
     */
    @Override
    public abstract DataItem parse(String line);

}