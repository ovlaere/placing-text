package be.ugent.intec.ibcn.geo.clustering;

import be.ugent.intec.ibcn.geo.clustering.datatypes.Cluster;
import be.ugent.intec.ibcn.geo.common.datatypes.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * This class provides the generic (and common) functionality of the clustering
 * algorithms in this framework.
 * 
 * In the current version of the clustering package, the input data is read
 * into an array. Although this has been tested to input sizes of millions of
 * training items, this introduced a bottleneck to the size of training files
 * that can be handled.
 * 
 * Depending on the size of the input data you want to cluster, it is mandatory
 * to increase the -Xmx VM parameter to avoid OutOfMemoryExceptions.
 * 
 * Note: the current clustering format is just a list of the medoids, i.e. the 
 * id's, latitude and longitude of the training items that are the cluster
 * centra. In the rest of the framework, an on the fly association is made using
 * nearest-neighbour (using a KD-tree for performance). This means that in the
 * current version, no clustering is possible where data points would not be
 * assigned to a medoid that is NOT its nearest medoid.
 * 
 * @author Olivier Van Laere <oliviervanlaere@gmail.com>
 */
public abstract class AbstractClustering {

    /**
     * Constant storing the amount of available processors on the system.
     */
    protected static final int NR_THREADS = 
            Runtime.getRuntime().availableProcessors();
    {
        System.out.println("[ Available CPU's: " + NR_THREADS + " ]");
    }
    
    /**
     * Holds a reference to the input data.
     */
    protected Point [] data;
    
    /**
     * Reference to the ClusteringParameters, used for intra-cluster 
     * optimization.
     */
    protected ClusteringParameters parameters;
    
    /**
     * Constructor.
     * @param parameters parameters used for optimization
     */
    public AbstractClustering(ClusteringParameters parameters) {
        this.parameters = parameters;
    }
    
    /**
     * Abstract method declaration for the actual clustering call.
     * @param outputfile Filename of the file that will contain the clustering
     */
    public abstract void cluster(String outputfile);
    
    /**
     * Helper class that allows multi-threaded optimization of individual 
     * Clusters. This class will search for a cluster center that minimizes the 
     * overall cluster cost.
     * An iteration limit of 100 iteration is used by default, as well as a 
     * sampling in case that more than 512 elements are in the collection.
     * 
     * @see ClusteringParameters
     */
    protected class ClusterOptimizer implements Runnable{

        /**
         * Default max number of points to process by each of the threads.
         */
        private int sample_limit = 
                parameters.optimization_overall_sample_limit / NR_THREADS;

        /**
         * Keeps track of the Cluster this Thread is optimizing.
         */
        protected Cluster cluster;

        /**
         * @param cluster the Cluster this Thread is optimizing.
         */
        public ClusterOptimizer(Cluster cluster) {
            this.cluster = cluster;
        }

        /**
         * run() implementation.
         * This method will optimize a specified cluster in a multi-threaded 
         * way: it will split the elements of the Cluster into different 
         * sublists that will be processed by multiple threads. The results of 
         * all threads are then matched against each other, and the overall best
         * configuration is stored.
         */
        @Override
        public void run() {
            boolean runMore;
            // Keep track of the iterations
            int iterations = 0;
            do {
                // Increase the iteration count
                iterations++;
                // Assume this will be the last iteration - so far
                runMore = false;
                // Initialize placeholders for the best configuration
                Point bestCenter = cluster.getCenter();
                double cost = cluster.calculateCost();
                double original_cost = cost;

                // Make a copy of the datapoints because we are iterating 
                // over the list
                List<Point> datapoints = 
                        new ArrayList<Point>(cluster.getElements());
                // Create helpers according to the number of threads we have
                ClusterOptimizerHelper[] threads = 
                        new ClusterOptimizerHelper[NR_THREADS];
                // Determine the size of each of the sub-problems
                int unit = (int) (datapoints.size() * 1.0 / NR_THREADS);
                // For each of the threads
                for (int i = 0; i < threads.length; i++) {
                    // Determine the workload
                    int end = (i + 1) * unit;
                    // Determine the workload for the remainder
                    if (i == threads.length - 1)
                        end = datapoints.size();
                    // Create a subset of potential best point candidates
                    List<Point> subset = 
                            new ArrayList(datapoints.subList(i * unit, end));
                    // Init the helper thread with the subset of candidates, 
                    // a copy of the cluster data and a sample limit 
                    // (= min(elements in the subset, sample_limit)
                    threads[i] = new ClusterOptimizerHelper(subset, 
                            new Cluster(cluster), 
                            (int)Math.min(subset.size(), sample_limit));
                    // Start optimizing the cluster
                    threads[i].start();
                }
                // Join the results for each thread
                for (int i = 0; i < threads.length; i++) {
                    try {
                        threads[i].join();
                    } catch (InterruptedException ignore) {}
                }
                // Gather the best result for each of the threads
                for (int i = 0; i < threads.length; i++) {
                    Point threadBest = threads[i].getBestCenter();
                    double threadCost = threads[i].getBestCost();
                    // If the thread cost is lower than the current best
                    if (threadCost < cost && 
                            // And the improvement is at least 
                            // optimization_min_improvement
                            (cost - threadCost)/threadCost >= 
                            parameters.optimization_min_improvement) {
                        // keep track of this new best configuration
                        bestCenter = threadBest;
                        cost = threadCost;
                    }
                }
                // In case the new best center differs from the current best
                if (bestCenter != cluster.getCenter()) {
                    runMore = finalSwap(bestCenter, cluster, 
                            original_cost, cost);
                }
                // clear the possibility to undo the swap...
                cluster.clearUndoSwap();
            }
            // In case there is no convergence and we have iterations left to 
            // run
            while (runMore && 
                    iterations < parameters.optimization_iteration_limit);
        }

