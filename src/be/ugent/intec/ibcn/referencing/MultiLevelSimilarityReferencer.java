package be.ugent.intec.ibcn.referencing;

import be.ugent.intec.ibcn.geo.classifier.NaiveBayesResults;
import be.ugent.intec.ibcn.geo.common.datatypes.DataItem;
import be.ugent.intec.ibcn.geo.common.datatypes.Point;
import be.ugent.intec.ibcn.geo.common.io.DataLoading;
import be.ugent.intec.ibcn.geo.common.io.ReferencingIO;
import be.ugent.intec.ibcn.similarity.SimilarItem;
import be.ugent.intec.ibcn.similarity.Similarity;
import be.ugent.intec.ibcn.similarity.SimilarityIndexer;
import be.ugent.intec.ibcn.similarity.SimilarityParameters;
import java.io.File;
import java.util.*;
import java.util.concurrent.*;

/**
 * More advanced, multilevel similarity based georeferencer.
 * 
 * This implementation covers a multi-level similarity based referencer. The 
 * default constructor with a single SimilarityParameters is changed to a
 * constructor with a list of parameters, for each of the different levels
 * you want to include in the referencing process. Please note that the order
 * in which the parameters are provided, is important. The parameters are 
 * assumed to represent the coarsest to the finest level of classification, e.g. 
 * 500, 2500 and 10000 (in that order).
 * 
 * Starting at the finest level, for each test item, the number of features used
 * during classification are retrieved. Due to the decreasing number of features
 * for models with more classes (and thus finer granularity), it can happen that
 * a given test item has no features at the finest level, but does have features
 * at coarser levels. For this reason, if no features were used during 
 * classification at the given level, we fall back to a coarser level. So for 
 * each test item, we store at which level the item should be referenced.
 * 
 * Then, for each of the different levels, and each of the classes along with
 * the items to georeference, we calculate the Jaccard similarity against the
 * training items that are in the given class, in a similar way as is done with
 * the regular @see SimilarityReferencer.
 * 
 * @see AbstractReferencer
 * 
 * @author Olivier Van Laere <oliviervanlaere@gmail.com>
 */
public class MultiLevelSimilarityReferencer extends AbstractReferencer {

    /**
     * Test data, used for sharing between threads.
     */
    private DataItem [] test_data;
    
    /**
     * List of Similarity parameters for the different levels.
     */
    private List<SimilarityParameters> parameters;
    
    /**
     * Constructor.
     * @param parameters Parameters for similarity search. The parameters
     * expect the different configuration to be in a certain order. For 
     * instance, if you try to run multilevel referencing, the clusterings 
     * should be in ascending order of number of clusters (i.e. 500, 2500, 
     * 10000) in this list.
     */
    public MultiLevelSimilarityReferencer(List<SimilarityParameters> parameters) {
        super(null);
        if (parameters == null || parameters.isEmpty())
            throw new RuntimeException("Please provide valid Similarity "
                    + "Parameters.");
        this.parameters = parameters;
    }
    
