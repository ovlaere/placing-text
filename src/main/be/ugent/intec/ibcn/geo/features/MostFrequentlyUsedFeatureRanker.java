package be.ugent.intec.ibcn.geo.features;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.ugent.intec.ibcn.geo.common.Util;
import be.ugent.intec.ibcn.geo.common.datatypes.DataItem;
import be.ugent.intec.ibcn.geo.common.io.DataLoading;
import be.ugent.intec.ibcn.geo.common.io.FeaturesIO;

/**
 * This class implements a simple most used feature occurrence feature ranking.
 * 
 * For more details
 * @see http://www.sciencedirect.com/science/article/pii/S002002551300162X#s0085
 * 
 * This implementation currently uses a full load of the data, while it would
 * be possible to just run through the input file and keep statistics.
 * 
 * @author Olivier Van Laere <oliviervanlaere@gmail.com>
 */
public class MostFrequentlyUsedFeatureRanker {

	/**
	 * Logger.
	 */
	protected static final Logger LOG = LoggerFactory.getLogger(MostFrequentlyUsedFeatureRanker.class);
	
    /**
     * Number of threads, for multi-threaded processing.
     */
    private static final int NR_THREADS = 
            Runtime.getRuntime().availableProcessors();

    /**
     * @return the name of this ranking method
     */
    protected String getMethodName() {
        return "Most Frequently used";
    }

    /**
     * The training data.
     */
    protected DataItem [] data;

    /**
     * Constructor.
     * @param inputfile filename of the training data
     * @param inputParser parser for the training data
     */
    public MostFrequentlyUsedFeatureRanker(String inputfile, 
            String inputParser) {
        // Load the training data 
        DataLoading dl = new DataLoading();
        this.data = dl.loadDataFromFile(inputfile, inputParser, -1, null);
    }

    /**
     * Actual most used feature ranking.
     * @param outputfile filename of the file to write the output to
     */
    public void process(String outputfile) {
        LOG.info("Doing {} feature selection.", getMethodName());
        // feature and occurence count
        Map<Object, Integer> map = new HashMap<Object, Integer>();
        // Prepare a thread pool
        ExecutorService executor = Executors.newFixedThreadPool(NR_THREADS);
        // Track futures
        List<Future<Map<String, Integer>>> list = 
                new ArrayList<Future<Map<String, Integer>>>();
        // Determine block length
        int length = (int) (data.length * 1.0 / NR_THREADS);
        for (int i = 0; i < NR_THREADS; i++) {
            int begin = i * length;
            if (i == NR_THREADS - 1) {
                length = data.length - (i * length);
            }
            int end = begin + length;
            // Start helper callables
            Callable<Map<String, Integer>> worker = 
                    new CounterHelper(begin, end);
            Future<Map<String, Integer>> submit = executor.submit(worker);
            list.add(submit);
        }
        // Retrieve the results
        for (Future<Map<String, Integer>> future : list) {
            try {
                Map<String, Integer> submap = future.get();
                // For all the items in the submap
                for (String feature : submap.keySet()) {
                    // Get the count in the main map
                    Integer count = map.get(feature);
                    // If there is none
                    if (count == null)
                        // Init a value
                        count = 0;
                    // Add the count of the submap
                    count+= submap.get(feature);
                    // Store this in the main map
                    map.put(feature, count);
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

        // Sort descending by the most used features
        map = Util.sortByValueDescending(map);
        // Fetch the features
        List<Object> features = new ArrayList(map.keySet());
        // Export the features to file
        FeaturesIO.exportFeaturesToFile(features, outputfile);        
    }

    /**
     * Helper class for counting features.
     */
    private class CounterHelper implements Callable<Map<String, Integer>>{

        /**
         * Start index for processing.
         */
        private int begin;

        /**
         * End index for processing.
         */
        private int end;

        /**
         * Constructor.
         * @param begin start index for processing
         * @param end end index for processing
         */
        public CounterHelper(int begin, int end) {
            this.begin = begin;
            this.end = end;
        }

        /**
         * Actual feature counting
         * @return A map with the feature - Occurrences for this part of the 
         * data
         * @throws Exception 
         */
        @Override
        public Map<String, Integer> call() throws Exception {
            // Prepare the local map
            Map<String, Integer> local_map = new HashMap<String, Integer>();
            int i = begin;
            for (; i < end; i++) {
                // Fetch the data item
                DataItem item = data[i];
                // Sanity check
                if (item != null) {
                    Object [] data = item.getData();
                    for (Object o : data) {
                        String feature = (String)o;
                        // Sanity check
                        if (feature.trim().length() > 0 && 
                                !feature.equals("")) {
                            // Track occurrences
                            Integer count = local_map.get(feature);
                            if (count == null)
                                count = 0;
                            count++;
                            local_map.put(feature, count);
                        }
                    }
                }
            }
            // Report progress as we go
            if ((i - begin) % 100000 == 0)
                System.out.print(".");
            return local_map;
        }
    }
}