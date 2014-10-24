package be.ugent.intec.ibcn.geo.classifier;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
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
import be.ugent.intec.ibcn.geo.common.datatypes.DataItemHome;
import be.ugent.intec.ibcn.geo.common.datatypes.Point;
import be.ugent.intec.ibcn.geo.common.interfaces.LineParserDataItem;
import be.ugent.intec.ibcn.geo.common.io.DataLoading;
import be.ugent.intec.ibcn.geo.common.io.FileIO;

/**
 * This class provides an implementation of a Naive Bayes classifier 
 * with different smoothing techniques.
 *
 * For details,
 * @see http://www.sciencedirect.com/science/article/pii/S002002551300162X#s0100
 * 
 * This classifier will run through the training data on file, making it 
 * scalable to large input files (tested to up to 64 million training items). 
 * However, the test data needs to be loaded into memory using the default IO 
 * method. This has not been a bottleneck so far, as the training data is mostly
 * in the range of thousands to 100 000 items. In case the test data would grow 
 * beyond this range, this should be rewritten.
 * 
 * The classifier will work train the model in batches, in case there are too
 * many features to be used or too many classes to be used. The optimal value
 * for this depends on the amount of available memory, and would be 
 * with X (the amount of memory, in bytes), F the number of features 
 * and C the umber of classes: C ~ Math.sqrt(M * 100 / F)
 * 
 * e.g. 16GB of ram and 175K features: 
 *  Math.sqrt(16*1024*1024*1024*100 / 175000) ~ 3133 classes per batch.
 * So, the classifier would be able to do ~3133 classes per batch. In case you
 * would like to use 20K classes, this would thus take 7 batches.
 * 
 * This class will write the intermediate results to a temp file, using its own 
 * IO methods. This information is then later on loaded using NaiveBayesResults 
 * for processing. 
 * 
 * For details, 
 *  @see NaiveBayesResults
 * 
 * @author Olivier Van Laere <oliviervanlaere@gmail.com>
 */
public class NaiveBayes {

	/**
	 * Logger.
	 */
	protected static final Logger LOG = LoggerFactory.getLogger(NaiveBayes.class);
	
    /**
     * Holds the number of classes to process in one batch.
     */
    private int naive_bayes_batch_size = 2500;
    
    /**
     * Set the number of classes to process in one batch.
     * @param naive_bayes_batch_size 
     */
    public void setNaiveBayesBatchSize(int naive_bayes_batch_size) {
        this.naive_bayes_batch_size = naive_bayes_batch_size;
    }
    
    /**
     * Get a suggestion for the (near) optimal batch size, given the amount
     * of memory available in gigabyte.
     * @param memory_in_gb The amount of memory available to the JVM for the
     * classification
     * @return An integer number of classes that is suggested to use.
     */
    public int getSuggestedBatchSize(double memory_in_gb) {
        return (int)(Math.sqrt(memory_in_gb*1024*1024*1024*100 / 
                parameters.featureCount));
    }
    
    /**
     * Constant containing the number of processors available in the system.
     */
    private static final int NR_THREADS = 
            Runtime.getRuntime().availableProcessors();

    /**
     * Constant defining the maximum likelihood prior.
     */
    public static final int PRIOR_MAX_LIKELIHOOD = 0;
    
    /**
     * Constant defining the uniform prior.
     */
    public static final int PRIOR_UNIFORM = 1;
    
    /**
     * Constant defining the home location prior.
     */
    public static final int PRIOR_HOME = 2;

    /**
     * Constant defining Bayesian smoothing using Dirichlet priors.
     */
    public static final int SMOOTHING_DIRICHLET = 10;
    
    /**
     * Constant defining Jelinek-Mercer smoothing.
     */
    public static final int SMOOTHING_JELINEK = 11;
    
    /**
     * The overall classification parameters.
     */
    private ClassifierParameters parameters;
    
    /**
     * The actual test data.
     */
    private DataItem [] test_data;
    
