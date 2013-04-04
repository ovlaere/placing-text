package be.ugent.intec.ibcn.geo.common.interfaces;

import be.ugent.intec.ibcn.geo.common.datatypes.Point;

/**
 * Generic input data parser interface.
 * 
 * @author Olivier Van Laere <oliviervanlaere@gmail.com>
 */
public interface LineParser {
    
    /**
     * Require implementations to implement a parse method
     * @param line Content from the input line
     * @return A point that was parsed from the input
     */
    public Point parse(String line);

    /**
     * @return the number of lines processed by the parser.
     */
    public int getProcessed();

    /**
     * @return the number of parse errors encountered the parser.
     */
    public int getErrors();
}
