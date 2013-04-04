package be.ugent.intec.ibcn.geo.features;

import be.ugent.intec.ibcn.geo.common.Util;
import be.ugent.intec.ibcn.geo.common.datatypes.DataItem;
import be.ugent.intec.ibcn.geo.common.io.FeaturesIO;
import be.ugent.intec.ibcn.geo.common.io.FileIO;
import be.ugent.intec.ibcn.geo.common.io.parsers.LineParserDataItem;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

/**
 * This class contains an algorithm to rank features based
 * on the methods described by the Working Notes paper of TUDelft at the
 * 2011 Placing Task by Claudia Hauff.
 * 
 * For the actual paper
 *  @see http://ceur-ws.org/Vol-807/Hauff_WISTUD_Placing_me11wn.pdf
 * 
 * Also see
 *  @see http://www.sciencedirect.com/science/article/pii/S002002551300162X#s0090
 * 
 * If you use this code for academic research, please cite the paper above.
 *
 * This implementation will 'batch' process a large number of features. This can
 * be done in memory if a sufficient amount of memory is available (e.g. 16GB or ram).
 * In that case, up to 20M features can be 'tracked' and calculated. If more than
 * 20M unique features are used, the batch process will come into play:
 * - the code will loop over the input file, assigning and counting features
 * to the grid
 * - the geoscore will be calculated for those features
 * - once finished, the code will loop again over the input file for the next
 * 20M features, etc.
 * 
 * The parameter BATCH size can be set using the BATCH_LIMIT static variable.
 */
public class GeoSpreadFeatureRanker {

    /**
     * Number of threads, for multi-threaded processing.
     */
    private static final int NR_THREADS = Runtime.getRuntime().availableProcessors();

    /**
     * @return the name of this ranking method
     */
    protected String getMethodName() {
        return "Geospread";
    }
    
    /**
     * Constant defining the horizontal and vertical merge method.
     */
    public static final int MERGE_HORIZONTAL_VERTICAL = 0;
    
    /**
     * Constants defining the diagonal merge method.
     */
    public static final int MERGE_DIAGONAL = 1;
    
    /**
     * Variable that sets the merge method to be used.
     */
    protected int merge_method = MERGE_HORIZONTAL_VERTICAL;
    
    /**
     * Set the merge method to be used
     * @param method One of the valid, existing constants for merging
     */
    public void setMergeMethod(int method) {
        this.merge_method = method;
    }
    
    /**
     * Constant holding the number of items to be loaded each time before
     * publishing progress.
     */
    private static final int REPORT_SIZE_LOAD = 100000;
    
    /**
     * Constant holding the number of items to be processed each time before
     * publishing progress.
     */
    private static final int REPORT_SIZE_PROCESS = 10000;
    
    /**
     * Number of tags to process by each thread individually. The lower the number,
     * the less likely the change all other threads will be waiting for the one
     * thread processing the 'hardest' cases (i.e. tags with a significantly larger
     * number of occurrences).
     */
    private int thread_batch_size = 100;
    
    /**
     * Set the number of tags to process by each thread individually. The lower 
     * the number, the less likely the change all other threads will be waiting 
     * for the one thread processing the 'hardest' cases (i.e. tags with a 
     * significantly larger number of occurrences).
     * @param size Number of tags to process by each thread individually.
     */
    public void setThreadBatchSize(int size) {
        this.thread_batch_size = size;
    }
    
    /**
     * The maximum number of tags to process in one run through the training file.
     * The value of this variable is determined by the amount of available memory
     * to the virtual machine. The higher the number, the faster the ranking will
     * be over, unless you run out of memory. Limiting this value will make the
     * code batch over the input file multiple times (which can be a pain if the
     * training contains e.g. 60M items), but you will be able to compute an 
     * overall ranking.
     */
    private int batch_limit = 20000000;
    
    /**
     * Set the maximum number of tags to process in one run through the training file.
     * @param limit the maximum number of tags to process in one run through the training file.
     */
    public void setBatchLimit(int limit) {
        this.batch_limit = limit;
    }
    
    /**
     * Table holding maps of tag - count pairs, to keep track of occurrences
     * of tags along the grid over the world.
     */
    private HashMap<CellPoint, Integer> [] stats;

    /**
     * On the fly tag (likely String) to ID mapping.
     */
    private Map<Object, Integer> feature_id;
    