        /**
         * Helper method that processes the case where a cluster medoid should 
         * be replace with a better one. The default behaviour for optimization 
         * is specified in this implementation, which can be overridden by other
         * implementations.
         * @param bestCenter Best center so far
         * @param cluster Cluster object
         * @param original_cost 
         * @param cost
         * @return always true if a swap was made. The default optimization runs
         * until the iteration limit is hit.
         */
        protected boolean finalSwap(Point bestCenter, Cluster cluster, 
            double original_cost, double cost) {
            // - Process the cluster change
            cluster.swapCenter(bestCenter);
            // Mark this as a change to inform that a new iteration is needed
            return true;
        }
        
        /**
         * Helper class that allows multi-threaded optimization of separate 
         * Clusters.
         * This class will search for a cluster center that minimizes the 
         * overall cluster cost.
         */
        protected class ClusterOptimizerHelper extends Thread {

            /**
             * A list of Points that are possible center candidates.
             */
            private List<Point> datapoints;

            /**
             * A copy of the original Cluster to be optimized.
             */
            private Cluster cluster;

            /**
             * Keeps track of the best Cluster cost.
             */
            private double cost;

            /**
             * @return the best Cluster cost found so far.
             */
            public double getBestCost() {
                return this.cost;
            }

            /**
             * Keeps track of the Point that leads to the minimal Cluster cost.
             */
            private Point bestCenter = null;

            /**
             * @return the Point that leads to the minimal Cluster cost.
             */
            public Point getBestCenter() {
                return this.bestCenter;
            }

            /**
             * Keeps track of the sample size to use.
             */
            private int sample_count;

            /**
             * Constructor.
             * @param datapoints A list of Points that are possible center 
             * candidates
             * @param cluster A copy of the original Cluster to be optimized.
             * @param sample_count Sample size to use.
             */
            public ClusterOptimizerHelper(List<Point> datapoints, 
                    Cluster cluster, int sample_count) {
                this.datapoints = datapoints;
                this.cluster = cluster;
                this.sample_count = sample_count;
                // Init the cluster cost based on the current center.
                this.cost = cluster.getCost();
            }

            /**
             * run() implementation.
             * Will try every possible center Point and store the optimal 
             * configuration. In case sample_count < datapoints.size(), 
             * sampling will be applied.
             */
            @Override
            public void run() {
                // Init and seed a random generator
                Random rg = new Random(987654321L);
                // Copy the datapoints
                List<Point> copy = new ArrayList(datapoints);
                // List of selected points
                List<Point> samples = new ArrayList<Point>();
                // While we do not have enough sample_count
                while (samples.size() < sample_count) {
                    // Pick a random point
                    int index = rg.nextInt(copy.size());
                    // Add it to the selected list
                    samples.add(copy.get(index));
                    // Remove from the datapoints
                    copy.remove(index);
                }
                // For each point in the samples
                for (Point p : samples) {
                    // try making p the new center - cost is recalculated 
                    // automatically
                    cluster.swapCenter(p);
                    // check if it has a lower configuration cost
                    // AND that the configuration change is larger than 
                    // optimization_min_improvement
                    if (cluster.getCost() < cost && cost - cluster.getCost() > 
                            parameters.optimization_min_improvement) {
                        // keep track of this configuration
                        bestCenter = p;
                        cost = cluster.getCost();
                    }
                    // undo the new configuration
                    cluster.undoCenterSwap();
                    // and try the next one
                }
            }
        }
    }
}