package be.ugent.intec.ibcn.geo.clustering;

import be.ugent.intec.ibcn.geo.clustering.datatypes.Cluster;
import be.ugent.intec.ibcn.geo.common.datatypes.Point;
import be.ugent.intec.ibcn.geo.common.io.ClusteringIO;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * This class provides the implementation of the grid clustering algorithm.
 * 
 * This algorithm is quite simple: the surface of the earth, represented by 
 * 360 degrees of longitude and 180 degrees of latitude is divided according
 * to a grid of grid_precision_rows degrees in latitude and 
 * grid_precision_columns in longitude. Each input point that is located within
 * one of the grid cells, is associated to that cell.
 * 
 * Once all data points are assigned to a cell, each cell is optimized: the
 * medoid is determined, which is the point that minimizes the distance to all
 * other points in the cell.
 * 
 * For more details
 * @see http://www.sciencedirect.com/science/article/pii/S002002551300162X#s0050
 * 
 * @author Olivier Van Laere <oliviervanlaere@gmail.com>
 */
public class GridClustering extends AbstractClustering {

    /**
     * Precision (in degrees) latitude of the grid rows.
     */
    private double grid_precision_rows;

    /**
     * Precision (in degrees) longitude of the grid columns.
     */
    private double grid_precision_columns;

    /**
     * Constructor.
     * @param parameters Clustering parameters
     * @param data Input data to cluster
     * @param grid_precision_rows Precision (in degrees) of the rows
     * @param grid_precision_columns Precision (in degrees) of the columns
     */
    public GridClustering(ClusteringParameters parameters, Point [] data, 
            double grid_precision_rows, double grid_precision_columns) {
        super(parameters);
        this.data = data;
        this.grid_precision_rows = grid_precision_rows;
        this.grid_precision_columns = grid_precision_columns;
    }

    /**
     * Constructor.
     * @param parameters Clustering parameters
     * @param data Input data to cluster
     * @param grid_precision Precision (in degrees) of the rows and columns
     */
    public GridClustering(ClusteringParameters parameters, Point [] data, 
            double grid_precision) {
        this(parameters, data, grid_precision, grid_precision);
    }
    
