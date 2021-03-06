package be.ugent.intec.ibcn.analyzer;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.ugent.intec.ibcn.geo.common.datatypes.DataItem;
import be.ugent.intec.ibcn.geo.common.io.DataLoading;

/**
 * Abstract class with the basics for analyzing location predictions.
 * 
 * In order to analyze the results, the test data needs to be loaded. Therefore
 * all extentions of this are automatically provided with the loaded test data.
 * 
 * Analyzers should implement the run method, with a filename to analyze.
 * 
 * @author Olivier Van Laere <oliviervanlaere@gmail.com>
 */
public abstract class AbstractAnalyzer {
    
	/**
	 * Logger.
	 */
	protected static final Logger LOG = LoggerFactory.getLogger(AbstractAnalyzer.class);
	
    /**
     * Numberformat.
     */
    protected  static final NumberFormat formatter = 
            new DecimalFormat("#00.00");

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
        LOG.info("Loading test from {}", parameters.getTestFile());
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