package be.ugent.intec.ibcn.analyzer;

import be.ugent.intec.ibcn.geo.common.datatypes.DataItem;
import be.ugent.intec.ibcn.geo.common.io.DataLoading;
import java.text.DecimalFormat;
import java.text.NumberFormat;

/**
 * TODO Add comment
 * 
 * @author Olivier Van Laere <oliviervanlaere@gmail.com>
 */
public abstract class AbstractAnalyzer {
    
    /**
     * Numberformat.
     */
    protected  static final NumberFormat formatter = new DecimalFormat("#00.00");

    /**
     * Parameters to use for analyzing.
     */
    protected AnalyzerParameters parameters;
    
    /**
     * The test data.
     */
    protected DataItem [] test_data;
    
    /**
     * Constructor.
     * @param parameters Parameters for analyzing
     */
    public AbstractAnalyzer(AnalyzerParameters parameters) {
        this.parameters = parameters;
        // Load the test data
        System.out.println("Loading test from " + parameters.getTestFile());
        DataLoading dl = new DataLoading();
        // Data is loaded WITHOUT feature selection
        this.test_data = dl.loadDataFromFile(parameters.getTestFile(), 
                parameters.getTestParser(), parameters.getTestLimit(), null);
    }
    
    /**
     * Actual location analysis. This method assumes the necessary setup 
     * has been carried out before this method is called.
     * @param inputfile A file containing test ID <lat,lon> pairs to analyze.
     */
    public abstract void run(String inputfile);
    
}