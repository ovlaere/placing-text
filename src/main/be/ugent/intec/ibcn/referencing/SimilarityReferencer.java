package be.ugent.intec.ibcn.referencing;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.ugent.intec.ibcn.geo.classifier.NaiveBayesResults;
import be.ugent.intec.ibcn.geo.common.datatypes.DataItem;
import be.ugent.intec.ibcn.geo.common.datatypes.Point;
import be.ugent.intec.ibcn.geo.common.io.DataLoading;
import be.ugent.intec.ibcn.geo.common.io.ReferencingIO;
import be.ugent.intec.ibcn.similarity.SimilarItem;
import be.ugent.intec.ibcn.similarity.Similarity;
import be.ugent.intec.ibcn.similarity.SimilarityIndexer;
import be.ugent.intec.ibcn.similarity.SimilarityParameters;

/**
 * Actual implementation of a similarity based georeferencer.
 * 
 * More advanced than the medoid referencer, this class will determine, for each
 * test item, to which class it was assigned by the classifier. The results
 * will be grouped per class.
 * 
 * Next, the similarity index will be read from file, for this class, and the
 * Jaccard similarity will be calculate between all the training items in the 
 * class and each test item assigned to this class.
 * 
 * The location of the most similar item is then returned as the location 
 * estimate for the test item. In case of absence of a similar item, we fall 
 * back to returning the location of the medoid of that class.
 * 
 * @see SimilarityIndexer
 * @see Similarity
 * @see AbstractReferencer
 * 
 * @author Olivier Van Laere <oliviervanlaere@gmail.com>
 */
public class SimilarityReferencer extends AbstractReferencer {

	/**
	 * Logger.
	 */
	protected static final Logger LOG = LoggerFactory.getLogger(SimilarityReferencer.class);

    /**
     * Test data, used for sharing between threads.
     */
    private DataItem [] test_data;
    
    /**
     * Constructor.
     * @param parameters Parameters for similarity search.
     */
    public SimilarityReferencer(SimilarityParameters parameters) {
        super(parameters);
    }
    
