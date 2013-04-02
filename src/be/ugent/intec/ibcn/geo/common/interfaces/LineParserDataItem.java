package be.ugent.intec.ibcn.geo.common.interfaces;

import be.ugent.intec.ibcn.geo.common.datatypes.DataItem;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 *
 * @author Olivier Van Laere <oliviervanlaere@gmail.com>
 */
public abstract class LineParserDataItem extends LineParserPoint {
    
    protected Set<String> filter = null;
    
    public void setFilter(Set<String> filter) {
        this.filter = filter;
    }      
 
    protected Map<Object, Integer> features = null;
    
    public void setFeatures(Map<Object, Integer> features) {
        this.features = features;
    }
    
    @Override
    public abstract DataItem parse(String line);

}