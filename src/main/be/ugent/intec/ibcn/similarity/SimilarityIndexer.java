package be.ugent.intec.ibcn.similarity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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

import be.ugent.intec.ibcn.geo.classifier.NaiveBayesResults;
import be.ugent.intec.ibcn.geo.common.Util;
import be.ugent.intec.ibcn.geo.common.datatypes.DataItem;
import be.ugent.intec.ibcn.geo.common.datatypes.Point;
import be.ugent.intec.ibcn.geo.common.interfaces.LineParser;
import be.ugent.intec.ibcn.geo.common.interfaces.LineParserDataItemSimilarity;
import be.ugent.intec.ibcn.geo.common.io.FileIO;

/**
 * SimilarityIndexer: creates similarity index on file, grouped by class IDs
 * from a given clustering. This allows to batch process the test items
 * that are within the same class. The similarity index is coupled to a given
 * clustering. Changing the clustering requires new indexing.
 * 
 * WARNING: This code might break if run on a training file with more than 
 * 16M-32M training items.
 * 
 * @author Olivier Van Laere <oliviervanlaere@gmail.com>
 */
public class SimilarityIndexer {

	/**
	 * Logger.
	 */
	protected static final Logger LOG = LoggerFactory.getLogger(SimilarityIndexer.class);

    /**
     * Constant containing the number of processors available in the system.
     */
    protected static final int NR_THREADS = 
            Runtime.getRuntime().availableProcessors();

    /**
     * Constant holding the number of lines to read for each thread burst.
     * The value of 25000 has proven to work well, but this can be tuned to
     * the performance of a specific system.
     * 
     * Please note that the individual threads will 'cache' READ_BURST lines in 
     * a list. So on a 16-core system the memory load will increase a lot
     * if this burst rate is too high.
     */
    protected static final int READ_BURST = 25000;

    /**
     * Variable holding the maximum number of open files at the same time.
     * Putting this value too high might result in crashes due to OS open
     * file limit restrictions.
     */
    private static int open_file_limit = 1000;
    
    /**
     * Set the open file limit.
     * @param open_file_limit the new open file limit.
     */
    public static void setOpenFileLimit(int new_open_file_limit) {
        open_file_limit = new_open_file_limit;
    }
    
    /**
     * Object used for locking while threads try to lock the input file.
     */
    private final Object fileLock = new Object();
    
    /**
     * Parameters for similarity indexing.
     */
    private SimilarityParameters parameters;
    
    /**
     * Global variable shared between threads to check whether the overall
     * processing limit is hit. This is important when you, for instance, only
     * want to process a number of training items less than the actual dataset.
     */
    private int linelimit;

    /**
     * Constructor.
     * @param parameters Parameters to use for similarity indexing
     */
    public SimilarityIndexer(SimilarityParameters parameters) {
        this.parameters = parameters;
    }
    
    /**
     * Actual similarity indexing.
     */
    public void index() {
        // Prepare a map for line to class assignments
        Map<Integer, Integer> id_class_map = new HashMap<Integer, Integer>();
        // Prepare a map for class to class count assignments
        Map<Integer, Integer> class_count_map = new HashMap<Integer, Integer>();                

        // Run through the training data, and determine to which class each line
        // belongs
        determineClassAssignments(id_class_map, class_count_map);
        // Create the actual similarity index
        createCacheFiles(id_class_map, class_count_map);
    }
    
