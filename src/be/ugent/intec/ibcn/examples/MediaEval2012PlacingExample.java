package be.ugent.intec.ibcn.examples;

import be.ugent.intec.ibcn.analyzer.AbstractAnalyzer;
import be.ugent.intec.ibcn.analyzer.AnalyzerParameters;
import be.ugent.intec.ibcn.analyzer.DistanceThresholdAnalyzer;
import be.ugent.intec.ibcn.geo.classifier.ClassifierParameters;
import be.ugent.intec.ibcn.geo.classifier.NaiveBayes;
import be.ugent.intec.ibcn.geo.clustering.AbstractClustering;
import be.ugent.intec.ibcn.geo.clustering.ClusteringParameters;
import be.ugent.intec.ibcn.geo.clustering.PamClustering;
import be.ugent.intec.ibcn.geo.clustering.PamParameters;
import be.ugent.intec.ibcn.geo.common.Util;
import be.ugent.intec.ibcn.geo.common.datatypes.Point;
import be.ugent.intec.ibcn.geo.common.io.ClusteringIO;
import be.ugent.intec.ibcn.geo.features.GeoSpreadFeatureRanker;
import be.ugent.intec.ibcn.referencing.AbstractReferencer;
import be.ugent.intec.ibcn.referencing.MultiLevelSimilarityReferencer;
import be.ugent.intec.ibcn.similarity.SimilarityIndexer;
import be.ugent.intec.ibcn.similarity.SimilarityParameters;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * This class implements a simplified version of the workflow used for the
 * run 1 configuration of the UG_CU submission to the MediaEval2012 Placing
 * Task (http://multimediaeval.org/mediaeval2012/placing2012/).
 * 
 * The details of the run configurations are described in 
 *   http://ceur-ws.org/Vol-927/mediaeval2012_submission_35.pdf
 * 
 * The workflow works as follows:
 *  1) 3 clustering configuration are made of the training data, 500, 2500 and
 *     10000 clusters (using Partition Around Medoids)
 *  2) The geospread feature ranking method is used to rank the features found
 *     in the training data.
 *  3) Language Models are created (using multinomial Naive Bayes classification)
 *     for the three clusterings. The configurations are 
 *      - k = 500  , # features = 1.5M, dirichlet mu value = 15000
 *      - k = 2500 , # features = 175K, dirichlet mu value = 12500
 *      - k = 10000, # features = 125K, dirichlet mu value =   500
 *  4) A multilevel referencer is run. If the classification assigned a test
 *     item to a certain class, without having any features at that level, we fall
 *     back to a coarser level, until we run out. Next, the most similar
 *     training item from that area is returned as the location estimate.
 * 
 * This implementation does not use any visual features at all. Also, the actual
 * run1 for placing2012 used metadata from for instance the textual home location
 * of the user of the Flickr photos, description and comment info, while in this
 * version, a training item without features is a lost cause, yielding worse results
 * than our actual submission. 
 * 
 * If you have signed the data agreement between MediaEval and yourself, you
 * can contact me to obtain the training and test set.
 * 
 * To give an idea of the performance of this workflow:
 * 
 * - clustering the training data (filtered to ~2.1M training items) into
 * 500, 2500 and 10000 clusters, takes about 1 hour in total on a 16-core server.
 * 
 * - the rest of the workflow (feature ranking, classification of the three models,
 * similarity search) takes 3min 28sec on a 2.4Ghz Core i5 (4 cores in hyperthreading), 
 * 16 GB for JVM -Xmx8g -Xmx16g and a SSD drive for storage.
 * 
 * @author Olivier Van Laere <oliviervanlaere@gmail.com>
 */
public class MediaEval2012PlacingExample {
    