    /**
     * Revert id to tag mapping.
     */
    private Map<Integer, Object> id_feature;
    
    /**
     * Size of the grid over the world, in the longitude direction.
     */
    private int grid_x;
    
    /**
     * Size of the grid over the world, in the latitude direction. 
     */
    private int grid_y;
    
    /**
     * Default constructor. This will use a grid of 1 degree lat/lon.
     */
    public GeoSpreadFeatureRanker() {
        this(180, 360);
    }
    
    /**
     * Constructor.
     * @param grid_x the number of grid cells in the x direction (longitude)
     * @param grid_y the number of grid cells in the y direction (latitude)
     */
    public GeoSpreadFeatureRanker(int grid_x, int grid_y) {
        this.grid_x = grid_x;
        this.grid_y = grid_y;
    }

    /**
     * Actual feature ranking method.
     * @param inputFile Training file to process
     * @param lineparserClassName Class name of the line parser implementation to use for parsing training lines
     * @param limit Possible limit to the number of lines to process from the input (-1 means no limit)
     * @param outputfile Outputfile for the feature ranking
     */
    public void process(String inputFile, String lineparserClassName, int limit, String outputfile) {
        System.out.println("Doing "+ getMethodName() +" feature selection.");
        // Publish the batch limit value
        System.out.println("BATCH LIMIT " + batch_limit);
        // Instantiate the parser
        LineParserDataItem parser = (LineParserDataItem)Util.getParser(lineparserClassName);
        // Prepare a map for the tags and their scores
        Map<Object, GeoScore> spread_map = new HashMap<Object, GeoScore>();
        // Prepare a list of batches to process later on, if we would need to
        // process too many tags. We cannot track x/y for all tags in that case
        List<Set<Object>> additional_batches = new ArrayList<Set<Object>>();
        // Tracking stats of the tags
        List<HashMap<CellPoint,Integer>> tmpstats = null;
        int counter = 0;
        int lines = 0;
        // Determine scale factors
        double scale_rows = grid_x / 180.;
        double scale_columns = grid_y / 360.;
        
        try {
            // Fetch the number of lines to process
            lines = FileIO.getNumberOfLines(inputFile);
            // Determine the number of lines to process in case a limit was set
            if (limit > 0 && limit < lines)
                lines = limit;
            
            // Prepare the superset of unique tags we have seen so far
            Set<Object> superset = new HashSet<Object>(lines);
            // The current batch of data to process next
            Set<Object> batch = new HashSet<Object>();
            
            // Create the feature - id mapping and reverse mapping
            this.feature_id = new HashMap<Object, Integer>(batch_limit);
            this.id_feature = new HashMap<Integer, Object>(batch_limit);
            // 
            tmpstats = new ArrayList<HashMap<CellPoint,Integer>>(lines);
            
            System.out.println("Reading and parsing lines from datafile: " + 
                    (limit > 0 && limit < lines ? limit + " ++ LIMITED BY VARIABLE ++" : lines));
            // Read the training data
            BufferedReader in = new BufferedReader(new FileReader(inputFile));
            String line = in.readLine();
            // Start the timer
            long start = System.currentTimeMillis();
            while (line != null) {
                // Parse the line
                DataItem item = parser.parse(line);
                if (item != null) {
                    double lat = item.getLatitude();
                    double lon = item.getLongitude();
                
                    // Determine cell values
                    int stat_x = Math.min((int)(Math.floor(lat + 90) * scale_rows), grid_x - 1);
                    int stat_y = Math.min((int)(Math.floor(lon + 180)* scale_columns), grid_y - 1);
                    // Fetch the item payload
                    String [] data = (String[])item.getData();
                    // If there are tags
                    if (data.length > 0) {
                        // Process each of the tags
                        for (String tag : data) {
                            // If the feature is to be processed
                            if (tag.trim().length() > 0 && !tag.trim().equals("\t")) {
                                // Add to the overall spread map
                                Integer id = this.feature_id.get(tag);
                                // Only allow new tags below the limit
                                if (id == null && this.feature_id.size() < batch_limit) {
                                    // Keep track of the tag in the superset
                                    superset.add(tag);
                                    id = feature_id.size();
                                    this.feature_id.put(tag, id);
                                    this.id_feature.put(id, tag);
                                }
                                // Process if we have an id - which means it is in the current batch
                                if (id != null) {
                                    // Determine the cellpoint
                                    CellPoint cp = new CellPoint(stat_x, stat_y);

                                    HashMap<CellPoint, Integer> map;
                                    // If we have not seen any stats so far for this id
                                    if (tmpstats.size() <= id) {
                                        map = new HashMap<CellPoint, Integer>();
                                        tmpstats.add(map);
                                    }
                                    else
                                        // We can fetch the stats for this tag
                                        map = tmpstats.get(id);
                                    // Fetch the current count
                                    Integer count = map.get(cp);
                                    if (count == null)
                                        // init if new
                                        map.put(cp, 1);
                                    else
                                        // increment if exists
                                        map.put(cp, count + 1);
                                }
                                // Id is to high, has to go to the next batch
                                else {
                                    // New tag
                                    if (!superset.contains(tag)) {
                                        superset.add(tag);
                                        batch.add(tag);
                                    }
                                    // If the batch is full
                                    if (batch.size() == batch_limit) {
                                        // add another batch
                                        additional_batches.add(batch);
                                        batch = new HashSet<Object>();
                                    }                                    
                                }
                            }
                        }
                    }
                    counter++;
                    // Do we need to report some progress
                    if (counter % REPORT_SIZE_LOAD == 0) {
                        long stop = System.currentTimeMillis();
                        // Print time, total unique features so far, and the 
                        // number of batches it will take to process them
                        System.out.println("-> " + counter + "\tTime: " + 
                                (stop-start) + "\tFeatures: " + superset.size() + 
                                " ("+(superset.size()/batch_limit) + ")");
                        start = System.currentTimeMillis();
                    }
                }
                // If there was a processing limit set and we just hit it
                if (limit >= 0 && counter == limit)
                    // stop reading input lines
                    break;
                line = in.readLine();
            }
            in.close();
            // If there was a pending batch of data
            if (batch.size() > 0) {
                // Store it for processing
                additional_batches.add(batch);
                // Start a new batch
                batch = new HashSet<Object>();
            }
        }
        catch (IOException e) {
            System.err.println("IOException: " + e.getMessage());
            System.exit(1);
        }
        // At this point, we have tracked all tags for the current batch
        
        // So we can calculate the actual scores for the tracked tags
        calculateScores(tmpstats, spread_map, additional_batches.size());
        
        /*
         * Below follows similar code to process the pending batches, this can
         * be coded better, but I didn't have the time to generalize this more
         * at the moment.
         */
        
        int batchcounter = 0;
        // Process additional batches - similar to the code above
        for (Set<Object> current_batch : additional_batches) {
            try {
                batchcounter++;
                BufferedReader in = new BufferedReader(new FileReader(inputFile));
                System.out.println("//========================================\\\\");
                System.out.println("\tExtra batch " + batchcounter + "/" + additional_batches.size());
                System.out.println("\\\\========================================//");
                counter = 0;
                // Prepare tools for this batch
                this.feature_id = new HashMap<Object, Integer>(batch_limit);
                this.id_feature = new HashMap<Integer, Object>(batch_limit);
                tmpstats = new ArrayList<HashMap<CellPoint,Integer>>(lines);
                // Get first line
                String line = in.readLine();
                long start = System.currentTimeMillis();
                while (line != null) {
                    DataItem item = parser.parse(line);
                    if (item != null) {
                        double lat = item.getLatitude();
                        double lon = item.getLongitude();

                        // Determine cell values
                        int stat_x = Math.min((int)(Math.floor(lat + 90) * scale_rows), grid_x - 1);
                        int stat_y = Math.min((int)(Math.floor(lon + 180)* scale_columns), grid_y - 1);
                                                
                        String [] data = (String[])item.getData();
                        if (data.length > 0) {
                            for (String tag : data) {
                                // If the feature is to be processed
                                if (tag.trim().length() > 0 && !tag.trim().equals("\t") 
                                        && current_batch.contains(tag)) {
                                    // Add to the overall spread map
                                    Integer id = this.feature_id.get(tag);
                                    if (id == null) {
                                        id = feature_id.size();
                                        this.feature_id.put(tag, id);
                                        this.id_feature.put(id, tag);
                                    }

                                    CellPoint cp = new CellPoint(stat_x, stat_y);

                                    HashMap<CellPoint, Integer> map = null;
                                    if (tmpstats.size() <= id) {
                                        map = new HashMap<CellPoint, Integer>();
                                        tmpstats.add(map);
                                    }
                                    else
                                        map = tmpstats.get(id);

                                    Integer count = map.get(cp);
                                    if (count == null)
                                        map.put(cp, 1);
                                    else
                                        map.put(cp, count + 1);
                                }
                            }
                        }
                        counter++;
                        if (counter % REPORT_SIZE_LOAD == 0) {
                            long stop = System.currentTimeMillis();
                            System.out.println("-> " + counter + "\tTime: " + (stop-start));
                            start = System.currentTimeMillis();
                        }
                    }
                    if (limit > 0 && counter == limit)
                        break;
                    line = in.readLine();
                }
                in.close();
            }
            catch (IOException e) {
                System.err.println("IOException: " + e.getMessage());
                System.exit(1);
            }
            // Again, at this point, we have tracked all tags for the current batch
            // So we can calculate the actual scores for the tracked tags
            calculateScores(tmpstats, spread_map, additional_batches.size());
        }
        
        // At this point, all batches are process and spread_map contains the 
        // overall spread scores
        
        // Actual feature ranking
        System.out.println("Sorting...");
        // Sort the map by value ascending
        spread_map = Util.sortByValueAscending(spread_map);
        // Fetch the features
        List<Object> features = new ArrayList(spread_map.keySet());
        // Export the features to file
        FeaturesIO.exportFeaturesToFile(features, outputfile);        
    }
    