    /**
     * Constructor.
     * @param parameters The parameters to use for classification.
     */
    public NaiveBayes(ClassifierParameters parameters) {
        this.parameters = parameters;
    }
    
    /**
     * Actual classification.
     */
    public void classify() {
        // Load the test data
        LOG.info("Loading test from {}", parameters.getTestFile());
        DataLoading dl = new DataLoading();
        this.test_data = dl.loadDataFromFile(parameters.getTestFile(), 
                parameters.getTestParser(), parameters.getTestLimit(), 
                parameters.features);

        
        int fileIndex = 0;
        int totalKnownClasses = parameters.getClassMapper().size();
        // Depending on the batch size, do the batches
        for (int begin = 0; begin < totalKnownClasses; 
                begin += this.naive_bayes_batch_size) {
            fileIndex = begin / this.naive_bayes_batch_size;
            String itermediateFile = parameters.getClassificationFile() + "." + 
                    fileIndex;
            // Check if tmp file exists, otherwise resume
            if (!(new File(itermediateFile)).exists()) {
                // Init the internal NB
                NaiveBayesInternal nb = new NaiveBayesInternal();
                // Train the classifier from begin to end - determined by the 
                // batch
                nb.trainMultinomial(begin, 
                        Math.min(begin + this.naive_bayes_batch_size, 
                        totalKnownClasses));
                // Evaluate the test data
                nb.evaluate(this.test_data, itermediateFile);
            }
        }
        // Gather results from the different batches
        NaiveBayesResults [] results = new NaiveBayesResults[fileIndex+1];
        for (int index = 0; index <= fileIndex; index++) {
            String file = parameters.getClassificationFile() + "." + index;
            results[index] = new NaiveBayesResults(file);
        }
        // Prepare the merged results
        NaiveBayesResults merged_results = new NaiveBayesResults();
        // For each of the test items we just processed
        // Actual file IDs start with 1 - this can be tricky if someone else
        // implements it otherwise. Well, you got your warning here :)
        for (int id = 1; id <= test_data.length; id++) {
            // Track the best score and the fileindex in which it occurred.
            double maxScore = Double.NEGATIVE_INFINITY;
            int maxR = -1;
            // For each of the files, check the lines against each other
            for (int r = 0; r < results.length; r++) {
                // Sanity check - bail out if this is violated
                if (results[r].size() != results[0].size())
                    throw new RuntimeException("The intermediate Naive Bayes "
                        + "results differ in size. This should not happen!?");
                // If the score under consideration is better than the current 
                // best
                if (results[r].getScore(id) > maxScore) {
                    // Replace the best values
                    maxScore = results[r].getScore(id);
                    maxR = r;
                }
            }
            // Sanity check - bail out if this is violated
            if (maxR < 0)
                throw new RuntimeException("The best maxR cannot be be -1");
            // Store the best result for this 
            merged_results.putPrediction(id, results[maxR].getPrediction(id) + 
                    (maxR * naive_bayes_batch_size));
            merged_results.putScore(id, results[maxR].getScore(id));
            merged_results.putFeatureCount(id, results[maxR].
                    getFeatureCount(id));
        }
        // Sanity check - bail out if this is violated
        if (merged_results.size() != results[0].size())
            throw new RuntimeException("Final Naive Bayes results"
                    + "have unexpected size. This should not happen!?");
        // Write the NB predictions to file
        merged_results.writeNaiveBayesResultsToFile(
                parameters.getClassificationFile());
        // Remove the temp files
        for (int index = 0; index <= fileIndex; index++)
            new File(parameters.getClassificationFile() + "." + index).delete();
        // At this point, for all of the test items, there is an entry on file
        // using the format:
        // ID ClassID Score #Features (used for classification).
    }
    
    /**
     * Inner class that does the actual training and test of a (part of)
     * the Naive Bayes model.
     * 
     * The main class (NaiveBayes) determines whether, given the number of
     * features and the number of classes, the model would fit into memory. 
     * If this is not the case, the results will be calculate in multiple
     * batches.
     */
    private class NaiveBayesInternal {
        
