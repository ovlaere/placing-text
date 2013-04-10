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
        
        // Shorthand for the dir prefix for the filenames
        String dataDir = "/"; // Your actual dataDir here
        
        // Provide the full path and filename of the files that will be used
        String trainingFile = dataDir + "training"; // Your actual training file here
        String testFile     = dataDir + "test"; // Your actual test file here
        String featureFile  = dataDir + "features.geo"; // Your actual feature file here
        String medoidFile = dataDir + "medoids.@1"; // Your actual medoid file here
        String classificationFile = dataDir + "classification"; // Your actual output file here
        
        // Parser classes
        String trainingParser = "be.ugent.intec.ibcn.geo.common.io.parsers.LineParserTrainingItem";
        String testParser = "be.ugent.intec.ibcn.geo.common.io.parsers.LineParserTestItem";
        String medoidParser = "be.ugent.intec.ibcn.geo.common.io.parsers.LineParserMedoid";
        
        int classes = 2500; // Number of classes in you medoid file here
        int features = 175000; // Number of features you want to use here
        
        // Just to use the suggested parameters for NB processing
        int memory_available_in_gb = 16;
        
        // Prepare the classifier parameters
        ClassifierParameters cp = new ClassifierParameters();
        // Set training file and parser
        cp.setTrainingFile(trainingFile);
        cp.setTrainingParser(trainingParser);
        // Set medoid file and parser
        cp.setMedoidFile(medoidFile);
        cp.setMedoidParser(medoidParser);
        // Set test file and parser
        cp.setTestFile(testFile);
        cp.setTestParser(testParser);
        // Set the output file
        cp.setClassificationFile(classificationFile);
        // Set feature ranking to use
        cp.setFeatureFile(featureFile);
        // Set class and feature count
        cp.setClassCount(classes);
        cp.setFeatureCount(features);
        // Set smoothing method and parameter - adapt to your needs
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
        int suggested_batch = nb.getSuggestedBatchSize(memory_available_in_gb);

        // And set this before starting to classify
        nb.setNaiveBayesBatchSize(suggested_batch);

        // Start classifiying - output will go to file
        nb.classify();
    }
}