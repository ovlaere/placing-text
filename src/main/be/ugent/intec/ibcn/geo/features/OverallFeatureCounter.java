package be.ugent.intec.ibcn.geo.features;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.ugent.intec.ibcn.geo.common.ClassMapper;
import be.ugent.intec.ibcn.geo.common.datatypes.DataItem;
import be.ugent.intec.ibcn.geo.common.datatypes.GeoClass;

/**
 * This class provides a map with the unique features found in the training data
 * along with their occurrence count.
 * 
 * @author Olivier Van Laere <oliviervanlaere@gmail.com>
 */
public class OverallFeatureCounter {

	/**
	 * Logger.
	 */
	protected static final Logger LOG = LoggerFactory.getLogger(OverallFeatureCounter.class);
	
    /**
     * Constant containing the number of processors available in the system.
     */
    protected static final int NR_THREADS = 
            Runtime.getRuntime().availableProcessors();
   
    /**
     * Actual feature - occurrence count map
     */
    private Map<Object, Integer> map;
    
    /**
     * @return A set of objects that represent the unique features found
     */
    public Set getFeatures() {
        return map.keySet();
    }
    
    /**
     * Counter that keeps the overall actual feature occurrences (not unique)
     */
    private int total_feature_occurrences = 0;
    
    /**
     * @return the actual (non-unique) feature occurrences
     */
    public int getTotalFeatureOccurrences() {
        return this.total_feature_occurrences;
    }
    
    /**
     * Reference to the training data.
     */
    private DataItem [] data;
    
    /**
     * Get the overall occurrence count for a given feature
     * @param feature the feature for which you want to get the count
     * @return the overall occurrence count for the given feature
     */
    public int getFeatureCount(Object feature) {
        return map.get(feature);
    }
    
    /**
     * Constructor.
     * @param classmapper ClassMapper object initialized with the current 
     * clustering
     * @param data Reference to the loaded training data
     */
    public OverallFeatureCounter(ClassMapper classmapper, DataItem [] data) {
        LOG.info("Generating overall feature count table");
        this.map = new HashMap<Object, Integer>();
        this.data = data;
        // Prepare the thread pool and the list for the futures
        ExecutorService executor = Executors.newFixedThreadPool(NR_THREADS);
        List<Future<Map<Object, Integer>>> list = 
                new ArrayList<Future<Map<Object, Integer>>>();
        for (GeoClass geoclass : classmapper.getClasses()) {
            // Create callable, track future and execute
            Callable<Map<Object, Integer>> worker = 
                    new ClassFeatureCounter(geoclass);
            Future<Map<Object, Integer>> submit = executor.submit(worker);
            list.add(submit);
        }
        // Process the results
        for (Future<Map<Object, Integer>> future : list) {
            try {
                Map<Object, Integer> result = future.get();
                for (Object key : result.keySet()) {
                    // If we already have a key
                    if (map.containsKey(key))
                        // +1
                        map.put(key, map.get(key)+result.get(key));
                    else
                        // Else start a new count
                        map.put(key, result.get(key));
                    // Track the total occurrences
                    total_feature_occurrences += result.get(key);
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
        LOG.info("Done. Mapped features: {}", map.size());
    }
    
    /**
     * Return the features and their occurrences found in a given class.
     * @param geoclass The GeoClass for which you want the features and 
     * their occurrence count
     * @param data Reference to the training data
     * @return a map with the unique features in the given GeoClass along with 
     * their occurrence count.
     */
    public Map<Object, Integer> getClassFeatureCount(GeoClass geoclass, 
            DataItem [] data){
        // Prepare the result
        Map<Object, Integer> local_map = new HashMap<Object, Integer>();
        // For each of the IDs in this class
        for (Integer id : geoclass.getElements()) {
            // Fetch the actual DataItem
            DataItem item = data[id-1];
            // Sanity checks
            if (item != null && item.getData().length > 0) {
                // for all the features of this item
                for (Object feature : item.getData()) {
                    // If we already have a key
                    if (local_map.containsKey(feature))
                        // +1
                        local_map.put(feature, local_map.get(feature)+1);
                    else
                        // Else start a new count
                        local_map.put(feature, 1);
                }
            }
        }
        return local_map;
    }

    /**
     * Helper class to process the feature count in a specific GeoClass.
     */
    private class ClassFeatureCounter implements Callable<Map<Object, Integer>>{

        /**
         * The GeoClass to process.
         */
        private GeoClass geoclass;

        /**
         * Constructor.
         * @param geoclass The GeoClass to process.
         */
        public ClassFeatureCounter(GeoClass geoclass) {
            this.geoclass = geoclass;
        }

        /**
         * Actualf eature counting.
         * @return A Map with the unique features and their occurrence counts
         * in the geoclass for this Callable.
         * @throws Exception 
         */
        @Override
        public Map<Object, Integer> call() throws Exception {
            return getClassFeatureCount(geoclass, data);
        }
    }
}