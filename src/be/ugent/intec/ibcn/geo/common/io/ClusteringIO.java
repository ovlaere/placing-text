package be.ugent.intec.ibcn.geo.common.io;

import be.ugent.intec.ibcn.geo.clustering.datatypes.Cluster;
import be.ugent.intec.ibcn.geo.common.datatypes.Point;
import be.ugent.intec.ibcn.geo.common.io.parsers.LineParserPoint;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * This class provides the necessary IO methods to read training data from file,
 * using the generic LineParserPoint interface. Secondly, this class provides the
 * IO code to write the clustering back to file.
 * 
 * The number of training lines is expected to be the first line
 * of the training file. If this number is not found (currently detected by a
 * parse error), the number of lines in the file are counted (for large files, 
 * this may introduce a significant delay!).
 * 
 * The current IO for clustering, loads all the training items as Point objects
 * INTO AN ARRAY, so this component might be a bottleneck if you try to cluster
 * tens of millions training items. Be sure to set your -Xms and -Xmx memory sizes
 * to appropriate values.
 * 
 * Also, the current IO loading divides the training data into a number of blocks.
 * Due to this, the last thread, reads the input file and skips N-1 blocks of data
 * from the start. This can be optimized by using a semaphore/lock, as has been
 * implemented in other parts of this framework. This would allow each thread to
 * fetch for instance 25000 lines to process, until the file is at the end.
 * 
 * @author Olivier Van Laere <oliviervanlaere@gmail.com>
 */
public class ClusteringIO {

    /**
     * Holds the number of available threads.
     */
    private static final int NR_THREADS = Runtime.getRuntime().availableProcessors();
    
    /**
     * Variable holding the number of training items to process before reporting
     * progress to the console.
     */
    private static final int REPORT_SIZE = 100000;
    
    /**
     * Counter for the number of processed items.
     */
    private int processed = 0;
    
    /**
     * Timestamp for the beginning of loading the training data.
     */
    private long t1;
    
    /**
     * Helper function that reports the current process to the console.
     */
    private synchronized void report() {
        // process a new update of REPORT_SIZE items that were processed.
        processed += REPORT_SIZE;
        // If we have process one million items, print this
        if (processed % 1000000 == 0)
            System.out.print((processed / 1000000) + "M");
        else
            // Else just print a dot
            System.out.print(".");
        // If we have processed 5 million items, 
        if (processed % 5000000 == 0) {
            long time = System.currentTimeMillis() - t1;
            t1 = System.currentTimeMillis();
            // Print the time it took, just to give an idea on completion time
            System.out.println(" ( "+time+" ms.)");
        }
    }
    
    /**
     * Load all the data from file.
     * @param filename File to read the data from
     * @param lineparser Classname of the lineparser implementation to use
     * @return an array
     */
    public Point[] loadDataFromFile(String filename, String lineparser) {
        return loadDataFromFile(filename, lineparser, -1);
    }
    
