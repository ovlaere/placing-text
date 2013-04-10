package be.ugent.intec.ibcn.examples;

import be.ugent.intec.ibcn.geo.common.Util;
import be.ugent.intec.ibcn.referencing.AbstractReferencer;
import be.ugent.intec.ibcn.referencing.MedoidReferencer;
import be.ugent.intec.ibcn.referencing.ReferencingParameters;
import be.ugent.intec.ibcn.referencing.SimilarityReferencer;
import be.ugent.intec.ibcn.similarity.SimilarityIndexer;
import be.ugent.intec.ibcn.similarity.SimilarityParameters;

/**
 * This class provides a simple example of how to use the result from classification
 * and convert them to actual location predictions for the test items.
 * 
 * Two ways of converting predictions into coordinates are covered in this 
 * file: medoid conversion and using similarity search.
 * 
 * @author Olivier Van Laere <oliviervanlaere@gmail.com>
 */
public class ReferencingExample {
    
    public static void main(String[] args) {
        
        // Shorthand for the dir prefix for the filenames
        String dataDir = "/"; // Your actual dataDir here
        
        // Provide the full path and filename of the files that will be used
        String trainingFile = dataDir + "training"; // Your actual training file here
        String testFile     = dataDir + "test"; // Your actual test file here
        String medoidFile = dataDir + "medoids";
        String classificationFile = dataDir + "classification";
        String resultFiletemplate = testFile + ".placing.@1";
        // Similarity index dir
        String indexDir = dataDir + "simindex/";
        
        // Parser classes
        String trainingParser = "be.ugent.intec.ibcn.geo.common.io.parsers.LineParserTrainingItem";
        String testParser = "be.ugent.intec.ibcn.geo.common.io.parsers.LineParserTestItem";
        String medoidParser = "be.ugent.intec.ibcn.geo.common.io.parsers.LineParserMedoid";
        String similarityParser = "be.ugent.intec.ibcn.geo.common.io.parsers.LineParserTrainingSimilarity";
        
        // Just go for the most similar item
        int similar_items_to_consider = 1;
        
        // Prepare the referencing/similarity parameters
        ReferencingParameters rp = new ReferencingParameters();
        // Set training file and parser - optional, not necessary for simple
        // referencing
        rp.setTrainingFile(trainingFile);
        rp.setTrainingParser(trainingParser);
        // Set medoid file and parser
        rp.setMedoidFile(medoidFile);
        rp.setMedoidParser(medoidParser);
        // Set test file and parser
        rp.setTestFile(testFile);
        rp.setTestParser(testParser);
        // Set the classification file
        rp.setClassificationFile(classificationFile);
        // Init the parameters
        rp.init();
        
        /**
         * Example of a simple medoid referencer
         */
        
        AbstractReferencer referencer_medoid = new MedoidReferencer(rp);
        // Run referencing - output will go to file
        referencer_medoid.run(Util.applyTemplateValues(resultFiletemplate, new String[]{"medoid"}));
     
        /**
         * Example of a similarity referencer
         */
        
        SimilarityParameters sp = new SimilarityParameters(rp);
        // Set a LineParserDataItemSimilarity implementation for training parser
        sp.setTrainingParser(similarityParser);
        // Set the similarity index
        sp.setSimilarityDirectory(indexDir);
        // Set the number of similar items to retain
        sp.setSimilarItemsToConsider(similar_items_to_consider);
        // Init the parameters
        sp.init();
        
        // Create and init the index
        SimilarityIndexer simindexer = new SimilarityIndexer(sp);
        simindexer.index();
        
        AbstractReferencer referencer_sim = new SimilarityReferencer(sp);
        // Run referencing - output will go to file
        referencer_sim.run(Util.applyTemplateValues(resultFiletemplate, new String[]{"sim"}));
        
        // Optionally - remove the index after using it
        // As long as the clustering does not change, it is recommended to keep
        // it to speed things up
        simindexer.removeIndex();
    }
}