    /**
     * Run through the training data, on the fly, and determine to which 
     * class from the classifier each item belongs.
     * @param id_class_map a map for line to class assignments
     * @param class_count_map a map for class to class count assignments
     */
    private void determineClassAssignments(Map<Integer, Integer> id_class_map, 
            Map<Integer, Integer> class_count_map) {
        // Start a timer
        long t1 = System.currentTimeMillis();
        // Get a set of the IDs that are found throughout classification
        Set<Integer> used_classIds = new HashSet<Integer>(
                new NaiveBayesResults(parameters.getClassificationFile())
                .getUsedClasses());        
        try {
            // Print some info
            LOG.info("Scanning classId... ({} used classIds) from {}", 
            		used_classIds.size(), parameters.getTrainingFile());
            // Fetch the number of lines to process
            int lines = FileIO.getNumberOfLines(parameters.getTrainingFile());
            // Determine the number of lines to process in case a limit was set
            if (parameters.getTrainingLimit() > 0 && 
                    parameters.getTrainingLimit() < lines) {
                lines = parameters.getTrainingLimit();
            }
            this.linelimit = lines;
            
            // Open the input
            BufferedReader in = new BufferedReader(
                    new FileReader(parameters.getTrainingFile()));
            // Prepare a thread pool
            ExecutorService executor = Executors.newFixedThreadPool(NR_THREADS);
            // Prepare a list for the futures
            List<Future<SimIndexHelperResult>> list = 
                    new ArrayList<Future<SimIndexHelperResult>>();
            // For each of the lines to process, start working in bursts
            for (int start = 0; start < lines; start += READ_BURST) {
                // Create callable, track future, submit
                Callable<SimIndexHelperResult> worker = 
                        new SimIndexerHelper(in, start, 
                        Math.min(lines-start, READ_BURST), 
                    // Using this the used_classIds, you can restrict indexing
                    // to the used classes only, but if the classification
                    // result changes, the indexing needs to be redone
//                        new HashSet<Integer>(used_classIds));
                    // When setting the used_classes to null, all classes
                    // will be indexed. The similarity index only needs
                    // to be rebuild when a new clustering is used.
                        null);
                Future<SimIndexHelperResult> submit = executor.submit(worker);
                list.add(submit);
            }
            
            // Now retrieve the results
            for (Future<SimIndexHelperResult> future : list) {
                try {
                    // Get the result helper
                    SimIndexHelperResult result = future.get();
                    // Store all the line to class assignments in the global map
                    id_class_map.putAll(result.getLine_class_map());
                    // For each of the classes in the class count map
                    for (int classId : result.getClass_count_map().keySet()) {
                        // Fetch the current count
                        Integer count = class_count_map.get(classId);
                        // If this is not found
                        if (count == null)
                            // Init the count
                            count = 0;
                        // Gather the actual item count - this is used to write
                        // at the top of the index files.
                        class_count_map.put(classId, count + 
                                result.getClass_count_map().get(classId));
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
            // Report some stats
            LOG.info("Scan time: {}, training items in classes: {}",
            		(System.currentTimeMillis() - t1), id_class_map.size());
        }
        catch (IOException e) {
            LOG.error("IOException: {}", e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    /**
     * Run through the training data and create files for each class the 
     * classifier used when predicting the test data.
     * @param line_class_map a map for line to class assignments
     * @param class_count_map a map for class to class count assignments
     */
    private void createCacheFiles(Map<Integer, Integer> line_class_map, 
            Map<Integer, Integer> class_count_map) {
        // Start a timer
        long t1 = System.currentTimeMillis();
        // Until all classes are processed - we need to be process within the
        // OPEN_FILE_LIMIT
        while (class_count_map.size() > 0) {
            LOG.info("Creating index for {} remaining classes, batch size: {}",
            		class_count_map.size(), open_file_limit);
            // Determine what we can do in this batch
            Set<Integer> classesInCurrentBatch = new HashSet<Integer>();
            try {
                // Create a map to track the filewriters
                Map<Integer, PrintWriter> classId_writer_map = 
                        new HashMap<Integer, PrintWriter>();
                for (int classId : class_count_map.keySet()) {
                    if (classId_writer_map.size() < open_file_limit) {
                        // Batch dir
                        String batchDir = getBatchDir(parameters, classId);
                        // Prepare a directory for the current indexFile
                        File dirtest = new File(batchDir);
                        // Make the necessary directories
                        dirtest.mkdirs();
                        // Open the writer for the index for the given classId
                        PrintWriter out = new PrintWriter(
                            new FileWriter(getIndexFile(parameters, classId)));
                        // Start the file with the number of items in the class
                        out.println(class_count_map.get(classId));
                        // Store a reference to the writer
                        classId_writer_map.put(classId, out);
                        // Add the current class to the current batch
                        classesInCurrentBatch.add(classId);
                    }
                }
                // Read the input data
                BufferedReader in = new BufferedReader(
                        new FileReader(parameters.getTrainingFile()));
                // skip the line count - this might loose an item if the file 
                // did not contain that info on the first line.
                in.readLine(); 
                int counter = 0;
                int noTags = 0;
                String line = in.readLine();
                // Get the parser
                LineParser parser = 
                        Util.getParser(parameters.getTrainingParser());
                while (line != null) {
                    DataItem item = (DataItem)parser.parse(line);
                    // Fetch the class assignment for this line number
                    Integer classId = null;
                    if (item != null) 
                    	classId = line_class_map.get(item.getId());
                    else
                    	noTags++;
                    // If we the class is in this batch
                    if (classId != null && 
                            classesInCurrentBatch.contains(classId)) {
                        // write the line to the file
                        classId_writer_map.get(classId).println(line);
                    }
                    line = in.readLine();
                    // report progress after 1M items
                    if (++counter % 1000000 == 0)
                        LOG.info("{}\ttraining items without tags: {}", counter, noTags);
                    // In case we hit the global linelimit
                    if (counter == linelimit)
                        break;
                }      
                LOG.info("Training data without tags: {}", noTags);
                // Close the input
                in.close();
                // Close all the open files
                for (PrintWriter out : classId_writer_map.values()) {
                    if (out != null)
                        out.close();
                }
            }
            catch (IOException e) {
                LOG.error("IOException: {}", e.getMessage());
                e.printStackTrace();
                System.exit(1);
            }
            // For each of the classes we processed, remove from the 
            // class_count keyset
            class_count_map.keySet().removeAll(classesInCurrentBatch);
        }
        // Stop the timer
        long t2 = System.currentTimeMillis();
        // Print stats
        LOG.info("All done. [ {} ms.]", (t2-t1));
    }
    
    /**
     * Remove the index data on disk.
     */
    public void removeIndex() {
        FileIO.delete(new File(parameters.getSimilarityDirectory()));
    }
    
    /**
     * Callable that processes a part of the training data to find out to which
     * classID a given line belongs.
     */
    private class SimIndexerHelper implements Callable<SimIndexHelperResult> {

        /**
         * Reference to the shared BufferedReader for the input data.
         */
        private BufferedReader file;
        
        /**
         * Value indicating the starting line number for this callable.
         */
        private int start;
        
        /**
         * Length of the burst to process with this callable.
         */        
        private int burst;
        
        /**
         * Copy of the classIDs used by the classifier.
         */
        private Set<Integer> used_classIds;
        
        /**
         * Constructor.
         * @param file Reference to the BufferedReader for the input
         * @param start Start index of the line this callable processes
         * @param burst Length of the burst this callable processes
         * @param used_classIds Copy of the classIDs used by the classifier
         */
        public SimIndexerHelper(BufferedReader file, int start, int burst, 
                Set<Integer> used_classIds) {
            this.file = file;
            this.start = start;
            this.burst = burst;
            this.used_classIds = used_classIds;
        }
        
        /**
         * Call implementation
         * @return A SimIndexHelperResult, containing for each of the lines
         * this callable processes the class assignments, as well as a map with
         * the number of lines in each class.
         * @throws Exception 
         */
        @Override
        public SimIndexHelperResult call() throws Exception {
            // Prepare local map
            Map<Integer, Integer> id_class_map = 
                    new HashMap<Integer, Integer>();
            Map<Integer, Integer> class_count_map = 
                    new HashMap<Integer, Integer>();
            List<String> data = new ArrayList<String>();
            try {
                // Get the parser
                LineParser parser = 
                        Util.getParser(parameters.getTrainingParser());
                // Try to get a lock on the input data
                synchronized(fileLock) {
                    int counter = 0;
                    // Load a BURST of data
                    String line;
                    do {
                        line = file.readLine();
                        if (line != null)
                            data.add(line);
                    }
                    // While there are lines, we are in our burst and we are
                    // within the global line limit
                    while (line != null && ++counter < burst && 
                            counter < linelimit);
                }
                // Now, for our local line cache, process the lines
                for (String current_line : data) {
                    Point item = parser.parse(current_line);
                    // If we have an item with features
                    if (item != null) {
                        // Fetch the class ID by on the fly assignment
                        int classId = parameters.getClassMapper().
                                findClass(item).getId();
                        // If this class is actually used by the classifier
                        if (used_classIds == null || 
                                used_classIds.contains(classId)){
                            // Store the class assignment
                            id_class_map.put(item.getId(), classId);
                            // Manage the item count in this class
                            Integer count = class_count_map.get(classId);
                            if (count == null)
                                count = 0;
                            count++;
                            class_count_map.put(classId, count);
                        }
                    }
                }
                // Report updates after processing 1M lines
                if ((start + burst) % 1000000 == 0)
                    LOG.info("{}", (start + burst));
            }
            catch (IOException e) {
                e.printStackTrace();
                System.exit(-1);
            }
            catch (Exception e) {
                LOG.error("Exception: {}", e.getMessage());
                e.printStackTrace();
            }
            // Clear the local data cache
            data.clear();
            // Return the result
            return new SimIndexHelperResult(id_class_map, class_count_map);
        }
    }
    
    /**
     * Helper class that gathers the results for a part of the data. This
     * allows the input data to be processed by multiple threads.
     */
    private class SimIndexHelperResult {
        
        /**
         * Mapping between the line numbers and the class they belong to.
         */
        private Map<Integer, Integer> line_class_map;
        
        /**
         * Mapping between the classes and the number of items in them.
         */
        private Map<Integer, Integer> class_count_map;

        /**
         * @return a map with class - item count pairs
         */
        public Map<Integer, Integer> getClass_count_map() {
            return class_count_map;
        }

        /**
         * @return a map with line numbers - class assignment pairs
         */
        public Map<Integer, Integer> getLine_class_map() {
            return line_class_map;
        }

        /**
         * Constructor.
         * @param line_class_map a map with line numbers - class assignment 
         * pairs
         * @param class_count_map a map with class - item count pairs 
         */
        public SimIndexHelperResult(Map<Integer, Integer> line_class_map, 
                Map<Integer, Integer> class_count_map) {
            this.line_class_map = line_class_map;
            this.class_count_map = class_count_map;
        }
    }
    
    /**
     * Get an absolute path to the current batch directory.
     * @param parameters Similarity parameters.
     * @param classId ClassID for which you want to get the current 
     * batch directory.
     * @return the absolute path to the current batch directory.
     */
    private static String getBatchDir(SimilarityParameters parameters, 
            int classId) {
        return parameters.getSimilarityDirectory() 
                + (classId / open_file_limit) + "/";
    }
    /**
     * Get the absolute path for a given index file.
     * @param parameters Similarity parameters.
     * @param classId ClassID for which you want to get the file path.
     * @return the absolute path for a given index file.
     */
    public static String getIndexFile(SimilarityParameters parameters, 
            int classId) {
        return getBatchDir(parameters, classId) + "index." + classId;
    }
    
        /**
     * Load the similarity data from file.
     * @param filename Specific index to load
     * @param lineparser Parser class to use to parse the input data
     * @param filter Set of items to filter against
     * @return An array of DataItems to use for similarity search
     */
    public static DataItem[] loadSimilarityIndex(String filename, 
            String lineparser, Set<String> filter) {
        // Prepare the result
        DataItem[] data = null;
        try {
            // Fetch the number of lines to process
            int lines = FileIO.getNumberOfLines(filename);
            // Init the training items to load
            data = new DataItem[lines];
            // Set up the parser
            LineParserDataItemSimilarity parser = (LineParserDataItemSimilarity)
                    Util.getParser(lineparser);
            // Set the filter terms
            parser.setFilter(filter);
            // Open the input
            BufferedReader in = new BufferedReader(new FileReader(filename));
            // Read the input
            in.readLine(); // skip the line count on line one
            int id = 0;
            String line = in.readLine(); // skip 
            while (line != null) {
                data[id++] = parser.parse(line);
                line = in.readLine();
            }
            // Close the input
            in.close();
        } catch (IOException e) {
            LOG.error("IOException: {}", e.getMessage());
        }
        return data;
    }
}