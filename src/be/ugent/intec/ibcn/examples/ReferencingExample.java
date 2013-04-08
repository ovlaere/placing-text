package be.ugent.intec.ibcn.examples;

import be.ugent.intec.ibcn.geo.classifier.ClassifierParameters;
import be.ugent.intec.ibcn.geo.classifier.NaiveBayes;
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
//        // Set training file and parser
//        rp.setTrainingFile("training");
//        rp.setTrainingParser("be.ugent.intec.ibcn.geo.common.io.parsers.LineParserTrainingItem");
        // Set medoid file and parser
        rp.setMedoidFile("medoids.2500");
        rp.setMedoidParser("be.ugent.intec.ibcn.geo.common.io.parsers.LineParserMedoid");
        // Set test file and parser
        rp.setTestFile("mediaeval2011plus");
        rp.setTestParser("be.ugent.intec.ibcn.geo.common.io.parsers.LineParserTestItem");
        // Set the output file
        rp.setOutputFile("nbPredictions.2011.nohome");
        // Init the parameters
        rp.init();
        
        /**
         * Example of a simple medoid referencer
         */
        
        AbstractReferencer referencer = new MedoidReferencer(rp);
        // Run referencing - output will go to file
        referencer.run("predictions.2011.nohome");
    }
}