    /**
     * Actual similarity search implementation.
     * @param outputFileName 
     */
    @Override
    public void run(String outputFileName) {
        // Load the test data
        LOG.info("Loading test from {}", parameters.getTestFile());
        DataLoading dl = new DataLoading();
        // Data is loaded WITHOUT feature selection
        this.test_data = dl.loadDataFromFile(parameters.getTestFile(), 
                parameters.getTestParser(), parameters.getTestLimit(), null);
        
        NaiveBayesResults classifier_output = 
                new NaiveBayesResults(parameters.getClassificationFile());
        
        int counter = 0;
        Map<Integer, List<Integer>> class_items = 
                new HashMap<Integer, List<Integer>>();
        
        for (int i = 0; i < test_data.length; i++) {
            DataItem item = test_data[i];
            // Sanity check
            if (item != null) {
                // Determine class Id
                int classId = classifier_output.getPrediction(item.getId());
                // This info is ignored in this implementation
                int numberOfFeaturesUsed = classifier_output.
                        getFeatureCount(item.getId());
                
                List<Integer> list = class_items.get(classId);
                if (list == null)
                    list = new ArrayList<Integer>();
                // the number in the list is the actual index in the array
                list.add(i); 
                class_items.put(classId, list);                    
            }
            else
                throw new RuntimeException("This should not happen? "
                        + "Test item id " + i);
        }

        LOG.info("Calculating similarities");
        // Print some stats
        LOG.info("Clustering {}: actual used classes {}.", 
        		parameters.getClassMapper().size(), class_items.size());
        // Prepare a thread pool
        ExecutorService executor = Executors.newFixedThreadPool(NR_THREADS);
        // Prepare a list for the futures
        List<Future<Map<Integer, Point>>> list = 
                new ArrayList<Future<Map<Integer, Point>>>();
        int batches_launched = 0;
        // For each of the classes we need to process
        for (int classId : class_items.keySet()) {
        	// Batch this
        	int batch_size = 50;
        	List<Integer> items_to_process = class_items.get(classId);
        	int batches = (items_to_process.size() / batch_size) + 1;
        	for (int i = 0; i < batches; i++) {
        		List<Integer> sublist = items_to_process.subList(
        				i * batch_size, Math.min((i+1) * batch_size, items_to_process.size()));
	            // Init a Callable, track the future, execute
	            Callable<Map<Integer, Point>> worker = new SimilarityHelperRunnable(classId, sublist);
	            Future<Map<Integer, Point>> submit = executor.submit(worker);
	            list.add(submit);
	            batches_launched++;
        	}
        }
        LOG.info("Similarity batches launched: {}", batches_launched);
        // Prepare the results
        Map<Integer, Point> predictions = new TreeMap<Integer, Point>();
        // Retrieve the results
        for (Future<Map<Integer, Point>> future : list) {
            try {
                // Abandon results, but use this for progress monitoring
                predictions.putAll(future.get());
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
            // Report similarity progress every 25 batches
            if (++counter % 25 == 0)
                LOG.info("{}/{}", counter, batches_launched);
        }
        // This will make the executor accept no new threads
        // and finish all existing threads in the queue
        executor.shutdown();
        
        // Write to file
        ReferencingIO.writeLocationsToFile(predictions, outputFileName);
    }
    
    /**
     * This class loads the similarity index from file for a given class.
     * It will find the most similar items for each of the test items assigned
     * to that class and return the location of the most similar item in 
     * a map.
     */
    private class SimilarityHelperRunnable implements 
            Callable<Map<Integer, Point>> {
    
        /**
         * The classId of the class that is being processed.
         */
        private int classId;

        /**
         * List of IDs of test items assigned by the classifier in this class.
         */
        private List<Integer> items_for_this_class;

        /**
         * Constructor.
         * @param classId The class for which we will process the index.
         * @param items_for_this_class The list of test IDs assigned to 
         * the class.
         */
        public SimilarityHelperRunnable(int classId,  
                List<Integer> items_for_this_class) {
            this.classId = classId;
            this.items_for_this_class = items_for_this_class;
        }

        @Override
        public Map<Integer, Point> call() throws Exception {
            // Prepare the result
            Map<Integer, Point> predictions = new HashMap<Integer, Point>();
            
            // Create a superset of all the features found in this class
            Set<String> filter = new HashSet<String>();
            // For each of the ids in this class
            for (int i : items_for_this_class) {
                // Fetch the DataItem
                DataItem item = test_data[i];
                // Sanity check
                if (item != null) {
                    // For each of the features
                    for (Object f : item.getData())
                        // Add to the filter set
                        filter.add((String)f);
                }
                else {
                    LOG.error("This should not happen!? {}", i);
                }
            }
            
            // Get the index file name
            String indexFile = SimilarityIndexer.getIndexFile(
                    (SimilarityParameters)parameters, classId);
            
            // Is there an index for this class?
            if (new File(indexFile).exists()) {
                // Load the similarity data
                DataItem [] items = SimilarityIndexer.loadSimilarityIndex(
                        indexFile, parameters.getTrainingParser(), filter);
                // For each of the items predicted in this class
                for (int i : items_for_this_class) {
                    DataItem item = test_data[i];
                    // Sanity check
                    if (item != null) {
                        // Fetch the most similar items
                        SortedSet<SimilarItem> similarities = 
                                Similarity.jaccard(items, item, 
                                ((SimilarityParameters)parameters).
                                getSimilarItemsToConsider());
                        // If there are similar items found
                        if (similarities.size() > 0) {
                            predictions.put(item.getId(), 
                                similarities.first().getItem());
                        }
                        // If there are no similar items found
                        else {
                            // Fall back to medoid
                            predictions.put(item.getId(), 
                                parameters.getClassMapper().getMedoids().
                                    get(classId));
                        }
                    }
                }
            }
            // If not - this should not happen? - fallback to medoid prediction
            else {
                // For each of the items predicted in this class
                for (int i : items_for_this_class) {
                    DataItem item = test_data[i];
                    // Sanity check
                    if (item != null) {
                        predictions.put(item.getId(), 
                            parameters.getClassMapper().getMedoids().
                                get(classId));
                    }
                }
            }
            return predictions;
        }
    }
}