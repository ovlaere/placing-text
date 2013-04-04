package be.ugent.intec.ibcn.geo.features;

import be.ugent.intec.ibcn.geo.common.datatypes.DataItem;
import be.ugent.intec.ibcn.geo.common.datatypes.Point;
import be.ugent.intec.ibcn.geo.common.io.DataLoading;
import be.ugent.intec.ibcn.geo.common.io.FileIO;
import be.ugent.intec.ibcn.geo.common.ClassMapper;
import java.util.List;
import java.util.Random;

/**
 * This class provides the common functionality of some statistical and class
 * based feature rankers.
 * 
 * @see ChiSquareFeatureRanker
 * @see MaxChiSquareFeatureRanker
 * @see LogLikelihoodFeatureRanker
 *
 * @author Olivier Van Laere <oliviervanlaere@gmail.com>
 */
public abstract class AbstractClassLevelRanker {

    /**
     * Number of threads, for multi-threaded processing.
     */
    protected static final int NR_THREADS = Runtime.getRuntime().availableProcessors();
    
    /**
     * Random generator used in shuffling data.
     */
    protected Random rg = new Random(123987456L);

    /**
     * Store the total number of photos in the training data.
     */
    protected int total_photos = 0;

    /**
     * Object containing the overall tag counts.
     */
    protected OverallTagCounter otc;

    /**
     * ClassMapper used for on the fly clustering association.
     */
    protected ClassMapper classmapper;
    
    /**
     * The training data.
     */
    protected DataItem [] data;
    
    /**
     * Constructor.
     * @param inputfile Input file with the training data
     * @param inputParser Parser implementation to use for the input data
     * @param medoidfile Input file with the medoids (cluster centra)
     * @param medoidParser Parser implementation to use for the medoid data 
     */
    public AbstractClassLevelRanker(String inputfile, String inputParser, 
            String medoidfile, String medoidParser) {
        // Load the training data 
        DataLoading dl = new DataLoading();
        this.data = dl.loadDataFromFile(inputfile, inputParser, -1, null);
        for (DataItem item : data)
            if (item != null)
                total_photos++;
        
        // Load the medoids
        List<Point> medoid_points = FileIO.loadMedoids(medoidfile, medoidParser);
        // Init the classmapper
        this.classmapper = new ClassMapper(medoid_points);
        // Attach the actual data - i.e. fully loaded clustering info now
        this.classmapper.attachElements(data);
        // Now we can init an overall tag counter
        this.otc = new OverallTagCounter(classmapper, data);
    }
}