        /**
         * The (partial) Naive Bayes model.
         */
        private double[][] nb_model;

        /**
         * The number of rows in the Naive Bayes model.
         */
        private int rows;

        /**
         * The number of columns in the Naive Bayes model.
         */
        private int columns;

        /**
         * Prior values for each of the different classes in the Naive Bayes
         * model.
         */
        private double[] priors;

        /**
         * Total number of training items in the Naive Bayes model.
         */
        private int n;

        /**
         * Training of the multinomial NB model.
         * @param begin Begin of the class IDs to process in this batch.
         * @param end End of the class IDs to process in this batch.
         */
        public void trainMultinomial(int begin, int end) {
            LOG.info("== Init multinomial Naive Bayes model [ " + 
                    begin+" - "+end+" | " + parameters.getClassMapper().size() + 
                    " ]. ==");
            long t1 = System.currentTimeMillis();
            // +1 row = overall feature count
            this.rows = end - begin + 1;
            // + 1 column = total class count
            this.columns = parameters.features.size() + 1;
            // Prepare the NB model
            this.nb_model = new double[rows][columns];
            // Prepare the priors
            this.priors = new double[rows - 1];
            long t2 = System.currentTimeMillis();
            LOG.info(" [OK. "+(t2-t1)+" ms.]");
            // Get the parser
            LineParserDataItem parser = (LineParserDataItem)
                    Util.getParser(parameters.getTrainingParser());
            // Set the features for parsing the input
            parser.setFeatures(parameters.features);
            try {
                // Set up the input
                BufferedReader in = new BufferedReader(
                        new FileReader(parameters.getTrainingFile()));
                // Fetch the number of lines to process
                int lines = FileIO.getNumberOfLines(
                        parameters.getTrainingFile());
                // Determine the number of lines to process in case a limit was 
                // set
                if (parameters.getTrainingLimit() > 0 && 
                        parameters.getTrainingLimit() < lines) {
                    lines = parameters.getTrainingLimit();
                }
                // Set up the input lines
                String line = in.readLine();
                int counter = 0;
                // While we have lines or are within the limits
                while (line != null && counter < lines) {
                    // Parse the line
                    DataItem item = parser.parse(line);
                    // Sanity check
                    if (item != null) {
                        // On the fly assign to a medoid
                        int classId = parameters.getClassMapper().
                                findClass(item).getId();
                        // If the class is within the current focus zone for 
                        // this batch
                        if (classId >= begin && classId < end)
                            // Increment the prior counter for max likelihood
                            this.priors[classId-begin]++;
                        // For each of the features we have
                        for (Object feature : item.getData()) {
                            // Again, if the class is within the current focus 
                            // zone
                            if (classId >= begin && classId < end) {
                                // Increment feature for this class
                                nb_model[classId-begin][(Integer)feature]++;
                                // Increment total feature count for this class
                                this.nb_model[classId-begin][columns-1]++;
                            }
                            // Increment overall feature count for this feature
                            this.nb_model[rows-1][(Integer)feature]++;
                            // Increment overall feature count for all features
                            this.nb_model[rows-1][columns-1]++;
                        }
                        // Increment overall element count
                        this.n++;
                    }
                    // Next line
                    line = in.readLine();
                    counter++;
                    // Report every one million items processed
                    if (counter % 1000000 == 0)
                        LOG.info("{}", counter);
                }                
                in.close();
            } catch (IOException e) {
                LOG.error("IOException: {}", e.getMessage());
            }

            // Maximum likelihood prior based on training items in this class
            // in log space
            for (int classId = 0; classId < rows - 1; classId++)
                this.priors[classId] = Math.log(this.priors[classId] / this.n);
            // Stop the init timer
            long t3 = System.currentTimeMillis();
            LOG.info("[Init OK. "+(t3-t2)+" ms.]");
            // Train the model on the statistics gathered
            LOG.info("== Training multinomial Naive Bayes model. ==");
            // Start timer
            long start = System.currentTimeMillis();
            // Prepare a threadpool
            ExecutorService executor = Executors.newFixedThreadPool(NR_THREADS);
            // For all of the classes in this batch
            for (int classId = 0; classId < rows - 1; classId++)
                // Start working
                executor.submit(new NaiveBayesTrainCallable(classId));
            // This will make the executor accept no new threads
            // and finish all existing threads in the queue
            executor.shutdown();
            // Wait until all threads are finish
            while (!executor.isTerminated()) {}
            // Stop the itmer and publish statistics
            long stop = System.currentTimeMillis();
            LOG.info("Training complete. (" + 
                    (stop - start) + " ms.)");
            LOG.info(" [nb_model] classes  : " + 
                    (nb_model.length - 1));
            LOG.info(" [nb_model] features : " + 
                    (nb_model[0].length - 1));        
        }

