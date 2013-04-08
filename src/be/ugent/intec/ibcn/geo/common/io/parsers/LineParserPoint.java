package be.ugent.intec.ibcn.geo.common.io.parsers;

import be.ugent.intec.ibcn.geo.common.datatypes.Point;
import be.ugent.intec.ibcn.geo.common.interfaces.LineParser;
import java.util.regex.Pattern;

/**
 * Default abstract parser definition.
 * @author Olivier Van Laere <oliviervanlaere@gmail.com>
 */
public abstract class LineParserPoint implements LineParser {
    
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

    /**
     * Counts the number of lines processed.
     */
    protected int processed = 0;

    /**
     * @return the number of lines processed by the parser.
     */
    @Override    
    public int getProcessed() {
        return this.processed;
    }
    
    /**
     * Counts the number of parse errors encountered.
     */
    protected int errors = 0;
    
    /**
     * @return the number of parse errors encountered the parser.
     */
    @Override
    public int getErrors() {
        return this.errors;
    }
    
    /**
     * Abstract definition of the required parse method
     * @param line Content from the input line
     * @return A point that was parsed from the input
     */
    @Override
    public abstract Point parse(String line);
}