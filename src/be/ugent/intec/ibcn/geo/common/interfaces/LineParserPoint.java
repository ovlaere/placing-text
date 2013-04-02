package be.ugent.intec.ibcn.geo.common.interfaces;

import be.ugent.intec.ibcn.geo.common.datatypes.Point;
import java.util.regex.Pattern;

/**
 *
 * @author Olivier Van Laere <oliviervanlaere@gmail.com>
 */
public abstract class LineParserPoint {
    
    /**
     * Precompiled regex space split pattern significantly boosts performance.
     * (compared to regular string.split method). Stored here as it is commonly
     * used by parser implementations.
     */
    protected Pattern pattern_space = Pattern.compile(" ");
    
    /**
     * Precompiled regex comma split pattern significantly boosts performance.
     * (compared to regular string.split method). Stored here as it is commonly
     * used by parser implementations.
     */
    protected Pattern pattern_comma = Pattern.compile(",");

    protected int processed = 0;

    public int getProcessed() {
        return this.processed;
    }
    
    protected int errors = 0;
    
    public int getErrors() {
        return this.errors;
    }
    
    public abstract Point parse(String line);

}