        /**
         * Helper class for multi-threaded processing of the NB training phase.
         */
        private class NaiveBayesTrainCallable implements Runnable {

            /**
             * Class ID of the class that is being processed.
             */
            private int classId;

            /**
             * Constructor.
             * @param classId ID of the class being processed.
             */
            public NaiveBayesTrainCallable(int classId) {
                this.classId = classId;
            }

            /**
             * NB Training for a specific class of the classifier.
             */
            @Override
            public void run() {
                // Get the occurences of all features in this class
                int occTacc = (int)nb_model[this.classId][columns - 1];
                // Get the occurences of all features in total
                int totalOccTacc = (int)nb_model[rows - 1][columns - 1];

                // - 1 because the last column holds the class occurrence count
                for (int t = 0; t < columns - 1; t++) {
                    // Get the occurences of this feature in this class
                    int occTa = (int)nb_model[this.classId][t];
                    // Get the occurences of this feature in total
                    int totalOccTa = (int)nb_model[rows - 1][t];

                    // Calculate the probability
                    // Default probability in case the features don't occur -
                    // This can happen when the feature list is extracted from 
                    // data different than the training data for the classifier
                    double p = 1;
                    if (totalOccTa > 0) {
                        // Switch on the smoothing method
                        switch (parameters.smoothingMethod) {
                            case SMOOTHING_JELINEK:
                                double lambda = parameters.jelinekLambda;
                                p = (lambda * ((totalOccTa * 1.) / 
                                        (totalOccTacc * 1.))) +
                                        ((1. - lambda) * 
                                        ((occTa * 1.) / (occTacc * 1.)));
                                break;
                            case SMOOTHING_DIRICHLET:
                                double mu = parameters.dirichletMu;
                                p = (occTa + mu * ((totalOccTa * 1.) / 
                                    (totalOccTacc * 1.))) / ((occTacc * 1.) + 
                                        mu);
                                break;
                        }
                    }
                    // Set the probability for feature t in classId to p.
                    nb_model[this.classId][t] = p;
                }
            }
        }
        