    /**
     * Actual similarity search implementation.
     * @param outputFileName 
     */
    @Override
    public void run(String outputFileName) {
        // Load the test data
        System.out.println("Loading test from " + parameters.get(0).
                getTestFile());
        DataLoading dl = new DataLoading();
        // Data is loaded WITHOUT feature selection
        this.test_data = dl.loadDataFromFile(parameters.get(0).getTestFile(), 
                parameters.get(0).getTestParser(), 
                parameters.get(0).getTestLimit(), null);
        
        // Prepare an array of classifier output results
        NaiveBayesResults [] classifier_output = 
                new NaiveBayesResults[parameters.size()];
        // Prepare an array of class and item assignments
        Map<Integer, List<Integer>>[] class_items = 
                new HashMap[parameters.size()];

        // Init the data 
        for (int i = 0; i < this.parameters.size(); i++) {
            classifier_output[i] = 
                    new NaiveBayesResults(parameters.get(i).
                    getClassificationFile());
            class_items[i] = new HashMap<Integer, List<Integer>>();
        }
        
        int counter = 0;      
        for (int i = 0; i < test_data.length; i++) {
            DataItem item = test_data[i];
            // Sanity check
            if (item != null) {
                // Loop over the multiple levels
                for (int level_index = parameters.size() - 1; 
                        level_index >= 0; ) {
                    // Determine the number of features used at this level
                    int numberOfFeaturesUsed = 
                            classifier_output[level_index].
                            getFeatureCount(item.getId());
                    // If classification was guessing and there are coarser 
                    // levels
                    if (numberOfFeaturesUsed == 0 && level_index > 0) {
                        level_index--;
                        continue;
                    }
                    
                    // If we end up here, we have a 'valid' or last resort 
                    // classification
                    
                    // Determine class Id for the current level
                    int classId = classifier_output[level_index].
                            getPrediction(item.getId());
                    
                    List<Integer> list = class_items[level_index].get(classId);
                    if (list == null)
                        list = new ArrayList<Integer>();
                    // the number in the list is the actual index in the array
                    list.add(i); 
                    class_items[level_index].put(classId, list);
                    
                    // Make sure we don't loop again
                    level_index = -1;
                }
            }
            else {
                throw new RuntimeException(
                        "This should not happen? Test item id " + i);
            }
        }

        System.out.println("Calculating similarities");
        
        // Prepare a thread pool
        ExecutorService executor = Executors.newFixedThreadPool(NR_THREADS);
        // Prepare a list for the futures
        List<Future<Map<Integer, Point>>> list = 
                new ArrayList<Future<Map<Integer, Point>>>(); 
        
        int total_to_process = 0;
        // Loop over the different levels
        for (int i = 0; i < this.parameters.size(); i++) {
            // Print some stats
            System.out.println("Clustering " + parameters.get(i).
                    getClassMapper().size() + " for " + class_items[i].size() + 
                    " classes.");
            total_to_process += class_items[i].size();
        
            // For each of the classes we need to process for this level
            for (int classId : class_items[i].keySet()) {
                // Init a Callable, track the future, execute
                Callable<Map<Integer, Point>> worker = 
                        new SimilarityHelperRunnable(classId, 
                        class_items[i].get(classId), parameters.get(i));
                Future<Map<Integer, Point>> submit = 
                        executor.submit(worker);
                list.add(submit);
            }
        }
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
            // Report similarity progress every 25 classes
            if (++counter % 25 == 0)
                System.out.println(counter + "/" + total_to_process);
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
         * Local reference to the parameters used for processing in this 
         * callable.
         */
        private SimilarityParameters parameters;
        
        /**
         * Constructor.
         * @param classId The class for which we will process the index.
         * @param items_for_this_class The list of test IDs assigned to the 
         * class.
         * @param parameters Local reference to the parameters used for 
         * processing in this callable.
         */
        public SimilarityHelperRunnable(int classId,  
                List<Integer> items_for_this_class, 
                SimilarityParameters parameters) {
            this.classId = classId;
            this.items_for_this_class = items_for_this_class;
            this.parameters = parameters;
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
                    System.err.println("This should not happen!? " + i);
                }
            }
            
            // Get the index file name for this level
            String indexFile = SimilarityIndexer.getIndexFile(
                    this.parameters, classId);
            
            // Is there an index for this class?
            if (new File(indexFile).exists()) {
                // Load the similarity data
                DataItem [] items = SimilarityIndexer.loadSimilarityIndex(
                        indexFile, this.parameters.getTrainingParser(), filter);
                // For each of the items predicted in this class
                for (int i : items_for_this_class) {
                    DataItem item = test_data[i];
                    // Sanity check
                    if (item != null) {
                        // Fetch the most similar items
                        SortedSet<SimilarItem> similarities = 
                                Similarity.jaccard(items, item, 
                                this.parameters.getSimilarItemsToConsider());
                        // If there are similar items found
                        if (similarities.size() > 0) {
                            predictions.put(item.getId(), 
                                similarities.first().getItem());
                        }
                        // If there are no similar items found
                        else {
                            // Fall back to medoid
                            predictions.put(item.getId(), 
                                this.parameters.getClassMapper().getMedoids().
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
                            this.parameters.getClassMapper().getMedoids().
                                get(classId));
                    }
                }
            }
            return predictions;
        }
    }
}