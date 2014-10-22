package be.ugent.intec.ibcn.referencing;

/**
 * Base class for georeferencing. This class defines the common variables and
 * constants for this process.
 * 
 * For details on converting classification results into actual locations
 * @see http://www.sciencedirect.com/science/article/pii/S002002551300162X#s0130
 * 
 * @author Olivier Van Laere <oliviervanlaere@gmail.com>
 */
public abstract class AbstractReferencer {
    
    /**
     * Constant containing the number of processors available in the system.
     */
    protected static final int NR_THREADS = 
            Runtime.getRuntime().availableProcessors();
    
    /**
     * Constant defining medoid based location estimation
     */
    public static final int REFERENCE_METHOD_MEDOID = 0;
    
    /**
     * Constant defining similarity based location estimation
     */
    public static final int REFERENCE_METHOD_SIMILARITY = 1;

    /**
     * Parameters for referencing.
     */
    protected ReferencingParameters parameters;
    
    /**
     * Constructor.
     * @param parameters parameters for referencing.
     */
    public AbstractReferencer(ReferencingParameters parameters) {
        this.parameters = parameters;
    }
    
    /**
     * Actual location referencing. This method assumes the necessary setup 
     * has been carried out before this method is called.
     * @param outputFileName The name of the file containing the test ids
     * and the lat/lon location estimates.
     */
    public abstract void run(String outputFileName);
}