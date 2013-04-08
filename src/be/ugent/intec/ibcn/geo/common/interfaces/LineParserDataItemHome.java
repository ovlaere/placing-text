package be.ugent.intec.ibcn.geo.common.interfaces;

import be.ugent.intec.ibcn.geo.common.datatypes.DataItemHome;

/**
 * Generic data input parser interface.
 * 
 * @author Olivier Van Laere <oliviervanlaere@gmail.com>
 */
public interface LineParserDataItemHome extends LineParserDataItem {
    
    /**
     * Require implementations to implement a parse method
     * @param line Content from the input line
     * @return A DataItemHome that was parsed from the input
     */
    @Override
    public DataItemHome parse(String line);

}
