package be.ugent.intec.ibcn.geo.features;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.ugent.intec.ibcn.geo.common.Util;
import be.ugent.intec.ibcn.geo.common.datatypes.Coordinate;
import be.ugent.intec.ibcn.geo.common.datatypes.DataItem;
import be.ugent.intec.ibcn.geo.common.datatypes.Point;
import be.ugent.intec.ibcn.geo.common.interfaces.LineParserDataItem;
import be.ugent.intec.ibcn.geo.common.io.FeaturesIO;
import be.ugent.intec.ibcn.geo.common.io.FileIO;
import edu.wlu.cs.levy.CG.KDTree;
import edu.wlu.cs.levy.CG.KeyDuplicateException;
import edu.wlu.cs.levy.CG.KeySizeException;

public class RipleyKFeatureRanker{

	/**
	 * Logger.
	 */
	protected static final Logger LOG = LoggerFactory.getLogger(RipleyKFeatureRanker.class);

    /**
     * Number of threads, for multi-threaded processing.
     */
    private static final int NR_THREADS = 
            Runtime.getRuntime().availableProcessors();

    /**
     * Constant holding the definition for the scoring method that uses the
     * Ripley K score, and ranks the feature according to K*N, N being the
     * number of occurrences of the features.
     */
    public static final int RIPLEY_K_N_SCORE = 1;
    
    /**
     * Constant holding the definition for the scoring method that uses the
     * Ripley K score, and ranks the feature according to K*log(N), N being the
     * number of occurrences of the features.
     */
    public static final int RIPLEY_K_LOG_N_SCORE = 2;
    
    /**
     * Holds the method used for ranking the features.
     */
    private int method;
    
    /**
     * @return the name of this ranking method
     */
    protected String getMethodName() {
        switch(method) {
            case RIPLEY_K_N_SCORE:
                return "Ripley K - K*N";
                
            case RIPLEY_K_LOG_N_SCORE:
                return "Ripley K - K*log(N)";
        }
        return "Ripley K";
    }

    /**
     * Stack of List of Strings. This stack keeps track of multiple lists
     * that will be used by multiple threads. By tracking the lists by means
     * of a Stack, we can reuse the lists among all available threads. This 
     * provides a significant performance increase over creating, abandoning and
     * having the lists begin garbage collected.
     */
    private Stack<List<String>> dataStack = new Stack<List<String>>();
    
    /**
     * Object used for locking the shared input file.
     */
    private final Object fileLock = new Object();
    
    /**
     * Object used for locking the dataStack. 
     */
    private final Object stackLock = new Object();
    
    /**
     * Constant defining the number of lines each thread will read from the
     * input per burst.
     */
    private static final int BURST = 25000;
    
    /**
     * Full package and class name for the Parser implementation to use for
     * parsing the input data.
     */
    private String lineparserClassName;
    
    /**
     * Threshold to use for nearest neighbour search.
     */
    private double threshold;
    
    /**
     * Sample of the feature occurrences to use in the actual K calculations.
     * By default, a sample of 5000 occurrences is used. Most features won't
     * be affected by this, while for the features that do have more occur-
     * rences, the data is dense enough to calculate a good score.
     */
    private int sample;
    
    /**
     * Reference to the filename of the input file with the training data.
     */
    private String inputFile;
    
    /**
     * Constructor.
     * @param inputFile Filename for the input data
     * @param lineparserClassName Package and classname for the input parser
     * implementation
     * @param threshold Threshold to use for nearest neighbour search in the K
     * formula
     * @param sample Number of feature occurrences to use in the actual K
     * formula.
     */
    public RipleyKFeatureRanker(String inputFile, String lineparserClassName,
                                double threshold, int sample) {
        this.inputFile = inputFile;
        this.lineparserClassName = lineparserClassName;
        this.threshold = threshold;
        this.sample = sample;
    }
    
    /**
     * Constructor. A default sample size of 5000 is used.
     * @param inputFile Filename for the input data
     * @param lineparserClassName Package and classname for the input parser
     * implementation
     * @param threshold Threshold to use for nearest neighbour search in the K
     * formula
     */
    public RipleyKFeatureRanker(String inputFile, String lineparserClassName,
                                double threshold) {
        this(inputFile, lineparserClassName, threshold, 5000);
    }

    /**
     * Constructor. A default sample size of 5000 is used and a default 
     * threshold of 100 kilometer.
     * @param inputFile Filename for the input data
     * @param lineparserClassName Package and classname for the input parser
     * implementation
     */
    public RipleyKFeatureRanker(String inputFile, String lineparserClassName) {
        this(inputFile, lineparserClassName, 100, 5000);
    }