    /**
     * Helper method that converts tag tracking in cells to actual spread scores.
     * @param tmpstats The stats (x,y occurrences) for the tags in the current batch
     * @param spread_map The map that should contain the results of tag, geoscore pairs
     * @param batches_to_follow The number of batches that follow, just for output purposes
     */
    private void calculateScores(List<HashMap<CellPoint,Integer>> tmpstats, 
            Map<Object, GeoScore> spread_map, int batches_to_follow) {
        // Convert tmpstats to an array - for multi-threaded accessing without locking
        this.stats = tmpstats.toArray(new HashMap[0]);
        
        System.out.println("Total features: " + this.feature_id.keySet().size());
        // Notify how many batches will follow
        if(batches_to_follow > 0)
            System.out.println(" - will process " + batches_to_follow + " more batches...");

        // Create an executor service and process things with multiple threads
        ExecutorService executor = Executors.newFixedThreadPool(NR_THREADS);
        List<Future<Map<Object, GeoScore>>> list = new ArrayList<Future<Map<Object, GeoScore>>>();
        // Schedule the threads to process the current stats, 'thread_batch_size' a time
        for (int begin = 0; begin < this.stats.length; begin += thread_batch_size) {
            int end = Math.min(begin + thread_batch_size, this.stats.length);
            // Queue the helpers, track the futures, execute
            Callable<Map<Object, GeoScore>> worker = new GeoSpreadCalculatorHelper(begin, end);
            Future<Map<Object, GeoScore>> submit = executor.submit(worker);
            list.add(submit);
        }
        int resultcounter = 0;
        // Join threads and process results
        for (Future<Map<Object, GeoScore>> future : list) {
            try {
                // Get the map with the results local to the Callable
                Map<Object, GeoScore> local_map = future.get();
                // For each of those tags
                for (Object tagid : local_map.keySet()) {
                    // Resolve the id
                    Object tag = this.id_feature.get((Integer)tagid);
                    // Sanity check
                    if (!((String)tag).equals(""))
                        // put the score in the spread map
                        spread_map.put(tag, local_map.get(tagid));
                }
                resultcounter++;
                // Report progress as needed
                if ((resultcounter * thread_batch_size) % REPORT_SIZE_PROCESS == 0)
                    System.out.println((resultcounter * thread_batch_size));
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
    }
    
    /**
     * Private helper class (Callable) for multi-threaded processing
     */
    private class GeoSpreadCalculatorHelper implements Callable<Map<Object, GeoScore>>{

        /**
         * Start index.
         */
        private int begin;

        /**
         * End index.
         */
        private int end;

        /**
         * Constructor.
         */
        public GeoSpreadCalculatorHelper(int begin, int end) {
            this.begin = begin;
            this.end = end;
        }

        /**
         * Actual spread calculation.
         * @return a map containing the processed tags and their spread scores
         * @throws Exception
         */
        @Override
        public Map<Object, GeoScore> call() throws Exception {
            Map<Object, GeoScore> map = new HashMap<Object, GeoScore>();
            // For all the data this thread is to process
            for (int i = begin; i < end; i++) {
                // Calculate the spread score
                map.put(i, calculateSpreadScore(i));
            }
            return map;
        }
        
        /**
         * Actual spread score calculation
         * @param tag Tag to process
         * @return a double value with the geographic spread score
         */
        private GeoScore calculateSpreadScore(int tagid) {
            // Init a list of clusters that have a non-zero occurence count
            List<Cluster> clusters = new ArrayList<Cluster>();
            Map<CellPoint, Integer> tagOccurences = stats[tagid];
            for (CellPoint cp : tagOccurences.keySet()) {
                int count = tagOccurences.get(cp);
                clusters.add(new Cluster(cp, count));
            }
            boolean changed = true;
            // While there are still changes in the clustering
            while (changed) {
                changed = false;
                List<Cluster> clustera_copy = new ArrayList<Cluster>(clusters);
                // Init a list of invalidated clusters that can no longer be used
                Set<Cluster> invalidated = new HashSet<Cluster>();
                for (Cluster cluster_a : clustera_copy) {
                    // If it is a valid cluster
                    if (!invalidated.contains(cluster_a)) {
                        List<Cluster> clusterb_copy = new ArrayList<Cluster>(clusters);
                        for (Cluster cluster_b : clusterb_copy) {
                            // If the clusters a and b are 'connected'
                            if (cluster_a != cluster_b && cluster_a.isConnected(cluster_b)) {
                                // Merge them
                                cluster_a.merge(cluster_b);
                                // Remove b
                                clusters.remove(cluster_b);
                                // Invalidate b
                                invalidated.add(cluster_b);
                                // Flag change
                                changed = true;
                            }

                        }
                    }
                }
            }
            int max_count = -1;
            // Determine max count among all clusters
            for (Cluster cluster : clusters) {
                max_count = Math.max(max_count, cluster.count);
            }
            // calculate the actual geospread score
            double score = clusters.size() * 1. / max_count;
            // return a geoscore object
            return new GeoScore(clusters.size(), max_count, score);
        }
    }
    
    /**
     * Helper class representing a geoscore. This object contains the number of 
     * connected individual components involved, the max count over the components
     * and the actual score.
     */
    private class GeoScore implements Comparable<GeoScore> {
        
        /**
         * Number of interconnected components.
         */
        private int components;
        
        /**
         * The maximum tag count found in one of the components.
         */
        private int maxcount;
        
        /**
         * The actual geospread score.
         */
        private double score;
        
        /**
         * Constructor.
         * @param components the number of interconnected components
         * @param maxcount the maximum tag count found in one of the components
         * @param score the actual geospread score
         */
        public GeoScore(int components, int maxcount, double score) {
            this.components = components;
            this.maxcount = maxcount; 
            this.score = score;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final GeoScore other = (GeoScore) obj;
            if (this.components != other.components) {
                return false;
            }
            if (this.maxcount != other.maxcount) {
                return false;
            }
            if (Double.doubleToLongBits(this.score) != Double.doubleToLongBits(other.score)) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 29 * hash + this.components;
            hash = 29 * hash + this.maxcount;
            hash = 29 * hash + (int) (Double.doubleToLongBits(this.score) ^ (Double.doubleToLongBits(this.score) >>> 32));
            return hash;
        }
        
        @Override
        public int compareTo(GeoScore t) {
            if (this.score < t.score)
                return -1;
            else if (this.score > t.score)
                return 1;
            else
                return 0;
        }

        /**
         * toString implementation.
         */
        @Override
        public String toString() {
            return "GeoScore{" + "components=" + components + ", maxcount=" + maxcount + ", score=" + score + '}';
        }
    }

    /**
     * Private helper class representing a mini cluster in the grid.
     */
    private class Cluster {

        /**
         * Occurrences of a tag in this cluster.
         */
        private int count;

        /**
         * List containing the specific cellpoints of the grid.
         */
        private List<CellPoint> cellpoints = new ArrayList<CellPoint>();

        /**
         * Returns a List containing the specific cellpoints of the grid.
         * @return a List containing the specific cellpoints of the grid
         */
        public List<CellPoint> getCellPoints() {
            return this.cellpoints;
        }

        /**
         * Constructor.
         * @param p Initial cellpoint
         * @param count Initial count
         */
        public Cluster(CellPoint p, int count) {
            this.cellpoints.add(p);
            this.count = count;
        }

        /**
         * Merge another cluster c.
         * @param c cluster c
         */
        public void merge(Cluster c) {
            // Add all the other cellpoints
            cellpoints.addAll(c.cellpoints);
            // Retain the max count value of both clusters
            this.count = Math.max(c.count, this.count);
        }

        /**
         * Determine if two clusters are connected
         * @param c other cluster
         * @return true if both clusters are connected
         */
        public boolean isConnected(Cluster c) {
            // For all the points of this
            for (CellPoint cellPoint : cellpoints) {
                // For all the points of c
                for (CellPoint cellPointOther : c.cellpoints) {
                    // Depending on the merge method
                    switch (merge_method) {
                        // Are the up/down/left/right connected
                        case MERGE_HORIZONTAL_VERTICAL:
                          if ((cellPoint.x == cellPointOther.x &&
                              (cellPoint.y == cellPointOther.y + 1 ||
                              cellPoint.y == cellPointOther.y - 1))
                             ||
                              (cellPoint.y == cellPointOther.y &&
                              (cellPoint.x == cellPointOther.x + 1 ||
                              cellPoint.x == cellPointOther.x - 1))) {
                            return true;
                          }
                          break;
                        // Are they on 1 of 8 sides connected
                        case MERGE_DIAGONAL:
                          if (cellPointOther.x == cellPoint.x - 1 && cellPointOther.y == cellPoint.y - 1 ||
                            cellPointOther.x == cellPoint.x - 1 && cellPointOther.y == cellPoint.y ||
                            cellPointOther.x == cellPoint.x - 1 && cellPointOther.y == cellPoint.y + 1 ||
                            cellPointOther.x == cellPoint.x  && cellPointOther.y == cellPoint.y - 1 ||
                            cellPointOther.x == cellPoint.x  && cellPointOther.y == cellPoint.y + 1 ||
                            cellPointOther.x == cellPoint.x + 1 && cellPointOther.y == cellPoint.y - 1 ||
                            cellPointOther.x == cellPoint.x + 1 && cellPointOther.y == cellPoint.y ||
                            cellPointOther.x == cellPoint.x + 1 && cellPointOther.y == cellPoint.y + 1
                          ) {
                              return true;
                          }
                          break;
                    }
                }
            }
            // If not, return false
            return false;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Cluster other = (Cluster) obj;
            if (this.count != other.count) {
                return false;
            }
            if (this.cellpoints != other.cellpoints && (this.cellpoints == null || !this.cellpoints.equals(other.cellpoints))) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 67 * hash + this.count;
            hash = 67 * hash + (this.cellpoints != null ? this.cellpoints.hashCode() : 0);
            return hash;
        }

        /**
         * toString override
         * @return a String representation of this object.
         */
        @Override
        public String toString() {
            return cellpoints + " ("+ count +")";
        }
    }

    /**
     * Private helper class representing a grid cell
     */
    private class CellPoint {

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final CellPoint other = (CellPoint) obj;
            if (this.x != other.x) {
                return false;
            }
            if (this.y != other.y) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 83 * hash + this.x;
            hash = 83 * hash + this.y;
            return hash;
        }

        /**
         * x coordinate
         */
        private int x;

        /**
         * Get the x-coordinate
         * @return the x-coordinate
         */
        public int getX() {
            return this.x;
        }

        /**
         * y coordinate
         */
        private int y;

        /**
         * Get the y-coordinate
         * @return the y-coordinate
         */
        public int getY() {
            return this.y;
        }

        /**
         * Constructor
         * @param x coordinate
         * @param y coordinate
         */
        public CellPoint(int x, int y) {
            this.x = x;
            this.y = y;
        }

        /**
         * toString override
         * @return a String representation of this object.
         */
        @Override
        public String toString() {
            return "[" + x + "," + y + ']';
        }
    }
}