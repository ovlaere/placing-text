package be.ugent.intec.ibcn.examples;

import be.ugent.intec.ibcn.analyzer.AbstractAnalyzer;
import be.ugent.intec.ibcn.analyzer.AnalyzerParameters;
import be.ugent.intec.ibcn.analyzer.DistanceThresholdAnalyzer;

/**
 * This class covers the basics of analyzing location predictions (actual 
 * coordinates) against the ground truth. An example is given of the usage of
 * a DistanceThresholdAnalyzer which allows to analyze the number of predictions
 * that are correct within certain thresholds (e.g. # items within 1km, 10km, ...).
 * 
 * A clustering file is required if you want to analyze accuracy of the 
 * classifier (i.e. # prediction in the correct class) as well.
 * 
 * @author Olivier Van Laere <oliviervanlaere@gmail.com>
 */
public class AnalyzerExample {
    
    public static void main(String[] args) {
        
        // Shorthand for the dir prefix for the filenames
        String dataDir = "/"; // Your actual dataDir here
        
        // Provide the full path and filename of the files that will be used
        String testFile     = dataDir + "test"; // Your actual test file here
        String medoidFile = dataDir + "medoids"; // Your medoid file here
        // Result file
        String resultFile = testFile + ".placing"; // Your result file here
        
        // Parser classes
        String testParser = "be.ugent.intec.ibcn.geo.common.io.parsers.LineParserTestItem";
        String medoidParser = "be.ugent.intec.ibcn.geo.common.io.parsers.LineParserMedoid";
        
        // Prepare the classifier parameters
        AnalyzerParameters ap = new AnalyzerParameters();
        // Set medoid file and parser - optional
        ap.setMedoidFile(medoidFile);
        ap.setMedoidParser(medoidParser);
        // Set test file and parser
        ap.setTestFile(testFile);
        ap.setTestParser(testParser);
        // Init the parameters
        ap.init();
        
        /**
         * Example of a simple distance analyzer.
         */
        AbstractAnalyzer analyzer = new DistanceThresholdAnalyzer(ap, 
                new double[]{0.001, 0.01, 0.1, 1, 5, 10, 50, 100, 1000, 10000, 40000});
        // Run referencing - output will go to file
        analyzer.run(resultFile);
    }
}