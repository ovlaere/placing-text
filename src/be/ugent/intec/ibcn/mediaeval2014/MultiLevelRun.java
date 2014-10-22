package be.ugent.intec.ibcn.mediaeval2014;

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
import be.ugent.intec.ibcn.referencing.MedoidReferencer;
import be.ugent.intec.ibcn.referencing.MultiLevelSimilarityReferencer;
import be.ugent.intec.ibcn.referencing.ReferencingParameters;
import be.ugent.intec.ibcn.referencing.SimilarityReferencer;
import be.ugent.intec.ibcn.similarity.SimilarityIndexer;
import be.ugent.intec.ibcn.similarity.SimilarityParameters;

import java.io.File;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

/**
 * This class implements a more or less copy of the 
 * 	 be.ugent.intec.ibcn.examples.MediaEval2012PlacingExample.java
 * which was run as a baseline for the MediaEval2014 placing task.
 * 
 * The details of the 2014 Placing Task submissions can be found at
 * 	 http://ceur-ws.org/Vol-1263/ 
 * 
 * The data for this run consisted of the MediaEval2014 Placing Task corpus:
 * 	- 5 000 000 training pictures, with only tags and machine tags (no visual here)
 *  -    25 000 training videos, with only tags and machine tags (no visual here)
 *  Training total: 5 025 000 lines
 *  -   500 000 test pictures, with only tags and machine tags (no visual here)
 *  -    10 000 test videos, with only tags and machine tags (no visual here)
 *  Test total: 510 000 lines
 *  
 *  This dataset is part of a larger, Creative Commons dataset released by 
 *  Yahoo Labs/Flickr, more info here: http://labs.yahoo.com/news/yfcc100m/
 * 
 * The workflow works as follows:
 * 
 *  1) 3 clustering configuration are made of the training data, 500, 2500 and
 *     10000 clusters (using Partition Around Medoids). Clustering the 5M training
 *     took
 *        500: 4070 s.
 *		 2500: 4999 s.
 *		10000: 4217 s.
 *     On a machine with 24 cores.
 *     
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
 * This implementation does not use any visual features at all.
 * 
 * If you want an exact copy of the dataset, you can obtain this with the
 * MediaEval consortium. 
 * http://www.multimediaeval.org/mediaeval2014/placing2014/
 *  
 * - the rest of the workflow (feature ranking, classification of the three models, 
 * similarity search on each individual level for reference, and then multilevel
 * referencing) takes around one hour on a 24 core 2.2Ghz, with 40 GB for 
 * JVM -Xmx20g -Xmx40g.
 * 
 * The workflow was run as:
 * 
 * 	nohup java -cp .:lib/kd -Xms20g -Xmx40g -ea -Dfile.encoding=UTF-8 
 *  be.ugent.intec.ibcn.mediaeval2014.MultiLevelRun $train $test $label 2>&1 > 
 *  log_mediaeval2014_multilevelrun_"$label".txt &
 *  
 *  The 3 parameters are training file, test file and a label, to be used to
 *  distinguish between different output files for different run configurations.
 * 
 * The training data was normalized to improve performance a bit. This process
 * consisted of:
 * 	- URL decoding the raw tag in UTF-8 format
 *  - Replacing all whitespaces by ""
 *  - Normalizing the tag using java.text.Normalizer
 *  - Retaining alphanumeric characters only
 *  
 *  The results using this training and test set, the normalized input data and this
 *  workflow below, are:
 * 
 * Error range (km)     % within    # within    # total
 *              0.001       00.11       552     510000
 *              0.01        00.34      1743     510000
 *              0.1         03.60     18350     510000
 *              1.0         17.68     90181     510000
 *              5.0         33.52    170967     510000
 *              10.0        39.17    199768     510000
 *              50.0        47.72    243352     510000
 *              100.0       50.66    258378     510000
 *              1000.0      61.96    315984     510000
 *              10000.0     90.52    461629     510000
 *              40000.0    100.00    510000     510000
 *              
 * Quartile error:
 * 
 * 25%	Q1	  02.09 km
 * 50%	Q2	  84.90 km (median)
 * 75%	Q3	4143.23 km
 * 
 * @author Olivier Van Laere <oliviervanlaere@gmail.com>
 */