    public static void main(String[] args) {
        
        // General parameters for the 2012 Placing Task run 1.
        
        int [] clusterings = new int[]{500, 2500, 10000};
        int [] featuresToRetain = new int []{1500000, 175000, 125000};
        int [] dirichletMus = new int []{15000, 12500, 500};

        // Shorthand for the dir prefix for the filenames
        String dataDir = "/"; // Your actual dataDir here
        
        // Provide the full path and filename of the files that will be used
        String trainingFile = dataDir + "training"; // Your actual training file here
        String testFile     = dataDir + "test"; // Your actual test file here
        String featureFile  = dataDir + "features.geo";
        String medoidTemplate = dataDir + "medoids.@1";
        String classificationTemplate = dataDir + "classification.@1";
        // Similarity index dir
        String indexTemplate = dataDir + "simindex.@1/";
        // Result file
        String resultFile = testFile + ".placing";
        
        // Parser classes
        String clusterParser = "be.ugent.intec.ibcn.geo.common.io.parsers.LineParserClusterPoint";
        String trainingParser = "be.ugent.intec.ibcn.geo.common.io.parsers.LineParserTrainingItem";
        String testParser = "be.ugent.intec.ibcn.geo.common.io.parsers.LineParserTestItem";
        // Enable the home parser to use the home prior
//        String testParser = "be.ugent.intec.ibcn.geo.common.io.parsers.LineParserTestItemHome";
        String medoidParser = "be.ugent.intec.ibcn.geo.common.io.parsers.LineParserMedoid";
        String similarityParser = "be.ugent.intec.ibcn.geo.common.io.parsers.LineParserTrainingSimilarity";
        
        // Just to use the suggested parameters for NB processing
        int memory_available_in_gb = 16;
        
        /**
         * Clustering
         */
        
        // For each of the clusterings
        for (int clusters : clusterings) {
            // Prepare the parameters - use the default values
            ClusteringParameters cp = new ClusteringParameters();
            // But we set out own input parser
            cp.setLineParserClassNameForInput(clusterParser);

            // Provide the full path and filename for the output
            String clusteringOutputFile = Util.applyTemplateValues(
                    medoidTemplate, new String[]{""+clusters});
            // If the clustering is not already found
            if (!(new File(clusteringOutputFile)).exists()) {
                // Prepare the ClusteringIO
                ClusteringIO cio = new ClusteringIO();

                /**
                * Load the input data
                */

                // Load all the data from the file to cluster
                Point [] data = cio.loadDataFromFile(trainingFile, 
                        cp.getLineParserClassNameForInput());

                PamParameters pp = new PamParameters();
                AbstractClustering clusteringPam = new PamClustering(pp, data, clusters);
                clusteringPam.cluster(clusteringOutputFile + "." + clusters);
            }            
        } // End clustering loop
        
        /**
         * Feature ranking
         */
        {
            // If the feature ranking is not already found on file
            if (!(new File(featureFile)).exists()) {
                // Geospread feature ranking
                GeoSpreadFeatureRanker gsf = new GeoSpreadFeatureRanker();
                gsf.process(trainingFile, trainingParser, -1, featureFile);
            }
        }
        
        /**
         * Classification
         */
        
        // For each of the clusterings
        for (int i = 0; i < clusterings.length; i++) {
            int clusters = clusterings[i];
            int features = featuresToRetain[i];
            int dirichletMu = dirichletMus[i];
        
            String classificationFile = Util.applyTemplateValues(
                    classificationTemplate, new String[]{""+clusters});
            String medoidFile = Util.applyTemplateValues(
                    medoidTemplate, new String[]{""+clusters});
            
            // If the classification is not already found on file
            if (!(new File(classificationFile)).exists()) {
            
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
                cp.setClassCount(clusters);
                cp.setFeatureCount(features);
                // Set smoothing method and parameter
                cp.setSmoothingMethod(NaiveBayes.SMOOTHING_DIRICHLET);
                cp.setDirichletMu(dirichletMu);
                // Set Prior mode and parameters
                cp.setPriorMode(NaiveBayes.PRIOR_MAX_LIKELIHOOD);
                // Optionally, choose for the home prior and set the weight
//                cp.setPriorMode(NaiveBayes.PRIOR_HOME);
//                cp.setHomeWeight(0.4);
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
        
        /**
         * Multilevel georeferencing
         */
        // If the resultfile does not yet exist
        if (!new File(resultFile).exists()) {
            // Prepare a list to store the different parameters
            List<SimilarityParameters> multi_parameters = new ArrayList<SimilarityParameters>();
            // For each of the configurations
            for (int i = 0; i < clusterings.length; i++) {
                int clusters = clusterings[i];

                String classificationFile = Util.applyTemplateValues(
                        classificationTemplate, new String[]{""+clusters});
                String medoidFile = Util.applyTemplateValues(
                        medoidTemplate, new String[]{""+clusters});
                String indexDir = Util.applyTemplateValues(
                        indexTemplate, new String[]{""+clusters});

                SimilarityParameters sp = new SimilarityParameters();
                // Set training file and parser
                sp.setTrainingFile(trainingFile);
                sp.setTrainingParser(similarityParser);
                // Set medoid file and parser
                sp.setMedoidFile(medoidFile);
                sp.setMedoidParser(medoidParser);
                // Set test file and parser
                sp.setTestFile(testFile);
                sp.setTestParser(testParser);

                // Set the output file
                sp.setClassificationFile(classificationFile);
                // Set the similarity index
                sp.setSimilarityDirectory(indexDir);
                // Set the number of similar items to retain
                sp.setSimilarItemsToConsider(1);
                // Init the parameters
                sp.init();

                // If the index does not already exist
                if (!new File(indexDir).exists()) {
                    // Create and init the index
                    SimilarityIndexer simindexer = new SimilarityIndexer(sp);
                    simindexer.index();
                }

                // Store the parameters
                multi_parameters.add(sp);
            }

            AbstractReferencer referencer = new MultiLevelSimilarityReferencer(multi_parameters);
            // Run referencing - output will go to file
            referencer.run(resultFile);

            // Optionally - remove the index after using it
            // As long as the clustering does not change, it is recommended to keep
            // it to speed things up

            for (SimilarityParameters sp : multi_parameters) {
                SimilarityIndexer simindexer = new SimilarityIndexer(sp);
                simindexer.removeIndex();
            }
        }
        
        /**
         * Analyze the results.
         */
        
        // Prepare the classifier parameters
        AnalyzerParameters ap = new AnalyzerParameters();
        // Set test file and parser
        ap.setTestFile(testFile);
        ap.setTestParser(testParser);
        // Init the parameters
        ap.init();
        
        AbstractAnalyzer analyzer = new DistanceThresholdAnalyzer(ap, 
                new double[]{0.001, 0.01, 0.1, 1, 5, 10, 50, 100, 1000, 10000, 40000});
        // Run referencing - output will go to file
        analyzer.run(resultFile);
    }
}