    /**
     * Load data from file.
     * @param filename File to read the data from
     * @param lineparser Classname of the lineparser implementation to use
     * @param limit -1 means all lines, a value >= 0 means a limit on the lines to process
     * @return an array
     */
    public Point[] loadDataFromFile(String filename, String lineparser, int limit) {
        System.out.println("=| Parser:  " + lineparser);
        // Fetch the number of lines to process
        int lines = getNumberOfLines(filename);
        // Determine the number of lines to process in case a limit was set
        if (limit > 0 && limit < lines)
            lines = limit;
        // Prepare the array with the results
        Point[] data = new Point[lines];
        // Print out the number of lines the code is going to read, and if this
        // is limited by a threshold, print this as well, to enable verification
        System.out.println("Loading " + lines + " data items..." + 
                (lines == limit ? " ++ LIMITED BY VARIABLE ++" : ""));
        // Start the timer
        long start = System.currentTimeMillis();
        this.t1 = start;
        // Create the thread pool
        ExecutorService executor = Executors.newFixedThreadPool(NR_THREADS);
        // Create a list of DataLoaderHelper
        List<DataLoaderHelper> helpers = new ArrayList<DataLoaderHelper>();
        // Determine the block length for each thread to process
        int length = (int) (data.length * 1.0 / NR_THREADS);
        for (int i = 0; i < NR_THREADS; i++) {
            // +1 because of the initial line containing the line count of the file
            int begin = i * length + 1;
            // If this is the last thread
            if (i == NR_THREADS - 1) {
                // The block length can be determined otherwise
                length = data.length - (i * length);
            }
            // Determine end value
            int end = begin + length;
            // Init the helper thread with a reference to the data, begin, end, filename and parser
            DataLoaderHelper helper = new DataLoaderHelper(data, begin, end, filename, lineparser);
            // submit the helper
            executor.submit(helper);
            // track the helper instance
            helpers.add(helper);
        }
        // This will make the executor accept no new threads
        // and finish all existing threads in the queue
        executor.shutdown();
        // Wait until all threads are finish
        while (!executor.isTerminated()) {}
        System.out.println();
        // Stop the timer
        long stop = System.currentTimeMillis();
        
        int parser_errors = 0;
        int parser_processed = 0;
        // Fetch processed and error counters from the helper
        for (DataLoaderHelper helper : helpers) {
            parser_errors += helper.getErrors();
            parser_processed += helper.getProcessed();
        }
        
        int counter = 0;
        // Check some stats for sanity purposes
        for (Point item : data) {
            if (item != null) {
                counter++;
            }
        }
        System.out.println("Loading complete. Non-null items: "+counter+" (" + (stop - start) + " ms.).");
        System.out.println("+ Parser processed: " + parser_processed);
        System.out.println("+ Parser errors   : " + parser_errors);
        
        return data;
    }

    /**
     * Find out the number of lines in the given file.
     * The linecount is supposed to be on the first line, if not, the file will
     * be iterated to count the number of lines.
     * @param filename Filename for which the linecount is requested,
     * @return the linecount of the given file
     */
    private int getNumberOfLines(String filename) {
        // Default to -1;
        int lines = -1;
        try {
            BufferedReader in = new BufferedReader(new FileReader(filename));
            // Parse the linecount on the first line
            lines = Integer.parseInt(in.readLine());
            in.close();
        } catch (NumberFormatException e) {
            System.out.println("Linecount not found on first line. Will loop trough the file.");
        } catch (IOException e) {
            System.err.println("IOException: " + e.getMessage());
        }
        // Loop through the file to find out the number of lines
        if (lines < 0) {
            int counter = 0;
            try {
                BufferedReader in = new BufferedReader(new FileReader(filename));
                while (in.readLine() != null)
                    counter++;
                in.close();
                lines = counter;
            } catch (IOException e) {
                System.err.println("IOException: " + e.getMessage());
            }
        }
        // Throw an exception in case something went wrong
        if (lines < 0)
            throw new RuntimeException("Missing or invalid line count for " + filename);
        return lines;
    }
    
    /**
     * Helper class that loads training lines for a part of the training file.
     */
    private class DataLoaderHelper implements Runnable{

        /**
         * Reference to the array that keeps the loaded data. 
         */
        private Point [] data;

        /**
         * Index to start processing the input data.
         */
        private int begin;

        /**
         * Index to stop processing the input data.
         */
        private int end;

        /**
         * Input file to process
         */
        private String filename;
        
        /**
         * LineParserPoint class to use for parsing lines.
         */
        private LineParserPoint parser;
        
        /**
         * @return the number of correctly processed lines.
         */
        public int getProcessed() {
            return parser.getProcessed();
        }

        /**
         * @return the number of parse errors encountered during processing.
         */
        public int getErrors() {
            return parser.getErrors();
        }

        /**
         * Constructor.
         * @param data Reference to the array that needs to contain the results
         * @param begin Start index
         * @param end End index
         * @param filename Filename to process
         * @param lineparserpoint class name of the LineParserPoint implementation to use
         */
        public DataLoaderHelper(Point [] data, int begin, int end, 
                String filename, String lineparserpoint) {
            this.data = data;
            this.begin = begin;
            this.end = end;
            this.filename = filename;
            // Instantiate the LineParserPoint parser
            try {
                this.parser = (LineParserPoint) Class.forName(lineparserpoint).newInstance();
            } catch (InstantiationException ex) {
                System.err.println("InstantiationException " + ex.getMessage());
            } catch (IllegalAccessException ex) {
                System.err.println("IllegalAccessException " + ex.getMessage());
            } catch (ClassNotFoundException ex) {
                System.err.println("ClassNotFoundException " + ex.getMessage());
            }
            // First line of file should contain the number of lines in the file
            if (this.begin == 0)
                this.begin = 1;
        }