public class MultiLevelRun {
    
    public static void main(String[] args) {
    	
    	System.out.println("MediaEval2014 3 level approach");
    	
    	if (args.length != 3) {
    		System.out.println("Missing argument: training test label");
    		System.exit(0);
    	}    	
    	
    	String trainingFile = args[0];
    	String testFile = args[1];
    	String label = args[2];
    	
        // General parameters 2014 Placing Task run 1.
        
      int [] clusterings = new int[]{500, 2500, 10000};
      int [] featuresToRetain = new int []{1500000, 175000, 125000};
      int [] dirichletMus = new int []{15000, 12500, 500};

        // Shorthand for the dir prefix for the filenames
        String dataDir = "./"; // Your actual dataDir here
        
        // Provide the full path and filename of the files that will be used
//        String trainingFile = dataDir + "training." + label; // Your actual training file here
//        String testFile     = dataDir + "test." + label; // Your actual test file here
        String featureFile  = dataDir + "features."+label+".geo";
        String medoidTemplate = dataDir + "medoids.@1";
        String classificationTemplate = dataDir + "classification."+label+".@1";
        // Similarity index dir
        String indexTemplate = dataDir + "simindex."+label+".@1/";
        // Result file
        String resultFile = testFile + "."+label+".placing";
        
        // Parser classes
        String clusterParser = "be.ugent.intec.ibcn.geo.common.io.parsers.LineParserClusterPoint";
        String trainingParser = "be.ugent.intec.ibcn.geo.common.io.parsers.LineParserTrainingItem";
        String testParser = "be.ugent.intec.ibcn.geo.common.io.parsers.LineParserTestItem";
        // Enable the home parser to use the home prior
//        String testParser = "be.ugent.intec.ibcn.geo.common.io.parsers.LineParserTestItemHome";
        String medoidParser = "be.ugent.intec.ibcn.geo.common.io.parsers.LineParserMedoid";
        String similarityParser = "be.ugent.intec.ibcn.geo.common.io.parsers.LineParserTrainingSimilarity";
        
        // Just to use the suggested parameters for NB processing
        int memory_available_in_gb = 40;
        
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
                clusteringPam.cluster(clusteringOutputFile);
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
                
                String resultFileMedoid = resultFile + ".medoid." + clusters;
                
                AbstractReferencer referencer_medoid = new MedoidReferencer(rp);
                // Run referencing - output will go to file
                referencer_medoid.run(resultFileMedoid);
                
                // Prepare the classifier parameters
                AnalyzerParameters ap = new AnalyzerParameters();
                // Set test file and parser
                ap.setTestFile(testFile);
                ap.setTestParser(testParser);
                // Init the parameters
                ap.init();
                System.out.println("Analyzing " + resultFileMedoid);
                AbstractAnalyzer analyzer = new DistanceThresholdAnalyzer(ap, 
                        new double[]{0.001, 0.01, 0.1, 1, 5, 10, 50, 100, 1000, 10000, 40000});
                // Run referencing - output will go to file
                analyzer.run(resultFileMedoid);
                
                SimilarityParameters sp1 = new SimilarityParameters(rp);
                // Set a LineParserDataItemSimilarity implementation for training parser
                sp1.setTrainingParser(similarityParser);
                // Set the similarity index
                sp1.setSimilarityDirectory(indexDir);
                // Set the number of similar items to retain
                sp1.setSimilarItemsToConsider(1);
                // Init the parameters
                sp1.init();
                
                String resultFileSim = resultFile + ".sim." + clusters;
                
                if (!new File(resultFileSim).exists()) {
	                AbstractReferencer referencer_sim = new SimilarityReferencer(sp1);
	                // Run referencing - output will go to file
	                referencer_sim.run(resultFileSim);
                }
	                
                System.out.println("Analyzing " + resultFileSim);
                // Run referencing - output will go to file
                analyzer.run(resultFileSim);
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