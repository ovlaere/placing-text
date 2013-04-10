package be.ugent.intec.ibcn.geo.clustering;

import be.ugent.intec.ibcn.geo.clustering.datatypes.Cluster;
import be.ugent.intec.ibcn.geo.clustering.datatypes.InitialPoint;
import be.ugent.intec.ibcn.geo.common.datatypes.Coordinate;
import be.ugent.intec.ibcn.geo.common.datatypes.Point;
import be.ugent.intec.ibcn.geo.common.io.ClusteringIO;
import edu.wlu.cs.levy.CG.KDTree;
import edu.wlu.cs.levy.CG.KeyDuplicateException;
import edu.wlu.cs.levy.CG.KeySizeException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * This class contains an optimized implementation of the Partition Around 
 * Medoids (PAM) algorithm, a k-means variant. 
 * 
 * For more details on the algorithm, 
 * @see http://www.sciencedirect.com/science/article/pii/S002002551300162X#s0045
 * @see http://en.wikipedia.org/wiki/K-medoids
 * 
 * The algorithm makes use of a KD-tree for retrieving the nearest neighbours. 
 * To download the implementation of this data structure
 *  @see http://home.wlu.edu/~levys/software/kd/
 * 
 * To give an idea of the performance using the default configuration: 
 * - to cluster 2.09M training items into 2500 clusters, this implementation 
 * took 25 iterations and 1 633 082 ms (27min 13sec) using a 16 core machine 
 * (Intel(R) Xeon(R) CPU E5620  @ 2.40GHz).
 * 
 * @author Olivier Van Laere <oliviervanlaere@gmail.com>
 */
public class PamClustering extends AbstractClustering {
    
    /**
     * Variable keeping track of changed clusters during the process.
     */
    private boolean clusterChanged = false;

    /**
     * Use this method to inform that there was a change during the last
     * iteration.
     */
    public synchronized void flagChange() {
        if (!clusterChanged)
            this.clusterChanged = true;
        this.clusterChanges++;
    }

    /**
     * Reset the change flag for a new iteration.
     */
    public synchronized void resetChangeFlags() {
        this.clusterChanged = false;
        this.clusterChanges = 0;
    }

    /**
     * @return true if a change happened during the last iteration.
     */
    public synchronized boolean clusterChanged() {
        return this.clusterChanged;
    }

    /**
     * Keep track of the number of changes during the last iteration.
     */
    private int clusterChanges = 0;

    /**
     * @return the number of changes during the last iteration.
     */
    public synchronized int getClusterChanges() {
        return this.clusterChanges;
    }

    /**
     * The dataset of Points
     */
    private List<Point> points;

    /**
     * Holds the number of clusters we are looking for.
     */
    protected int numberOfClusters;

    /**
     * Constructor
     * @param parameters Parameters for the PAM clustering
     * @param data Array of Point data to cluster
     * @param numberOfClusters the required number of clusters
     */
    public PamClustering(PamParameters parameters, Point [] data, 
            int numberOfClusters) {
        super(parameters);
        this.numberOfClusters = numberOfClusters;
        // Convert the input data to a synchronized List (as Vector 
        // is deprecated)
        this.points = Collections.synchronizedList(
                new ArrayList<Point>(Arrays.asList(data)));
    }