    /**
     * Implementation of the actual clustering algorithm.
     * @param outputfile Filename of the file that will contain the cluster medoids.
     */
    @Override
    public void cluster(String outputfile) {
        // Start the timer
        long overall_start = System.currentTimeMillis();
        // Perpare a threadpool
        ExecutorService executor = Executors.newFixedThreadPool(NR_THREADS);
        // Prepare the list of futures
        List<Future<Map<Integer, Long>>> list = new ArrayList<Future<Map<Integer, Long>>>();
        // Prepare a map for the clusters
        /**
         * Due to the large number of potential clusters (1 degree lat/1 degree lon)
         * results in 360 * 180 = 64800 possible clusters, and many of them will
         * be empty. So in order to avoid a huge list of sparse object, we use a
         * map and store only those clusters that actually contain data items.
         * To this end, a mapping is needed between the x,y coordinate of the cell
         * in the grid and the Long ID in the list of clusters. See below for more
         * details of the implementation.
         */
        Map<Long, Cluster> clusters = new HashMap<Long, Cluster>();
        // Determine the block length to process by each thread.
        int length = (int) (data.length * 1.0 / NR_THREADS);
        for (int i = 0; i < NR_THREADS; i++) {
            int begin = i * length;
            if (i == NR_THREADS - 1) {
                length = data.length - (i * length);
            }
            int end = begin + length;
            // Create, track and submit the worker runnables, each with their blocks of data
            Callable<Map<Integer, Long>> worker = new GridCallable(data, begin, end);
            Future<Map<Integer, Long>> submit = executor.submit(worker);
            list.add(submit);
        }        
        
        // Now retrieve the results
        for (Future<Map<Integer, Long>> future : list) {
            try {
                Map<Integer, Long> cluster_mapping = future.get();
                // For each id
                for (Integer id : cluster_mapping.keySet()) {
                    long cluster_id = cluster_mapping.get(id);
                    Cluster c = clusters.get(cluster_id);
                    // If there is not yet a cluster
                    if (c == null) {
                        // Create one with this data point as temporary center
                        c = new Cluster(data[id-1]);
                    }
                    else {
                        c.addElement(data[id-1]);
                    }
                    // Store the cluster id and cluster
                    clusters.put(cluster_id, c);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }

        // Now, start optimizing each of the individual clusters
        System.out.println("Determined "+ clusters.size() +" clusters...");
        System.out.println("Optimizing clusters...");

        int cluster_counter = 0;
        int min_size = Integer.MAX_VALUE;
        int max_size = -1;
        int size_check = 0;
        // Create a list to track the optimizer threads
        List<Thread> threads = new ArrayList<Thread>();
        // For each possible cluster
        for (Cluster c : clusters.values()) {
            size_check += c.size();
            // Optimize its center
            Thread t = new Thread(new ClusterOptimizer(c));
            // submit thread
            executor.submit(t);
            // Track thread
            threads.add(t);
            cluster_counter++;
            min_size = Math.min(c.size(), min_size);
            max_size = Math.max(c.size(), max_size);
        }
        // Join the threads
        for (int i = 0; i < threads.size(); i++) {
            try {
                threads.get(i).join();
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
            if (i > 0 && i % 100 == 0)
                System.out.println(" " + i);
        }
        // This will make the executor accept no new threads
        // and finish all existing threads in the queue
        executor.shutdown();
        // Wait until all threads are finish
        while (!executor.isTerminated()) {}

        // Print some stats
        System.out.println("Size check: " + size_check);
        System.out.println("[Grid Clustering] Clustering summary");
        System.out.println(" - Grid precision: " + grid_precision_rows);
        System.out.println(" - Total clusters: " + cluster_counter);
        System.out.println(" - Min cluster size (>0): " + min_size);
        System.out.println(" - Max cluster size: " + max_size);
        System.out.println("[=======================]");

        // Write to file
        ClusteringIO cio = new ClusteringIO();
        cio.writeClusteringToFile(clusters, outputfile);

        // Stop the timer
        long overall_stop = System.currentTimeMillis();

        // Print final stats
        System.out.println(" Overall processing time: " + (overall_stop - overall_start) + " ms.");
    }

    /**
     * Helper class that determines for the input items to which cluster they
     * belong.
     */
    private class GridCallable implements Callable<Map<Integer, Long>>{

        /**
         * The data to process.
         */
        private Point [] data;

        /**
         * Start of the data to process
         */
        private int begin;

        /**
         * End of the data to process
         */
        private int end;

        /**
         * Constructor.
         * @param data Data to process
         * @param begin startindex of the data to process
         * @param end endindex of the tadat to process
         */
        public GridCallable(Point [] data, int begin, int end) {
            this.data = data;
            this.begin = begin;
            this.end = end;
        }

        /**
         * Actual grid clustering
         */
        @Override
        public Map<Integer, Long> call() throws Exception {
            Map<Integer, Long> mapping = new HashMap<Integer, Long>();
            try {
                double scale_rows = 1. / grid_precision_rows;
                double scale_columns = 1. / grid_precision_columns;
                // For all the points this thread needs to process
                for (int i = begin; i < end; i++) {
                    Point p = this.data[i];
                    // Sanity check
                    if (p != null) {
                        // Determine cell values
                        int lat_adapted = (int)((p.getLatitude() + 90) * scale_rows);
                        int lon_adapted = (int)((p.getLongitude() + 180) * scale_columns);
                        // Map to a cluster id using the current scale parameters
                        long id = (long)(lat_adapted * 360 * scale_rows + lon_adapted);
                        if (id < 0)
                            throw new RuntimeException("Long underflow");
                        // Assign this input point to the cluster with the given ID
                        mapping.put(p.getId(), id);
                    }
                }
            }
            catch (Exception e) {
                System.err.println("Exception: " + e.getMessage());
                // Crash and burn
                e.printStackTrace();
                System.exit(1);
            }
            // return the clusters
            return mapping;
        }
    }
}