        /**
         * run() implementation of this Runnable.
         */
        @Override
        public void run() {
            try {
                // Set up the input
                BufferedReader file = new BufferedReader(new FileReader(filename));
                String line = file.readLine();
                int skipcounter = 0;
                // Skip the necessary number of lines to get to the part of the
                // file this Runnable is going to process
                while (skipcounter++ < begin)
                    line = file.readLine();
                int counter = begin;
                // Set the index
                int index = begin;
                // Process while we have input and while we have not reached end
                while (line != null && counter < end) {
                    // Trim the line, just to be sure
                    line = line.trim();
                    // If something remains
                    if (line.length() > 0)
                        // parse the line and store the result
                        data[index-1] = parser.parse(line);
                    ++counter;
                    // If we need to report progress
                    if ((counter - begin) % REPORT_SIZE == 0)
                        // do so
                        report();
                    // Read the next line
                    line = file.readLine();
                    index++;
                }
                file.close();
            }
            catch (IOException e){
                System.out.println("Error loading data: " + e.getMessage());
                System.exit(0);
            }
        }
    }
    
    /**
     * Helper method that facilitates writing a given grid-clustering to file.
     * @param clusters The resulting clusters from the algorithm
     * @param outputfile The name of the file to write the clusters to.
     */
    public void writeClusteringToFile(Map<Long, Cluster> clusters, String outputfile) {
        writeClusteringToFile(clusters, outputfile, false);
    }

    /**
     * Helper method that facilitates writing a given grid-clustering to file.
     * @param clusters The resulting clusters from the algorithm
     * @param outputfile The name of the file to write the clusters to.
     * @param fullWrite If set to true, not only the ID,lat,lon will be written to file,
     * but all the elements in the cluster as well. This is mainly for visualization and
     * debugging purposes only.
     */
    public void writeClusteringToFile(Map<Long, Cluster> clusters, String outputfile, boolean fullWrite) {
        try {
            PrintWriter file = new PrintWriter(new FileWriter(outputfile));
            for (Cluster c : clusters.values()) {
                if (fullWrite) {
                    file.println(c.toString());
                }
                else {
                    // Write the center data to file
                    Point center = c.getCenter();
                    file.println(center.getId() + "," + center.getLatitude() + "," 
                            + center.getLongitude());
                }
            }
            file.close();
        }
        catch (IOException e) {
            System.err.println("Error writing clustering to file: " + e.getMessage());
            System.exit(-1);
        }
    }
    
    /**
     * Helper method that facilitates writing a given clustering to file.
     * @param clusters The resulting clusters from the algorithm
     * @param outputfile The name of the file to write the clusters to.
     */
    public void writeClusteringToFile(List<Cluster> clusters, String outputfile) {
        writeClusteringToFile(clusters, outputfile, false);
    }
    
    /**
     * Helper method that facilitates writing a given clustering to file.
     * @param clusters The resulting clusters from the algorithm
     * @param outputfile The name of the file to write the clusters to.
     * @param fullWrite If set to true, not only the ID,lat,lon will be written to file,
     * but all the elements in the cluster as well. This is mainly for visualization and
     * debugging purposes only.
     */
    public void writeClusteringToFile(List<Cluster> clusters, String outputfile, boolean fullWrite) {
        try {
            PrintWriter file = new PrintWriter(new FileWriter(outputfile));
            for (Cluster c : clusters) {
                if (fullWrite) {
                    file.println(c.toString());
                }
                else {
                    // Write the center data to file
                    Point center = c.getCenter();
                    file.println(center.getId() + "," + center.getLatitude() + "," 
                            + center.getLongitude());
                }
            }
            file.close();
        }
        catch (IOException e) {
            System.err.println("Error writing clustering to file: " + e.getMessage());
            System.exit(-1);
        }
    }    
}