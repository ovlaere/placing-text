package be.ugent.intec.ibcn.geo.clustering;

/**
 * This class extends the default clustering parameters with some parameters
 * that are typical for the PAM clustering algorithm.
 * 
 * @author Olivier Van Laere <oliviervanlaere@gmail.com>
 */
public class PamParameters extends ClusteringParameters {

    /**
     * Max number of iterations that can be performed before the clustering
     * algorithm automatically ends.
     */
    protected int iterationLimit = 100;
    
    /**
     * Set the iteration limit.
     * @param iterationLimit
     */
    public void setIterationLimit(int iterationLimit) {
        this.iterationLimit = iterationLimit;
    }

    /**
     * Store the minimum change threshold required during partial cluster
     * optimization.
     */
    protected double cost_improvement_threshold_partial = 0.001;

    /**
     * Set the minimum change threshold required during partial cluster
     * optimization.
     * @param c the minimum change threshold required during partial cluster
     * optimization.
     */
    public void setCost_improvement_threshold_partial(double c) {
        this.cost_improvement_threshold_partial = c;
    }

    /**
     * Configure the minimum required cluster size.
     */
    protected int min_cluster_size = 25;

    /**
     * Set the minimum required cluster size.
     * @param min_cluster_size the minimum required cluster size.
     */
    public void setMin_cluster_size(int min_cluster_size) {
        this.min_cluster_size = min_cluster_size;
    }

    /**
     * Configure if the clusters should be merged when their size drops below
     * the minimum cluster size.
     */
    protected boolean merge_cluster_below_threshold = false;

    /**
     * Set if the clusters should be merged when their size drops below
     * the minimum cluster size.
     * @param merge_cluster_below_threshold if the clusters should be merged 
     * when their size drops below the minimum cluster size.
     */
    public void setMerge_cluster_below_threshold(boolean merge_cluster_below_threshold) {
        this.merge_cluster_below_threshold = merge_cluster_below_threshold;
    }
        
    /**
     * Constructor.
     */
    public PamParameters() {
        super();
    }
}