        /**
         * Evaluation of the NB model.
         * @param test_data Array of DataItem test data
         * @param intermediateFile filename for the intermediate results 
         */
        public void evaluate(DataItem[] test_data, String intermediateFile) {
            LOG.info("== Applying multinomial Naive Bayes model. ==");
            long start = System.currentTimeMillis();

            ExecutorService executor = Executors.newFixedThreadPool(NR_THREADS);
            List<Future<File>> list = new ArrayList<Future<File>>();
            int length = (int) (test_data.length * 1.0 / NR_THREADS);
            for (int i = 0; i < NR_THREADS; i++) {
                int begin = i * length;
                if (i == NR_THREADS - 1) {
                    length = test_data.length - (i * length);
                }
                int end = begin + length;
                Callable<File> worker = new NaiveBayesEvaluateCallable(begin, 
                        end, intermediateFile);
                Future<File> submit = executor.submit(worker);
                list.add(submit);
            }
            // Now retrieve the result and write them to file
            try {
                PrintWriter results = new PrintWriter(
                        new FileWriter(intermediateFile), true);
                int results_counter = 0;
                LOG.info("Merging individual thread result files");
                long start_merge = System.currentTimeMillis();
                for (Future<File> future : list) {
                    try {
                        File tmp_file = future.get();
                        LOG.info("Processing " + tmp_file.getName());
                        BufferedReader input = new BufferedReader(
                                new FileReader(tmp_file));
                        String line = input.readLine();
                        while (line != null) {
                            String [] values = line.split("\t");
                            // ID PREDICTION SCORE FEATURES_USED
                            results.println(values[0] + "\t" + values[1] + "\t" 
                                    + values[2] + "\t" + values[3]);
                            results_counter++;
                            line = input.readLine();
                        }
                        input.close();
                        tmp_file.delete();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    }            
                }
                results.close();
                long stop_merge = System.currentTimeMillis();
                LOG.info("Merge complete ("+(stop_merge-start_merge)
                        +" ms.)");
                LOG.info("Lines: " + results_counter);
            } catch (IOException e) {
                LOG.error("IOException during writing of predictions: {}", e.getMessage());
            }
            // This will make the executor accept no new threads
            // and finish all existing threads in the queue
            executor.shutdown();
            // Wait until all threads are finish
            while (!executor.isTerminated()) {}
            // Stop the itmer and publish statistics
            long stop = System.currentTimeMillis();
            LOG.info("=============== [NaiveBayes] Results: ======="
                    + "========");
            LOG.info("Total processing time: " + (stop - start) + 
                    " ms.");
            LOG.info("============================================="
                    + "========");
        }

        /**
         * Helper class for multi-threaded evaluation of the NB model.
         */
        private class NaiveBayesEvaluateCallable implements Callable<File> {

            /**
             * Index of the beginning of the data this thread has to process.
             */
            private int begin;

            /**
             * Index of the end of the data this thread has to process.
             */
            private int end;

            /**
             * Output destination.
             */
            private String outputDir;

            /**
             * Constructor.
             * @param begin the beginning of the data this thread has to 
             * process.
             * @param end the end of the data this thread has to process.
             * @param intermediateFile The filename of the intermediate result 
             * of this Naive Bayes batch.
             */
            public NaiveBayesEvaluateCallable(int begin, int end, 
                    String intermediateFile) {
                this.begin = begin;
                this.end = end;
                String path = new File(intermediateFile).getAbsoluteFile().
                        toString();
                this.outputDir = path.substring(0, path.lastIndexOf("/") + 1);
            }

            /**
             * Evaluation of the NB model.
             * @return a temporary File containing the predictions for this 
             * smaller part of the data for the current batch.
             * @throws Exception
             */
            @Override
            public File call() throws Exception {
                // Create a tmp file for this thread
                File tmp_file = File.createTempFile("NB_thread_results", 
                        ".geo");
                // Now get a tmp filename within the path of the preferred
                // output destination
                File tmp_file2 = new File(outputDir + tmp_file.getName());
                // Already delete the first real temp file
                tmp_file.delete();
                // Open a writer to the output
                PrintWriter file = new PrintWriter(new FileWriter(tmp_file2), 
                        true);
                int processed = 0;
                // For the part of the data this Callable processes
                for (int i = begin; i < end; i++) {
                    // Fetch the item
                    DataItem item = test_data[i];
                    // Extra sanity check
                    if (item != null) {
                        processed++;
                        // Print the ID
                        file.print(item.getId() + "\t");
                        // Determine the best score for this item
                        Map<Integer, Double> scores = 
                                new HashMap<Integer, Double>();
                        // For the current test item, evaluate the classes
                        int predictedClassId = getBestClass(item, scores);
                        // By sorting all classes evaluated in this batch
                        // in descending order
                        scores = Util.sortByValueDescending(scores);
                        // If we have found a valid class (sanity check)
                        if (predictedClassId >= 0) {
                            // Write the scores to file, tab separated
                            for (Integer classId : scores.keySet()) {
                                file.print(classId + "\t" + 
                                        scores.get(classId) + "\t");
                                file.flush();
                                // Break after the 'winning' class ID and its 
                                // score (in log space).
                                break;
                            }
                            // Write the number of features used for 
                            // classification to file as well.
                            file.println(item.getData().length);
                        } 
                        // This should not happen
                        else {
                            // So, if it does, bail out
                            file.close();
                            // Cry
                            throw new RuntimeException("[NULL PREDICTION] " + 
                                    item);
                        }
                        // Publish progress on a 10K item level
                        if (processed % 10000 == 0) {
                            LOG.info("{}", processed);
                        }
                    }
                }
                // Close output
                file.close();
                // Return File object with tmp results
                return tmp_file2;
            }

