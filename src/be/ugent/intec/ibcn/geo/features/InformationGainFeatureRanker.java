package be.ugent.intec.ibcn.geo.features;

import be.ugent.intec.ibcn.geo.common.Util;
import be.ugent.intec.ibcn.geo.common.datatypes.GeoClass;
import be.ugent.intec.ibcn.geo.common.io.FeaturesIO;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * This class implements the ranking of features using Information Gain. 
 * 
 * For details on the algorithm
 * @see http://www.sciencedirect.com/science/article/pii/S002002551300162X#s0080
 *
 * @author Olivier Van Laere <oliviervanlaere@gmail.com>
 */
public class InformationGainFeatureRanker extends AbstractClassLevelRanker {

    /**
     * @return the name of this ranking method
     */
    protected String getMethodName() {
        return "Information Gain";
    }
    
    /**
     * Overall entropy value used in calculations.
     */
    private double entropy = 0;

    /**
     * Constructor.
     * @param inputfile Input file with the training data
     * @param inputParser Parser implementation to use for the input data
     * @param medoidfile Input file with the medoids (cluster centra)
     * @param medoidParser Parser implementation to use for the medoid data 
     */
    public InformationGainFeatureRanker(String inputfile, String inputParser, 
            String medoidfile, String medoidParser) {
        super(inputfile, inputParser, medoidfile, medoidParser);
    }
    
    /**
     * Holds, for each classID, a feature-count map.
     */
    private Map<Integer, Map<Object, Integer>> area_feature_count_map;

    /**
     * Holds, for each classID, the total feature count in the class.
     */
    private Map<Integer, Integer> area_totalcount;

    /**
     * Actual Information gain feature selection (and convert it to a ranking).
     */
    public void process(String outputfile) {
        // init overall entropy stuff
        init();
        System.out.println("Doing "+ getMethodName() +" feature selection.");
        // Calculate actual Information Gain for each feature
        Map<Object, Double> information_gain = new HashMap<Object, Double>();
        // Start timer
        long start = System.currentTimeMillis();
        // Prepare a thread pool
        ExecutorService executor = Executors.newFixedThreadPool(NR_THREADS);
        // Prepare a list for the futures
        List<Future<IGResult>> list = new ArrayList<Future<IGResult>>();
        // For each of the features to process
        for (Object feature : this.otc.getFeatures()) {
            // Sanity check
            if (((String)feature).trim().length() > 0 && 
                    !((String)feature).equals("")) {
                // Instantiate a callable, track the future, execute
                Callable<IGResult> worker = new InformationGainHelper(feature);
                Future<IGResult> submit = executor.submit(worker);
                list.add(submit);
            }
        }
        int counter = 0;
        // Now retrieve the results form the futures
        for (Future<IGResult> future : list) {
            try {
                IGResult result = future.get();
                information_gain.put(result.getFeature(), result.getIG());
                // Report progress as we go
                if (++counter % 10000 == 0)
                    System.out.println(counter);
            }
            catch (Exception e) {
                System.err.println("Exception: " + e.getMessage());
                System.exit(1);
            }
        }
        // This will make the executor accept no new threads
        // and finish all existing threads in the queue
        executor.shutdown();
        // Wait until all threads are finish
        while (!executor.isTerminated()) {}
        // Sort the features by their IG value
        information_gain = Util.sortByValueDescending(information_gain);
        // Fetch the features
        List<Object> features = new ArrayList(information_gain.keySet());
        // Export the features to file
        FeaturesIO.exportFeaturesToFile(features, outputfile);
        // Stop the timer
        long stop = System.currentTimeMillis();
        System.out.println("Retained features: " + features.size() + 
                " ("+(stop-start)+" ms.)");
    }

    /**
     * Helper method for initializing some overall lookup data.
     */
    private void init() {
        // Start the timer
        long t1 = System.currentTimeMillis();
        System.out.println("Creating overall lookup map");
        area_feature_count_map = new HashMap<Integer, Map<Object, Integer>>();
        area_totalcount = new HashMap<Integer, Integer>();
        // For each GeoClass - calculate the overall entropy on the fly
        for (GeoClass geoclass : classmapper.getClasses()) {
            // Create a map with the feature occurrences
            Map<Object, Integer> area_feature_count = 
                    this.otc.getClassFeatureCount(geoclass, data);
            // Calculate the number of occurences
            int area_features = 0;
            for (int count : area_feature_count.values())
                area_features += count;
            // Store data for each of the GeoClasses
            area_feature_count_map.put(geoclass.getId(), area_feature_count);
            area_totalcount.put(geoclass.getId(), area_features);
            // Incorporate this info into the entropy
            double p = area_features * 1. / 
                    this.otc.getTotalFeatureOccurrences();
            entropy += p * Math.log(p);
        }
        // entropy definition
        entropy *= -1;
        // Stop timer
        long t2 = System.currentTimeMillis();
        System.out.println("Done. time: " + (t2-t1) + " ms.");
    }
    
    /**
     * Callable for the actual Information Gain calculation for a feature.
     */
    private class InformationGainHelper implements Callable<IGResult> {
        
        /**
         * The feature to process.
         */
        private Object feature;
        
        /**
         * Constructor.
         * @param feature The feature to process.
         */
        public InformationGainHelper(Object feature) {
            this.feature = feature;
        }
        
        /**
         * The actual Information Gain score calculation for a given feature.
         * @return an IGResult object, containing the feature and its IG value
         * @throws Exception 
         */
        @Override
        public IGResult call() throws Exception{
            double p_t_part = 0;
            double p_t_overline_part = 0;

            double p_t = (otc.getFeatureCount(feature) * 1.) / 
                    (otc.getTotalFeatureOccurrences() * 1.);
            double p_t_overline = 1 - p_t;

            for (int classId = 0; classId < classmapper.size(); classId++) {
                Integer count = area_feature_count_map.get(classId).get(
                        feature);
                if (count == null)
                    count = 0;
                int feature_occurences_in_a = count;
                int not_feature_occurences_in_a = area_totalcount.get(classId) - 
                        feature_occurences_in_a;

                double p_a_t = (feature_occurences_in_a * 1.) /
                        (otc.getFeatureCount(feature) * 1.);
                double p_a_t_overline = (not_feature_occurences_in_a * 1.) / 
                        (otc.getTotalFeatureOccurrences() - 
                            otc.getFeatureCount(feature) * 1.);

                if (p_a_t > 0)
                    p_t_part += p_a_t * Math.log(p_a_t);

                p_t_overline_part += p_a_t_overline * Math.log(p_a_t_overline);
            }
            IGResult result = new IGResult(this.feature, entropy + 
                    p_t * p_t_part + p_t_overline * p_t_overline_part);
            // Return the result
            return result;            
        }        
    }

    /**
     * This small class represents a two valued result of the IG calculation.
     */
    private class IGResult {

        /**
         * The feature that was processed.
         */
        private Object feature;

        /**
         * Information Gain of the feature that was processed.
         */
        private double ig;

        /**
         * Constructor.
         * @param feature The feature that was processed.
         * @param ig Information Gain of the feature that was processed.
         */
        public IGResult(Object feature, double ig) {
            this.feature = feature;
            this.ig = ig;
        }
        
        /**
         * @return The feature that was processed.
         */
        public Object getFeature() {
            return feature;
        }

        /**
         * @return Information Gain of the feature that was processed.
         */
        public double getIG() {
            return ig;
        }
    }
}