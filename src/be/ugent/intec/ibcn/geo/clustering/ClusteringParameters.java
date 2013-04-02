package be.ugent.intec.ibcn.geo.clustering;

/**
 * This class contains the parameters to be used in the clustering process.
 * The easiest way to use the default values in combination with custom 
 * parameter values is to extend this class.
 * 
 * @author Olivier Van Laere <oliviervanlaere@gmail.com>
 */
public class ClusteringParameters {
  
    /**
     * Optimization process: Iteration limit.
     */
    protected int optimization_iteration_limit = 100;

    /**
     * Set the iteration limit for the cluster optimization process.
     * @param optimization_iteration_limit 
     */
    public void setOptimization_iteration_limit(int optimization_iteration_limit) {
        this.optimization_iteration_limit = optimization_iteration_limit;
    }
    
    /**
     * @return the optimization iteration limit.
     */
    public int getOptimization_iteration_limit() {
        return optimization_iteration_limit;
    }

    /**
     * Optimization process: Overall sample count.
     */
    protected int optimization_overall_sample_limit = 512;
    
    /**
     * Set the overall sample count for the cluster optimization process.
     * @param optimization_overall_sample_limit 
     */
    public void setOptimization_overall_sample_limit(int optimization_overall_sample_limit) {
        this.optimization_overall_sample_limit = optimization_overall_sample_limit;
    }
    
    /**
     * @return the optimization overall sample limit.
     */
    public int getOptimization_overall_sample_limit() {
        return optimization_overall_sample_limit;
    }
    
    /**
     * Optimization process: Define the minimum improvement in cluster cost 
     * to consider it an actual improvement. 
     * This value is by default set to 0.05 (5%).
     */
    protected double optimization_min_improvement = 0.05;
    
    /**
     * Set the optimization minimal improvement.
     * @param optimization_min_improvement 
     */
    public void setOptimization_min_improvement(double optimization_min_improvement) {
        this.optimization_min_improvement = optimization_min_improvement;
    }

    
    /**
     * @return the optimization minimal improvement.
     */
    public double getOptimization_min_improvement() {
        return optimization_min_improvement;
    }
    
    /**
     * Holds the class of the LineParser implementation to use for parsing
     * the training lines.
     */
    protected String lineParserClassNameForInput = 
            "be.ugent.intec.ibcn.geo.clustering.LineParserClusterInputDefault";
    
    /**
     * Set the the classname of the line parser for the clustering input.
     * @param lineParserClassNameForInput 
     */
    public void setLineParserClassNameForInput(String lineParserClassNameForInput) {
        this.lineParserClassNameForInput = lineParserClassNameForInput;
    }
    
    /**
     * @return the classname of the line parser for the clustering input.
     */
    public String getLineParserClassNameForInput() {
        return lineParserClassNameForInput;
    }
    
    /**
     * If set to true, all clustering info will be writte to file. This is handy
     * for plotting the clustering and debugging, but should be set to false
     * for a usable file format for the rest of the framework.
     */
    protected boolean writeFullClusteringToFile = false;

    /**
     * @return false if only the medoids are written to file.
     */
    public boolean isWriteFullClusteringToFile() {
        return writeFullClusteringToFile;
    }

    /**
     * Set whether only the medoids are written to file, or all the clustering info.
     * @param writeFullClusteringToFile 
     */
    public void setWriteFullClusteringToFile(boolean writeFullClusteringToFile) {
        this.writeFullClusteringToFile = writeFullClusteringToFile;
    }
}