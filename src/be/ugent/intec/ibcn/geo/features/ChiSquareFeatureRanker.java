package be.ugent.intec.ibcn.geo.features;

import be.ugent.intec.ibcn.geo.common.datatypes.GeoClass;
import be.ugent.intec.ibcn.geo.common.io.FeaturesIO;
import be.ugent.intec.ugent.ibcn.geo.common.Util;
import java.util.*;
import java.util.concurrent.*;

/**
 * This class provides the basic Chi Square functionality for feature selection.
 * 
 * Please note that this way of ranking features, involves information from a
 * specific clustering. In order to load this data, some tools are used that
 * require ALL of the training data to be loaded into memory, which might result
 * in OutOfMemoryException on very large configurations.
 * 
 * For details on the algorithm
 * @see http://www.sciencedirect.com/science/article/pii/S002002551300162X#s0065
 *
 * @author Olivier Van Laere <oliviervanlaere@gmail.com>
 */
public class ChiSquareFeatureRanker extends AbstractClassLevelRanker {

    /**
     * @return the name of this ranking method
     */
    protected String getMethodName() {
        return "Chi2";
    }
    
    /**
     * Constructor.
     * @param inputfile Input file with the training data
     * @param inputParser Parser implementation to use for the input data
     * @param medoidfile Input file with the medoids (cluster centra)
     * @param medoidParser Parser implementation to use for the medoid data 
     */
    public ChiSquareFeatureRanker(String inputfile, String inputParser, 
            String medoidfile, String medoidParser) {
        super(inputfile, inputParser, medoidfile, medoidParser);
    }

