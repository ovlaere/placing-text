package be.ugent.intec.ibcn.examples;

import be.ugent.intec.ibcn.geo.classifier.ClassifierParameters;
import be.ugent.intec.ibcn.geo.classifier.NaiveBayes;

/**
 * This class gives an example of how to combine a clustering and a feature 
 * ranking in a classification model. The result of the classification step, 
 * is written to file.
 * 
 * Depending on the amount of input data, you might want to set the -Xmx (and
 * most likely -Xms) VM parameters to a higher value than the default one.
 * 
 * In the example below, the memory value used is 16GB of ram.
 * 
 * @author Olivier Van Laere <oliviervanlaere@gmail.com>
 */
public class ClassifierExample {
    
    public static void main(String[] args) {
        // Prepare the classifier parameters
        ClassifierParameters cp = new ClassifierParameters();
        // Set training file and parser
        cp.setTrainingFile("training");
        cp.setTrainingParser("be.ugent.intec.ibcn.geo.common.io.parsers.LineParserTrainingItem");
        // Set medoid file and parser
        cp.setMedoidFile("medoids.2500");
        cp.setMedoidParser("be.ugent.intec.ibcn.geo.common.io.parsers.LineParserMedoid");
        // Set test file and parser
        cp.setTestFile("mediaeval2011plus");
        cp.setTestParser("be.ugent.intec.ibcn.geo.common.io.parsers.LineParserTestItem");
//        cp.setTestParser("be.ugent.intec.ibcn.geo.common.io.parsers.LineParserTestItemHome");
        // Set the output file
        cp.setOutputFile("nbPredictions.2011.nohome");
        // Set feature ranking to use
        cp.setFeatureFile("features.geo");
        // Set class and feature count
        cp.setClassCount(2500);
        cp.setFeatureCount(175000);
        // Set smoothing method and parameter
        cp.setSmoothingMethod(NaiveBayes.SMOOTHING_DIRICHLET);
        cp.setDirichletMu(1750);
        // Set Prior mode and parameters
        cp.setPriorMode(NaiveBayes.PRIOR_MAX_LIKELIHOOD);
//        cp.setHomeWeight(0.75);
        // Init the parameters
        cp.init();
        
        // Init the Naive Bayes Classifier
        NaiveBayes nb = new NaiveBayes(cp);

        // If you want to get an idea on a good batch size for the number of
        // classes and features you are about to use
        int suggested_batch = nb.getSuggestedBatchSize(16);

        // And set this before starting to classify
        nb.setNaiveBayesBatchSize(suggested_batch);

        // Start classifiying - output will go to file
        nb.classify();
    }
}