    /**
     * Partitioning Around Medoids function.
     * @param outputfile The filename of the results of the clustering
     */
    @Override
    public void cluster(String outputfile) {
        System.out.println("[PAM] Output will be written in " + outputfile);
        // Init a random generator - with seed for reproduceability
        Random rg = new Random(123456789L);
        // Init a ClusteringIO
        ClusteringIO cio = new ClusteringIO();
        
        // Store the original min improvement threshold
        double original_optimization_min_improvement = 
                parameters.optimization_min_improvement;
        // Switch to an improvement threshold for clusters in progress
        parameters.optimization_min_improvement = 
                ((PamParameters)parameters).cost_improvement_threshold_partial;
        
        // Init the overall timer
        long overall_start = System.currentTimeMillis();
        
        /*
         * Step 1. Initialize: randomly select k of the n data points as the
         * mediods.
         */
        System.out.println("[PAM][Algorithm][Step 1] Initial medoid selection. "
                + "(#"+ numberOfClusters + ")");
        // Provide a map to track the current medoids
        Map<Integer, InitialPoint> medoids = 
                new HashMap<Integer, InitialPoint>(numberOfClusters);
        // Start a timer
        long start = System.currentTimeMillis();        
        // make a D-dimensional KD-tree for the Coordinates
        KDTree<Integer> initial_kd = new KDTree<Integer>(3);
        // Now pick our random clusters
        while (medoids.size() < numberOfClusters) {
            // Pick a random index out of the list of all indices
            int index = rg.nextInt(points.size());
            
            boolean valid_candidate = true;
            Point p = points.get(index);
            // If there are already medoids in the KD-tree
            if (initial_kd.size() > 0) {
                try {
                    // Check their distance
                    List<Integer> nbrs = initial_kd.nearest(
                            new Coordinate(p).doubleKey(), 1);
                    // If the current candidate is within 0.001m of a medoid
                    if (p.distance(medoids.get(nbrs.get(0))) <= 1E-3)
                        // Reject it as a valid candidate
                        valid_candidate = false;                    
                } catch (KeySizeException ex) {
                    System.err.println("Error: " + ex.getMessage());
                }
            }
            // If we could add it
            if (valid_candidate && !medoids.containsKey(p.getId())) {
                // Add it to the medoids
                medoids.put(p.getId(), new InitialPoint(p));
                // Remove that element from the points
                points.remove(index);
                try {
                    initial_kd.insert(new Coordinate(p).doubleKey(), p.getId());
                } catch (KeySizeException ex) {
                    System.err.println("Error: " + ex.getMessage());
                } catch (KeyDuplicateException ex) {
                    System.err.println("Error: " + ex.getMessage());
                }
            }
            // Report progress per 10 000 medoids
            if (medoids.size() % 10000 == 0)
                System.out.println(medoids.size());
        }
        // Stop the timer - Init complete
        long stop = System.currentTimeMillis();
        System.out.println(" - Selected " + medoids.size() + " medoids. (Time: "
                + (stop - start) + " ms.)");
        // Create a list of the current clusters
        List<Cluster> clusters = new ArrayList<Cluster>();
        for (Point point : medoids.values()) {
            clusters.add(new Cluster(point));
        }
        // Init the iteration counter
        int iterations = 0;

        /*
         * Step 2. Associate each data point to the closest medoid.
         */
        long iteration_start;
        do {
            // Start the iteration timer
            iteration_start = System.currentTimeMillis();
            // Clear any possible change flags
            resetChangeFlags();
            System.out.println("\n========] Iteration " + ++iterations + 
                    " [========\n");
            start = System.currentTimeMillis();
            for (Cluster cluster : clusters) {
                // Remove all data points
                cluster.clearElements();
            }
            System.out.println("[PAM][Algorithm][Step 2] Assigning " + 
                    points.size() + " datapoints to the closest medoid.");

            // make a D-dimensional KD-tree
            KDTree<Integer> kd = new KDTree<Integer>(3);
            for (int i = 0; i < clusters.size(); i++){
                try {
                    // Insert each medoid
                    Point center = clusters.get(i).getCenter();
                    kd.insert(new Coordinate(center).doubleKey(), i);
                } catch (KeySizeException ex) {
                    System.err.println("Error: " + ex.getMessage());
                } catch (KeyDuplicateException ex) {
                    System.err.println("Error: " + ex.getMessage());
                }
            }

            // For each data point, find the nearest neighbour
            for (Point p  : points) {
                try {
                    List<Integer> nbrs = kd.nearest(
                            new Coordinate(p).doubleKey(), 1);
                    // Fetch the closest cluster
                    Cluster best_cluster = clusters.get(nbrs.get(0));
                    // Add this element to that cluster
                    best_cluster.addElement(p);
                } catch (KeySizeException ex) {
                    System.err.println("Error: " + ex.getMessage());
                }
            }
            // Calculate checksum - just for sanity
            int before_checksum = 0;
            for (Cluster cluster : clusters) {
                before_checksum += cluster.size();
            }
            // Print checksum
            System.out.println("Checksum: " + before_checksum);
            // Mark timer just for stats purposes
            stop = System.currentTimeMillis();
            System.out.println(" - Done. (" + (stop - start) + " ms.)");

            /*
             * Step 3. For each mediod m
             */
            System.out.println("[PAM][Algorithm][Step 3] Improving cluster "
                    + "configurations.");
            // Create a thread pool
            ExecutorService executor = Executors.newFixedThreadPool(NR_THREADS);
            // For each cluster
            for (Cluster cluster : clusters) {
                // start optimizing - this will go multithreaded in a 
                // NR_THREADS * NR_THREADS way
                executor.submit(new PamClusterOptimizer(cluster, points));
            }
            // This will make the executor accept no new threads
            // and finish all existing threads in the queue
            executor.shutdown();
            // Wait until all threads are finish
            while (!executor.isTerminated()) {}
            // Stop the iteration time here
            long iteration_stop = System.currentTimeMillis();
            // and start over again with step 2
            System.out.println("[PAM][Algorithm] Iteration finished in "
                            + (iteration_stop - iteration_start) + " ms.");
            System.out.println(" - " + getClusterChanges()
                            + " clusters changed during iteration.");
            int after_checksum = 0;
            for (Cluster cluster : clusters) {
                after_checksum += cluster.size();
            }
            System.out.println("Checksum: " + after_checksum);
            // This should not happen, unless we have threading issues!
            if (before_checksum != after_checksum) {
                throw new RuntimeException("Checksum differs before and after "
                        + "iteration!");
            }

            // Optionally, merge clusters below a certain threshold
            // And only if there is more than 1 cluster
            if (((PamParameters)parameters).merge_cluster_below_threshold 
                    && clusters.size() > 1) {
                List<Cluster> clusters_below = new ArrayList<Cluster>();
                List<Cluster> new_clusters = new ArrayList<Cluster>();
                for (Cluster cluster : clusters) {
                    // If a cluster is below the threshold
                    if (cluster.size() < 
                            ((PamParameters)parameters).min_cluster_size) {
                        // Store the cluster that is below the threshold
                        clusters_below.add(cluster);
                        // Fetch it's center
                        Point center = cluster.getCenter();
                        // Add it as a data point
                        points.add(center);
                        // Once the cluster is removed, the datapoints will be
                        // reset in the next iteration and everything will start
                        // all over again
                    } 
                    else {
                        // Just copy the cluster without processing
                        new_clusters.add(cluster);
                    }
                }
                // In case all clusters where below the threshold
                if (new_clusters.isEmpty()) {
                    // Get the first one that was below
                    Cluster cluster = clusters_below.get(0);
                    // Remove the center from the points list that was added in
                    // the removal step
                    points.remove(cluster.getCenter());
                    // Use this one for the new cluster configuration
                    new_clusters.add(cluster);
                }
                if (clusters_below.size() > 0) {
                    clusters = new_clusters;
                    // Flag a change so that a new iteration will be required
                    flagChange();
                    System.out.println("\n++++ > Clusters below threshold ("
                                + clusters_below.size() + ") < ++++");
                    System.out.println("++++ > \tMerging Clusters (clusters: "
                                + clusters.size() + ")\t < ++++\n");
                }
            }
            // Write the current iteration to the result file, just in case
            cio.writeClusteringToFile(clusters, outputfile, 
                    parameters.writeFullClusteringToFile);
        }
        // keep doing this for as long as there are changes to the clusters
        // or clusters are being merged
        // and the number of iterations is smaller than the iterationlimit
        while (clusterChanged() && 
                iterations < ((PamParameters)parameters).iterationLimit);

        /*
         * 3. For each mediod m 
         *  1. For each non-mediod data point o 
         *    1. Swap m and o and compute the total cost of the configuration 
         * 4. Select the configuration with the lowest cost. 
         * 5. repeat steps 2 to 5 until there is no change in the medoid.
         */

        long overall_stop = System.currentTimeMillis();

        // Restore the original min improvement threshold
        parameters.optimization_min_improvement = 
                original_optimization_min_improvement;
        
        int clusters_threshold = 0;
        int empty_clusters = 0;
        int min_size = Integer.MAX_VALUE;
        int max_size = 0;

        for (Cluster cluster : clusters) {
            /*
                * Keep track of stats
                */
            if (cluster.size() == 0)
                    empty_clusters++;

            if (cluster.size() < ((PamParameters)parameters).min_cluster_size)
                    clusters_threshold++;

            if (cluster.size() < min_size && cluster.size() > 0)
                    min_size = cluster.size();

            if (cluster.size() > max_size)
                    max_size = cluster.size();
        }
        // Write the final clustering to file
        cio.writeClusteringToFile(clusters, outputfile, 
                parameters.isWriteFullClusteringToFile());

        // Print some stats
        System.out.println("[PAM] Clustering summary");
        System.out.println(" - Finished after " + iterations
                        + " iterations.");
        System.out.println(" - Total clusters: " + clusters.size());
        System.out.println(" - Empty clusters: " + empty_clusters);
        System.out.println(" - Clusters < " + 
                ((PamParameters)parameters).min_cluster_size + ": "
                        + clusters_threshold);
        if (clusters.size() - empty_clusters != 0)
            System.out.println(" - Avg cluster size (non-empty): " + 
                    ((int) ((points.size() * 1.0) + clusters.size()) / 
                    (clusters.size() - empty_clusters)));
        System.out.println(" - Min cluster size (>0): " + min_size);
        System.out.println(" - Max cluster size: " + max_size);
        System.out.println("[=======================]");
        System.out.println(" Overall processing time: "
                        + (overall_stop - overall_start) + " ms.");
    }
    
