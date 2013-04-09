package be.ugent.intec.ibcn.examples;

import be.ugent.intec.ibcn.referencing.AbstractReferencer;
import be.ugent.intec.ibcn.referencing.MedoidReferencer;
import be.ugent.intec.ibcn.referencing.ReferencingParameters;
import be.ugent.intec.ibcn.referencing.SimilarityReferencer;
import be.ugent.intec.ibcn.similarity.SimilarityIndexer;
import be.ugent.intec.ibcn.similarity.SimilarityParameters;

/**
 * TODO Add comment
 * 
 * @author Olivier Van Laere <oliviervanlaere@gmail.com>
 */
public class ReferencingExample {
    
    public static void main(String[] args) {
        // Prepare the referencing/similarity parameters
        ReferencingParameters rp = new ReferencingParameters();
        // Set training file and parser - optional, not necessary for simple
        // referencing
        rp.setTrainingFile("github/training");
        rp.setTrainingParser("be.ugent.intec.ibcn.geo.common.io.parsers.LineParserTrainingItem");
        // Set medoid file and parser
        rp.setMedoidFile("github/medoids.2500");
        rp.setMedoidParser("be.ugent.intec.ibcn.geo.common.io.parsers.LineParserMedoid");
        // Set test file and parser
        rp.setTestFile("github/dev");
        rp.setTestParser("be.ugent.intec.ibcn.geo.common.io.parsers.LineParserTestItem");
        // Set the classification file
        rp.setClassificationFile("github/dev.classification");
        // Init the parameters
        rp.init();
        
        /**
         * Example of a simple medoid referencer
         */
        
        AbstractReferencer referencer_medoid = new MedoidReferencer(rp);
        // Run referencing - output will go to file
        referencer_medoid.run("github/dev.placing.medoid");
     
        /**
         * Example of a similarity referencer
         */
        
        SimilarityParameters sp = new SimilarityParameters(rp);
        // Set a LineParserDataItemSimilarity implementation for training parser
        sp.setTrainingParser("be.ugent.intec.ibcn.geo.common.io.parsers.LineParserTrainingSimilarity");
        // Set the similarity index
        sp.setSimilarityDirectory("github/simindex.2500/");
        // Set the number of similar items to retain
        sp.setSimilarItemsToConsider(1);
        // Init the parameters
        sp.init();
        
        // Create and init the index
        SimilarityIndexer simindexer = new SimilarityIndexer(sp);
        simindexer.index();
        
        AbstractReferencer referencer_sim = new SimilarityReferencer(sp);
        // Run referencing - output will go to file
        referencer_sim.run("github/dev.placing.sim");
        
        // Optionally - remove the index after using it
        // As long as the clustering does not change, it is recommended to keep
        // it to speed things up
        simindexer.removeIndex();
        
        /**
         * TODO Multi-level with similarity?
         */
    }
}