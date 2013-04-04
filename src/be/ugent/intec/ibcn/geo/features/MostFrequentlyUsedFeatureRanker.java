package be.ugent.intec.ibcn.geo.features;

import be.ugent.intec.ibcn.geo.common.Util;
import be.ugent.intec.ibcn.geo.common.datatypes.DataItem;
import be.ugent.intec.ibcn.geo.common.io.DataLoading;
import be.ugent.intec.ibcn.geo.common.io.FeaturesIO;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * This class implements a simple most used tag occurence feature ranking.
 * 
 * For more details
 *  @see http://www.sciencedirect.com/science/article/pii/S002002551300162X#s0085
 * 
 * This implementation currently uses a full load of the data, while it would
 * be possible to just run through the input file and keep statistics.
 * 
 * @author Olivier Van Laere <oliviervanlaere@gmail.com>
 */
public class MostFrequentlyUsedFeatureRanker {

    /**
     * Number of threads, for multi-threaded processing.
     */
    private static final int NR_THREADS = Runtime.getRuntime().availableProcessors();

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
    
    public MostFrequentlyUsedFeatureRanker(String inputfile, String inputParser) {
        // Load the training data 
        DataLoading dl = new DataLoading();
        this.data = dl.loadDataFromFile(inputfile, inputParser, -1, null);
    }

    public void process(String outputfile) {
        System.out.println("Doing "+ getMethodName() +" feature selection.");
        // Tag and occurence count
        Map<Object, Integer> map = new HashMap<Object, Integer>();
        // Prepare a thread pool
        ExecutorService executor = Executors.newFixedThreadPool(NR_THREADS);
        // Track futures
        List<Future<Map<String, Integer>>> list = new ArrayList<Future<Map<String, Integer>>>();
        // Determine block length
        int length = (int) (data.length * 1.0 / NR_THREADS);
        for (int i = 0; i < NR_THREADS; i++) {
            int begin = i * length;
            if (i == NR_THREADS - 1) {
                length = data.length - (i * length);
            }
            int end = begin + length;
            // Start helper callables
            Callable<Map<String, Integer>> worker = new CounterHelper(begin, end);
            Future<Map<String, Integer>> submit = executor.submit(worker);
            list.add(submit);
        }
        // Retrieve the results
        for (Future<Map<String, Integer>> future : list) {
            try {
                Map<String, Integer> submap = future.get();
                // For all the items in the submap
                for (String tag : submap.keySet()) {
                    // Get the count in the main map
                    Integer count = map.get(tag);
                    // If there is none
                    if (count == null)
                        // Init a value
                        count = 0;
                    // Add the count of the submap
                    count+= submap.get(tag);
                    // Store this in the main map
                    map.put(tag, count);
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

        // Sort descending by the most used tags
        map = Util.sortByValueDescending(map);
        // Fetch the features
        List<Object> features = new ArrayList(map.keySet());
        // Export the features to file
        FeaturesIO.exportFeaturesToFile(features, outputfile);        
    }

    /**
     * Helper class for counting tags.
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
         * Actual tag counting
         * @return A map with the Tag - Occurrences for this part of the data
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
                        String tag = (String)o;
                        // Sanity check
                        if (tag.trim().length() > 0 && !tag.equals("")) {
                            // Track occurrences
                            Integer count = local_map.get(tag);
                            if (count == null)
                                count = 0;
                            count++;
                            local_map.put(tag, count);
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