    /**
     * Helper class that overrides the default Cluster optimizer. This allows
     * to reuse almost all of the code of the optimizer, while with minimal
     * effort, some administration specific to the PAM algorithm can be 
     * executed.
     */
    private class PamClusterOptimizer extends ClusterOptimizer{

        /**
         * Reference to the overall dataset of Points. This has to be a 
         * synchronized structure or the multithreading will loose datapoints 
         * here.
         */
        private List<Point> dataset;

        /**
         * @param cluster the Cluster this Thread is optimizing.
         * @param dataset reference to the overall dataset of Points.
         */
        public PamClusterOptimizer(Cluster cluster, List<Point> dataset) {
            super(cluster);
            this.dataset = dataset;
        }

        /**
         * Override of the original method to be able to invoke only one 
         * iteration, and call flagChange() to schedule a next iteration in the 
         * PAM algorithm.
         * @param bestCenter Best center so far
         * @param cluster Cluster object
         * @param original_cost 
         * @param cost
         * @return always false - no more iterations in the case of PAM 
         * optimization
         */
        @Override        
        protected boolean finalSwap(Point bestCenter, Cluster cluster, 
            double original_cost, double cost) {
            System.out.println("[Thread-" + Thread.currentThread().getId() + 
                    "] " + cluster.getCenter() + " -> " + bestCenter +
                    " (Cost: " + original_cost + " -> " + cost + " )");
            
            // Process the change in the dataset we are using by removin the 
            // point that becomes the new center
            dataset.remove(bestCenter);
            // add the old center as a new datapoint
            dataset.add(cluster.getCenter());
            // Process the cluster change
            cluster.swapCenter(bestCenter);
            // Mark this as a change to inform that a new PAM iteration is 
            // needed
            flagChange();
            // Pam optimization is run only once, as we do multiple iterations 
            // anyway
            return false;
        }
    }
}