            /**
             * Apply the multinomial model for a specific photo.
             * @param item DataItem that is being evaluated
             * @param scores Empty map to put the scores in for the classIds
             * @return the ID of the class that is considered to be the most
             * likely to contain the given DataItem that is being tested
             */
            private int getBestClass(DataItem item, 
                    Map<Integer, Double> scores) {
                // Fetch the features of the test object
                Object[] features = item.getData();
                // Init the best score as Min infinity
                // We are going into log space!
                double best_score = Double.NEGATIVE_INFINITY;
                // Track best class ID
                int best_class = -1;
                // For all classes in this batch
                for (int classId = 0; classId < rows - 1; classId++) {
                    // PRIOR PART  
                    double score = getPrior(classId, item);
                    // FEATURE BASED PART
                    for (Object feature : features) {
                        // This should always be the case, but you never know...
                        if (feature instanceof Integer) {
                            // Add log score for this feature
                            score += Math.log(
                                    nb_model[classId][(Integer)feature]);
                        }
                    }
                    // Put score for this classID in the map
                    scores.put(classId, score);
                    // Track best items
                    if (score > best_score) {
                        best_score = score;
                        best_class = classId;
                    }
                }
                // Return the ID of the best class.
                return best_class;
            }
        }
    
        /**
         * Return the prior for a given class. In certain cases, the prior is 
         * also influenced by the item itself.
         * @param classId The class ID for which we want to get the prior
         * @param item the Actual test item
         * @return a log-valued double prior score
         */
        private double getPrior(int classId, DataItem item) {
            // Default prior = 0;
            double prior = 0;
            // Switch prior modes
            switch (parameters.prior_mode) {
                // Max likelihood
                case PRIOR_MAX_LIKELIHOOD:
                    // Already inited in log space, so just return
                    prior = priors[classId];
                    break;
                // Uniform prior - 1 / #classes - needs log conversion
                case PRIOR_UNIFORM:
                    prior = Math.log(1. / parameters.classCount);
                    break;
                // Prior using information from home location
                case PRIOR_HOME:
                    // This can only be done if the parser had the test item as
                    // a DataItemHome
                    if (item instanceof DataItemHome) {
                        // Fetch the home location
                        DataItemHome itemH = (DataItemHome)item;
                        Point home = itemH.getHomeLocation();
                        // Check if we have this info
                        if (home != null) {
                            // Get the medoid for this class under consideration
                            Point center = parameters.getClassMapper().
                                    getMedoids().get(classId);
                            // Log score the prior using the info obtained
                            prior = Math.log(Math.pow(1. / 
                                    (center.distance(itemH.getHomeLocation()) + 
                                    0.001),
                                        parameters.home_weight));
                        }
                        // If we use the home prior, but don't have a home
                        // location we simply use the maximum likelihood.
                        else {
                            prior = priors[classId];
                        }
                    }
                    // If we use this prior but don't have DataItemHome objects
                    // fall back to the maximum likelihood.
                    else {
                        prior = priors[classId];
                    }                
            }
            // Return the prior
            return prior;
        }
    }
}