    /**
     * Actual Chi2 feature selection (and convert it to a ranking).
     */
    public void process(String outputfile) {
        System.out.println("Doing "+ getMethodName() +" feature selection.");
        // Start timer
        long start = System.currentTimeMillis();
        // Prepare an array of lists, for each GeoClass the feature ranking
        List<Object>[] class_rankings = new ArrayList[this.classmapper.size()];
        // Prepare a thread pool
        ExecutorService executor = Executors.newFixedThreadPool(NR_THREADS);
        // Track futures
        List<Future<ScoringResult>> list = new ArrayList<Future<ScoringResult>>();
        // For each GeoClass
        for (GeoClass geoclass : this.classmapper.getClasses()) {
            // Do chi2 calculation
            Callable<ScoringResult> worker = new ScoringHelper(geoclass);
            Future<ScoringResult> submit = executor.submit(worker);
            list.add(submit);
        }
        // Now, retrieve the results
        for (Future<ScoringResult> future : list) {
            try {
                ScoringResult result = future.get();
                Map<Object, Double> class_ranking = result.getScores();
                // Put the ranking for this class in the right spot in the array
                class_rankings[result.getGeoClass().getId()] =  
                        new ArrayList(class_ranking.keySet());
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
        // Merge the data over the class boundaries - round robin way
        List<Object> features = mergeClassRankings(class_rankings);
        // Export the features to file
        FeaturesIO.exportFeaturesToFile(features, outputfile);
        // Stop the timer
        long stop = System.currentTimeMillis();
        System.out.println("Retained features: " + features.size() + " ("+(stop-start)+" ms.)");
    }

    /**
     * Merge the individual rankings per class, in a round robin fashion.
     * @param class_rankings The ranked features lists per class
     * @return a list of features
     */
    private List<Object> mergeClassRankings(List<Object> [] class_rankings) {
        // Init max variable
        int max_size = 0;
        // Determine max list size
        for (List<Object> list : class_rankings) {
            max_size = Math.max(max_size, list.size());
        }
        // Prepare a list of rounds, to gather the features in a round robin fashion
        List<Set<Object>> rounds = new ArrayList<Set<Object>>();
        // Track the unique global tags
        Set<Object> uniqueTags = new HashSet<Object>();
        // round robin result aggregation
        for (int i = 0; i < max_size; i++) {
            // For each of the known classIds
            for (int classId = 0; classId < class_rankings.length; classId++) {
                // Fetch the feature ranking
                List<Object> list = class_rankings[classId];
                // If there are tags remaining beyond round i
                if (list.size() >= (i + 1)) {
                    // Get the tag
                    Object tag = list.get(i);
                    // If this is not the empty tag
                    if (((String)tag).trim().length() > 0 && !((String)tag).equals("")) {
                        Set<Object> set;
                        // If there already exists a set of elements for pos i
                        if (rounds.size() >= i+1) {
                            // Fetch
                            set = rounds.get(i);
                            if (!uniqueTags.contains(tag)) {
                                // Add this element to its position set
                                set.add(tag);
                                uniqueTags.add(tag);
                            }
                        }
                        // If not
                        else {
                            // Init it
                            set = new HashSet<Object>();
                            if (!uniqueTags.contains(tag)) {
                                // Add this element to its position set
                                set.add(tag);
                                uniqueTags.add(tag);
                            }
                            // Add this set to the rounds set
                            rounds.add(set);
                        }
                    }
                }
            }
        }
        // Prepare the final feature ranking
        List<Object> features = new ArrayList<Object>();
        // For each of the results for each round
        for (int i = 0; i < rounds.size(); i++) {
            Set<Object> round_set = rounds.get(i);
            // Make a list of these elements
            List<Object> shuffled = new ArrayList<Object>(round_set);
            // to shuffle them
            Collections.shuffle(shuffled, rg);
            // Add these to the list of current tags - random order implied by HashSet
            features.addAll(shuffled);
        }
        return features;
    }
    
    /**
     * Helper method calculating the chi2 value for a specific GeoClass.
     * @param geoclass the geoclass to calculate the chi2 value for
     * @return a Map containing the tags of this area, 
     * ordered in descending order (best chi2 first), along with their scores
     */
    protected Map<Object, Double> calculateScoreForClass(GeoClass geoclass) {
        // Prepare a map for the results
        Map<Object, Double> results  = new HashMap<Object, Double>();
        // Create a tag count map for this area
        Map<Object, Integer> classTagCount = this.otc.getClassTagCount(geoclass, data);
        // Fetch the number of items in this class
        long photos_in_area = geoclass.getElements().size();
        long N = -1;
        // Calculate the chi2 values
        for (Object tag : classTagCount.keySet()) {
            long a = classTagCount.get(tag);
            long b = otc.getTagCount(tag) - a;
            long c = photos_in_area - a;
            long d = total_photos - photos_in_area - b;

            if (N == -1) {
                N = a+b+c+d;
            }
            else if ((a+b+c+d) != N)
                System.out.println((a+b+c+d));

            double ad = a*d;
            double cb = c*b;
            double pow = Math.pow(ad-cb, 2);
            double nominator = N*pow;

            double ac = a+c;
            double bd = b+d;
            double ab = a+b;
            double cd = c+d;
            double denominator = ac*bd*ab*cd;

            double chi_square_value = nominator / (denominator);
            // old debug code
            if (chi_square_value < 0) {
                System.out.println(a*d);
                System.out.println(c*b);
                System.out.println((a*d-c*b));
                System.out.println(Math.pow((a*d-c*b),2));
                System.out.println(N*Math.pow((a*d-c*b),2));
                System.out.println((a+c));
                System.out.println((b+d));
                System.out.println((a+b));
                System.out.println((c+d));
                System.out.println((a+c)*(b+d));
                System.out.println((a+b)*(c+d));
                System.out.println((a+c)*(b+d)*(a+b)*(c+d));
                System.out.println(denominator);
            }
            results.put(tag, chi_square_value);
        }
        // Sort the results by value descending
        results = Util.sortByValueDescending(results);
        // There was some good reason to filter this once more, but
        // I forgot why it was?
        Map<Object, Double> filtered = new HashMap<Object, Double>();
        Iterator<Object> it = results.keySet().iterator();
        while (it.hasNext()){
            Object tag = it.next();
            filtered.put(tag,results.get(tag));
        }
        filtered = Util.sortByValueDescending(filtered);
        return filtered;
    }
    
    /**
     * Helper class that allows threaded score calculations for GeoClasses
     */
    protected class ScoringHelper implements Callable<ScoringResult>{

        /**
         * The GeoClass to process in this Callable.
         */
        private GeoClass geoclass;
        
        /**
         * Constructor.
         * @param geoclass The class to process with this helper
         */
        public ScoringHelper(GeoClass geoclass) {
            this.geoclass = geoclass;
        }

        /**
         * @return A map with the tags in this class and their scores.
         * @throws Exception 
         */
        @Override
        public ScoringResult call() throws Exception {
            return new ScoringResult(geoclass, calculateScoreForClass(geoclass));
        }
    }
    
    /**
     * Small helper class to pass the result of the scoring along.
     */
    protected class ScoringResult {
        
        /**
         * The geoclass for which the scores are presented.
         */
        private GeoClass geoclass;
        
        /**
         * @return The geoclass for which the scores are presented.
         */
        public GeoClass getGeoClass() {
            return geoclass;
        }
        
        /**
         * The scores of the geoclass.
         */
        private Map<Object, Double> scores;
        
        /**
         * @return The scores of the geoclass.
         */
        public Map<Object, Double> getScores() {
            return this.scores;
        }
        
        /**
         * Constructor.
         * @param geoclass The geoclass for which the scores are presented.
         * @param scores The scores of the geoclass.
         */
        public ScoringResult(GeoClass geoclass, Map<Object, Double> scores) {
            this.geoclass = geoclass;
            this.scores = scores;
        }
    }
}