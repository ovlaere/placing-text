package be.ugent.intec.ibcn.examples;

import be.ugent.intec.ibcn.referencing.AbstractReferencer;
import be.ugent.intec.ibcn.referencing.MedoidReferencer;
import be.ugent.intec.ibcn.referencing.ReferencingParameters;

/**
 * TODO Add comment
 * 
 * @author Olivier Van Laere <oliviervanlaere@gmail.com>
 */
public class ReferencingExample {
    
    public static void main(String[] args) {
        // Prepare the classifier parameters
        ReferencingParameters rp = new ReferencingParameters();
//        // Set training file and parser - optional, not necessary for simple
        // referencing
//        rp.setTrainingFile("<training_file_here>");
//        rp.setTrainingParser("be.ugent.intec.ibcn.geo.common.io.parsers.LineParserTrainingItem");
        // Set medoid file and parser
        rp.setMedoidFile("<clustering_input_here>");
        rp.setMedoidParser("be.ugent.intec.ibcn.geo.common.io.parsers.LineParserMedoid");
        // Set test file and parser
        rp.setTestFile("<test_file_here>");
        rp.setTestParser("be.ugent.intec.ibcn.geo.common.io.parsers.LineParserTestItem");
        // Set the classification file
        rp.setClassificationFile("<classification_output_file_here>");
        // Init the parameters
        rp.init();
        
        /**
         * Example of a simple medoid referencer
         */
        
        AbstractReferencer referencer = new MedoidReferencer(rp);
        // Run referencing - output will go to file
        referencer.run("<location_prediction_output_file_here>");
    }
}