    /**
     * Actual feature ranking.
     * @param outputfile The name of the output file that should contain the 
     * ranked features
     * @param power the power value to use for the weight in the K formula.
     * @param method Method to use for ranking the features (K*N or K*log(N)).
     */
    public void process(String outputfile, double power, int method) {
        // Set the method for ranking
        this.method = method;
        LOG.info("Doing {} feature selection.", getMethodName());
        // Get the occurrences of the features
        Map<String, List<Point>> feature_occurrences = getFeatureOccurrences();
        // Get the default K-scores for the features, using a value for the 
        // power weight
        Map<String, Double> feature_score = 
                calculateKValues(feature_occurrences, power);
        
        Map<String, Double> ranked_features = 
                rankFeatures(feature_occurrences, feature_score);
        // Fetch the features
        List<Object> features = new ArrayList(ranked_features.keySet());
        // Export to file
        FeaturesIO.exportFeaturesToFile(features, outputfile);
    }

    /**
     * Determines all the occurrences of the features in the training data.
     * @return A map containing for each feature in the training data, a List of
     * occurrences (Points).
     */
    private Map<String, List<Point>> getFeatureOccurrences() {
        // Prepare the return map
        Map<String, List<Point>> feature_occurrences = 
                new HashMap<String, List<Point>>();
        try {
            // Determine the number of lines in the input
            int lines = FileIO.getNumberOfLines(inputFile);
            // Open the reader
            BufferedReader in = new BufferedReader(new FileReader(inputFile));
            // Prepare the datastack for the given number of threads
            for (int i = 0; i < NR_THREADS; i++)
                dataStack.push(new ArrayList<String>(BURST));
            // Prepare the thread pool
            ExecutorService executor = Executors.newFixedThreadPool(NR_THREADS);
            // Prepare a List for the futures
            List<Future<Map<String, List<Point>>>> list 
                    = new ArrayList<Future<Map<String, List<Point>>>>();
            // Process the input data in bursts
            for (int start = 0; start < lines; start += BURST) {
                // Init callable, track future and execute workers
                Callable<Map<String, List<Point>>> worker = 
                        new FeatureOccurrenceHelper(in, start, 
                        Math.min(lines-start, BURST));
                Future<Map<String, List<Point>>> submit = executor.submit(worker);
                list.add(submit);
            }
            // Retrieve the results
            for (Future<Map<String, List<Point>>> future : list) {
                try {
                    // Fetch the local results
                    Map<String, List<Point>> local_map = future.get();
                    // Merge the results for each of the features
                    for (String feature : local_map.keySet()) {
                        List<Point> plist = feature_occurrences.get(feature);
                        if (plist == null)
                            plist = new ArrayList<Point>();
                        plist.addAll(local_map.get(feature));
                        feature_occurrences.put(feature, plist);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
            }
            // This will make the executor accept no new threads
            // and finish all existing threads in the queue
            executor.shutdown();
            // Wait until all threads are finish
            while (!executor.isTerminated()) {}            
            // Close the input
            in.close();
            LOG.info("Features procssed: {}", feature_occurrences.size());
        } catch (IOException e) {
            LOG.error("IOException: {}", e.getMessage());
        } catch (Exception e) {
            LOG.error("Exception: {}", e.getMessage());
            System.exit(1);
        }
        // return the features and their occurrences
        return feature_occurrences;
    }
    
    /**
     * For each of the features (and their occurrences), calulcate the K-score.
     * @param feature_occurrences The features to process and their occurrences
     * @param power The power value for the weight in the K-score
     * @return A map containing for each feature, the K-score.
     */
    private Map<String, Double> calculateKValues(
            Map<String, List<Point>> feature_occurrences, double power) {
        // Prepare a thread pool
        ExecutorService executor = Executors.newFixedThreadPool(NR_THREADS);
        // Prepate a list for the futures
        List<Future<Map<String, Double>>> list = 
                new ArrayList<Future<Map<String, Double>>>();
        // For each of the features to process
        for (String feature : feature_occurrences.keySet()) {
            // Get a reference to the list of occurrences
            List<Point> plist = feature_occurrences.get(feature);
            // Get the size
            int size = plist.size();
            // If there is a need for sampling
            if (size > sample) {
                // Copy construct a list of the occurrences
                plist = new ArrayList<Point>(feature_occurrences.get(feature));
                // Shuffle the contents
                Collections.shuffle(plist);
                // Sublist it
                plist = plist.subList(0, sample);
            }
            // Prepare the callable, track the future, execute
            Callable<Map<String, Double>> worker = 
                    new Ripley_K_Calculator_Power(feature, plist, power);
            Future<Map<String, Double>> submit = executor.submit(worker);
            list.add(submit);
        }
        // Prepare the result
        Map<String, Double> feature_score = new HashMap<String, Double>();
        // Stats tracking
        int processed = 0;
        // For each of the futures
        for (Future<Map<String, Double>> future : list) {
            try {
                // Add all to the result - future will contain only 1 feature
                // With a score
                feature_score.putAll(future.get());
                // Report progress every 10K features
                if (++processed % 10000 == 0)
                    LOG.info("{}", processed);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
        // This will make the executor accept no new threads
        // and finish all existing threads in the queue
        executor.shutdown();
        // Wait until all threads are finish
        while (!executor.isTerminated()) {}
        // return the features and their scores
        return feature_score;
    }
    
    /**
     * Helper class for multi-threaded feature occurrence counting.
     */
    private class FeatureOccurrenceHelper 
        implements Callable<Map<String, List<Point>>>{

        /**
         * Reference to  the shared input reader.
         */
        private BufferedReader file;
        
        /**
         * Line number to start from in this Callable.
         */
        private int start;

        /**
         * Burst length - number of lines - to process in this Callable.
         */
        private int burst;

        /**
         * Constructor.
         * @param file Reference to the shared input reader
         * @param start Start line number for this Callable
         * @param burst Burst length to process in this Callable
         */
        public FeatureOccurrenceHelper(BufferedReader file, int start, int burst) {
            this.file = file;
            this.start = start;
            this.burst = burst;
        }

        @Override
        public Map<String, List<Point>> call() throws Exception {
            // prepare the result
            Map<String, List<Point>> local_map = 
                    new HashMap<String, List<Point>>();
            // Prepare local cache for the lines to read
            List<String> data;
            // Try to lock the datastack
            synchronized(stackLock) {
                // Get a List for caching
                data = dataStack.pop();
            }
            try {
                // Try to lock the input
                synchronized(fileLock) {
                    // Report some stats if we have processed 1M lines
                    if (start > 0 && start % 1000000 == 0)
                        LOG.info("[{}]", start);
                    int counter = 0;
                    // Load a BURST of data
                    String line;
                    do {
                        line = file.readLine();
                        if (line != null)
                            data.add(line);
                    }
                    while (line != null && ++counter < burst);
                }
            } catch (IOException e) {
                LOG.error("IOException: {}", e.getMessage());
            }
            // Set up the parser
            LineParserDataItem parser = 
                    (LineParserDataItem)Util.getParser(lineparserClassName);
            // Process the data
            for (String current_line : data) {
                // Parse the item
                DataItem item = parser.parse(current_line);
                // Sanity check
                if (item != null) {
                    // For each of the features
                    for (Object o : item.getData()) {
                        String feature = (String)o;
                        // Fetch the list of known occurrences
                        List<Point> plist = local_map.get(feature);
                        if (plist == null)
                            plist = new ArrayList<Point>();
                        // Add this one
                        plist.add(item);
                        // Put the list
                        local_map.put(feature, plist);
                    }
                }
            }
            // Clear the cache
            data.clear();
            // try to lock the datastack
            synchronized(stackLock) {
                // Push this list back
                dataStack.push(data);
            }
            return local_map;
        }
    }

    /**
     * Actual Ripley K calculation. 
     * For the original K formula, see http://en.wikipedia.org/wiki/Spatial_descriptive_statistics#Ripley.27s_K_and_L_functions
     * This code implements a variant of this formule, using a power weight.
     * For details, see http://doi.ieeecomputersociety.org/10.1109/TKDE.2013.42
     */
    private class Ripley_K_Calculator_Power 
        implements Callable<Map<String, Double>>{

        /**
         * The feature for which we are calculating the score.
         */
        protected String feature;
        
        /**
         * The different points at which the feature occurs in the training 
         * data.
         */
        protected List<Point> feature_occurrences;
        
        /**
         * The power to use in the K formula.
         */
        protected double power;
        
        /**
         * Constructor.
         * @param feature The feature to calculate the K score for
         * @param feature_occurrences The occurrences of the feature in the
         * training data.
         * @param power The power weight to use.
         */
        public Ripley_K_Calculator_Power(String feature, 
                List<Point> feature_occurrences, double power) {
            this.feature = feature;
            this.feature_occurrences = feature_occurrences;
            this.power = power;
        }

        @Override
        public Map<String, Double> call() throws Exception {
            // Prepare the result
            Map<String, Double> result = new HashMap<String, Double>();
            // Prepare a map for duplicates in the KD mapping
            Map<Integer, Set<Integer>> duplicate_mapping 
                    = new HashMap<Integer, Set<Integer>>();
            // Init the KD tree
            KDTree<Integer> kd = new KDTree<Integer>(3);
            for (Point p : feature_occurrences) {
                try {
                    // Insert each of the points in the KD tree
                    kd.insert(new Coordinate(p).doubleKey(), p.getId());
                } catch (KeySizeException e) {
                    LOG.error("Error: {}", e.getMessage());
                }
                // if the key already exists
                catch (KeyDuplicateException e) {
                    try {
                        // Fetch the id of the original item that this one
                        // is a duplicate of
                        List<Integer> neighbours = 
                                kd.nearest(new Coordinate(p).doubleKey(), 1);
                        // Get the ID
                        int duplicate_id = neighbours.get(0);
                        // Fetch the associated duplicates
                        Set<Integer> ids = duplicate_mapping.get(duplicate_id);
                        // If there are none yet
                        if (ids == null)
                            ids = new HashSet<Integer>();

                        ids.add(p.getId());
                        // Put the set with this duplicate
                        duplicate_mapping.put(duplicate_id, ids);
                    } catch (KeySizeException e2) {
                        LOG.error("Error: {}" + e2.getMessage());
                    }
                }
            }
            // Actual score calculation
            double score = 0;
            // For each of the occurrences of the feature
            for (Point p : feature_occurrences) {
                // Get the neighbours within the given threshold
                Set<Integer> nbrs = getNeighbours(kd, new Coordinate(p), 
                        threshold, duplicate_mapping);
                // Calculate the score
                score += Math.pow((nbrs.size() - 1) * 
                        1./feature_occurrences.size(), power);
            }
            score = score  * 1./feature_occurrences.size();
            // prepare the result
            result.put(feature, score);
            return result;
        }
        
        /**
         * Method that determines the IDs of the occurrences of features that
         * are within a given distance from a given coordinate.
         * @param kd KDTree used for nearest neighbour lookup
         * @param c The coordinate to search around
         * @param distance The distance within which we seek neighbours
         * @param duplicate_mapping Mapping of duplicates in the KD Tree
         * @return A set of IDs of the Points found within the given distance
         * around the specified coordinate.
         */
        protected Set<Integer> getNeighbours(KDTree<Integer> kd, Coordinate c, 
                double distance, Map<Integer, Set<Integer>> duplicate_mapping) {
            // Prepare the list of results
            List<Integer> neighbours = null;
            // Determine min and max lat/lon bounds (in cartesian coordinates)
            // The coordinates are represented in Euclidian distance
            double [] lower_bounds = {c.getX() - distance, c.getY() - distance, 
                c.getZ() - distance};
            double [] upper_bounds = {c.getX() + distance, c.getY() + distance, 
                c.getZ() + distance};
            try {
                // Fetch all points within bounds
                neighbours = kd.range(lower_bounds, upper_bounds);
            } catch (KeySizeException e) {
                LOG.error("Error: {}", e.getMessage());
            }
            // Copy the current set of neigbhours
            Set<Integer> nbrs_copy = new HashSet<Integer>(neighbours);
            // Now, for each of the neighbours
            for (int id : neighbours) {
                // See if there were any duplicates
                Set<Integer> duplicates = duplicate_mapping.get(id);
                // If so
                if (duplicates != null) {
                    // Add these to the result set
                    nbrs_copy.addAll(duplicates);
                }
            }
            // Return the result
            return nbrs_copy;
        }

    }

    /**
     * Rank the features using the K score and a given method
     * @param feature_occurrences The features and their occurrences
     * @param feature_score The features and their K score
     * @return A Map containing the features and a ranking score, ordered
     * in descending order on the rank score value.
     */
    private Map<String, Double> rankFeatures(
                Map<String, List<Point>> feature_occurrences,
                Map<String, Double> feature_score) {
        // Prepare the result
        Map<String, Double> feature_rankscore = new HashMap<String, Double>();
        // For each of the features we have a score for
        for (String feature : feature_score.keySet()) {
            // Fetch the number of occurrences
            int occ = feature_occurrences.get(feature).size();
            // Fetch the score
            double score = feature_score.get(feature);
            // Depending on the ranking method
            switch(method) {
                case RIPLEY_K_N_SCORE:
                    // Put the info in the score map
                    feature_rankscore.put(feature, occ * score);
                    break;
                    
                case RIPLEY_K_LOG_N_SCORE:
                    // Put the info in the score map
                    feature_rankscore.put(feature, score * Math.log(occ));
                    break;
            }
        }
        // Sort the features on descending order of their rank score and return
        return Util.sortByValueDescending(feature_rankscore);
    }
}