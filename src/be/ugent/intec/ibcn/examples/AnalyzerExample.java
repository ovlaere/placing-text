package be.ugent.intec.ibcn.examples;

import be.ugent.intec.ibcn.analyzer.AbstractAnalyzer;
import be.ugent.intec.ibcn.analyzer.AnalyzerParameters;
import be.ugent.intec.ibcn.analyzer.DistanceThresholdAnalyzer;

/**
 * TODO Add comment
 * 
 * @author Olivier Van Laere <oliviervanlaere@gmail.com>
 */
public class AnalyzerExample {
    
    public static void main(String[] args) {
        // Prepare the classifier parameters
        AnalyzerParameters ap = new AnalyzerParameters();
        // Set medoid file and parser - optional
        ap.setMedoidFile("<clustering_input_here>");
        ap.setMedoidParser("be.ugent.intec.ibcn.geo.common.io.parsers.LineParserMedoid");
        // Set test file and parser
        ap.setTestFile("<test_file_here>");
        ap.setTestParser("be.ugent.intec.ibcn.geo.common.io.parsers.LineParserTestItem");
        // Init the parameters
        ap.init();
        
        /**
         * Example of a simple distance analyzer.
         */
        AbstractAnalyzer analyzer = new DistanceThresholdAnalyzer(ap, 
                new double[]{0.001, 0.01, 0.1, 1, 5, 10, 50, 100, 1000, 10000, 40000});
        // Run referencing - output will go to file
        analyzer.run("<location_prediction_output_file_here>");
    }
}