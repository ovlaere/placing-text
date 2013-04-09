package be.ugent.intec.ibcn.geo.common.interfaces;

import be.ugent.intec.ibcn.geo.common.datatypes.DataItem;
import java.util.Set;

/**
 * Generic data input parser interface for parsing DataItem objects used
 * for similarity purposes.
 * 
 * @author Olivier Van Laere <oliviervanlaere@gmail.com>
 */
public interface LineParserDataItemSimilarity {
    
    /**
     * @param filter Set a set of features to use for filtering the data
     * when loading. If no set is set, or if at least one features matches
     * the filter set, the item should be loaded.
     */
    public void setFilter(Set<String> filter);
    
    /**
     * Actual parse implementation.
     * @param line The String input line from file
     * @return An instantiated DataItem or null if no tags were present
     */
    public DataItem parse(String line);
}