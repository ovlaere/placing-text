package be.ugent.intec.ibcn.geo.common.io;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.ugent.intec.ibcn.geo.common.Util;
import be.ugent.intec.ibcn.geo.common.datatypes.DataItem;
import be.ugent.intec.ibcn.geo.common.interfaces.AbstractLineParserDataItem;

/**
 * Helper class for specific data loading.
 * 
 * Load the training data, given a certain parser, in a multi-threaded way.
 * The data is loaded into an array (for easy concurrent access), but this
 * requires a lot of memory for large datasets.
 * 
 * A map of features can be added in the constructor (or set to null) if you
 * want to filter the data to only those retained features.
 * 
 * @Deprecated Try to avoid using methods that load the data into memory.
 * There is however, for some parts of the code, not yet an alternative 
 * loading method.
 *
 * @author Olivier Van Laere <oliviervanlaere@gmail.com>
 */
@Deprecated
public class DataLoading {

	/**
	 * Logger.
	 */
	protected static final Logger LOG = LoggerFactory.getLogger(DataLoading.class);
	
    /**
     * Constant holding the number of available threads for multithreading.
     */
    private final int nr_threads = Runtime.getRuntime().availableProcessors();

    /**
     * Constant holding the amount of lines to process before posting updates.
     */
    private static final int REPORT_SIZE = 100000;

    /**
     * Tracks the number of processed lines.
     */
    private int processed = 0;

    /**
     * Global timestamp.
     */
    private long t1;

    /**
     * Method used for reporting data loading updates.
     */
    private synchronized void report() {
        processed += REPORT_SIZE;
        if (processed % 1000000 == 0)
            System.out.print((processed / 1000000) + "M");
        else
            System.out.print(".");

        if (processed % 5000000 == 0) {
            long time = System.currentTimeMillis() - t1;
            t1 = System.currentTimeMillis();
            LOG.info(" ( {} ms.)", time);
        }
    }

    /**
     * Load the training data from file.
     * @param filename Filename of the inputfile
     * @param lineparser Class name of the input parser to use
     * @param limit number of training lines to process or -1 for no limit
     * @param features a map containing the features used for this experiment
     * @return an array of DataItem objects
     */
    public DataItem[] loadDataFromFile(String filename, String lineparser, 
            int limit, Map<Object, Integer> features) {
        LOG.info("=| Parser: {}", lineparser);
        // Get the number of lines.
        int lines = FileIO.getNumberOfLines(filename);
        // Determine the number of lines to read
        if (limit > 0 && limit < lines)
            lines = limit;
        // Prepare the array for the result
        DataItem[] data = new DataItem[lines];
        LOG.info("Loading {} data items..." + (lines == limit ? " ++ LIMITED BY VARIABLE ++" : ""),
        		lines);
        // Start the timer
        long start = System.currentTimeMillis();
        // Prepare the thread pool
        ExecutorService executor = Executors.newFixedThreadPool(nr_threads);
        // Prepare a list to track the helper threads
        List<DataLoaderHelper> helpers = new ArrayList<DataLoaderHelper>();
        int length = (int) (data.length * 1.0 / nr_threads);
        for (int i = 0; i < nr_threads; i++) {
            // +1 because of the initial line should contain the line count of 
            // the file
            int begin = i * length + 1;
            if (i == nr_threads - 1) {
                length = data.length - (i * length);
            }
            int end = begin + length;
            // Instantiate, submit and track the helpers
            DataLoaderHelper helper = new DataLoaderHelper(data, begin, end, 
                    filename, lineparser, features);
            executor.submit(helper);
            helpers.add(helper);
        }
        // This will make the executor accept no new threads
        // and finish all existing threads in the queue
        executor.shutdown();
        // Wait until all threads are finish
        while (!executor.isTerminated()) {}
        LOG.info("");
        // Stop the timer
        long stop = System.currentTimeMillis();
        // Track some stats
        int parser_errors = 0;
        int parser_processed = 0;
        for (DataLoaderHelper helper : helpers) {
            parser_errors += helper.getErrors();
            parser_processed += helper.getProcessed();
        }
        int counter = 0;
        // Check some stats for sanity purposes
        for (DataItem item : data) {
            if (item != null) {
                counter++;
            }
        }
        // Print some stats
        LOG.info("Loading complete. Non-null items: {} ( {} ms.).", 
    		counter, (stop - start));
        LOG.info("+ Parser processed: {}", parser_processed);
        LOG.info("+ Parser errors   : {}", parser_errors);
        // Return the data
        return data;
    }

    /**
     * Private helper class for loading data in a multi-threaded way.
     */
    private class DataLoaderHelper implements Runnable{

        /**
         * The array that should contain the results.
         */
        private DataItem [] data;

        /**
         * Start index for processing.
         */
        private int begin;

        /**
         * End index for processing.
         */
        private int end;

        /**
         * Filename of the input file to load.
         */
        private String filename;

        /**
         * The parser used to parse the input.
         */
        private AbstractLineParserDataItem parser;

        /**
         * @return the number of lines processed by this Runnable.
         */
        public int getProcessed() {
            return parser.getProcessed();
        }

        /**
         * @return the number of parse errors encountered by this Runnable.
         */
        public int getErrors() {
            return parser.getErrors();
        }

        /**
         * Constructor.
         * @param data The data array that will be used to return the result
         * @param begin start index for processing
         * @param end end index for processing
         * @param filename the filename of the input file
         * @param lineparser the package and classname of the input parser
         * @param features a Map containing the features retained for this this 
         * experiment
         */
        public DataLoaderHelper(DataItem [] data, int begin, int end, 
                String filename, String lineparser, 
                Map<Object, Integer> features) {
            this.data = data;
            this.begin = begin;
            this.end = end;
            this.filename = filename;
            this.parser = 
                    (AbstractLineParserDataItem)Util.getParser(lineparser);
            this.parser.setFeatures(features);
            // First line of file should contain the number of lines in the file
            if (this.begin == 0)
                this.begin = 1;
        }

        /**
         * Actual data loading.
         */
        @Override
        public void run() {
            try {
                BufferedReader file = 
                        new BufferedReader(new FileReader(filename));
                String line = file.readLine();
                // Skip lines until we reach the start
                int skipcounter = 0;
                while (skipcounter++ < begin)
                    line = file.readLine();
                int counter = begin;
                int id = begin;
                // While we have valid lines to process - within our bounds
                while (line != null && counter < end) {
                    line = line.trim();
                    if (line.length() > 0)
                        // Parse the line
                        data[id-1] = parser.parse(line);

                    ++counter;
                    // Check for progress reporting
                    if ((counter - begin) % REPORT_SIZE == 0)
                        report();
                    line = file.readLine();
                    id++;
                }
                file.close();
            }
            catch (IOException e){
                LOG.error("Error loading data: {}", e.getMessage());
                System.exit(1);
            }
        }
    }
}