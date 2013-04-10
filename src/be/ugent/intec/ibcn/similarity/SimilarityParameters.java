package be.ugent.intec.ibcn.similarity;

import be.ugent.intec.ibcn.geo.common.AbstractParameters;
import be.ugent.intec.ibcn.geo.common.ClassMapper;
import be.ugent.intec.ibcn.geo.common.datatypes.Point;
import be.ugent.intec.ibcn.geo.common.io.FileIO;
import be.ugent.intec.ibcn.referencing.ReferencingParameters;
import java.io.File;
import java.util.List;

/**
 * Extend the common parameters with parameters that are specific to the 
 * similarity process.
 * 
 * @author Olivier Van Laere <oliviervanlaere@gmail.com>
 */
public class SimilarityParameters extends ReferencingParameters {

    /**
     * Field holding a full path and name template for the directories used
     * to create the similarity index.
     */
    protected String similarityDir;

    /**
     * @return a full path and name template for the directories used
     * to create the similarity index.
     */
    public String getSimilarityDirectory() {
        return similarityDir;
    }

    /**
     * Set a full path and name template for the directories used
     * to create the similarity index.
     * @param similarityDir a full path and name template 
     * for the directories used to create the similarity index.
     */
    public void setSimilarityDirectory(String similarityDir) {
        this.similarityDir = similarityDir;
    }
    
    /**
     * Keeps track of the number of similar items to consider for the
     * similarity search.
     */
    protected int similarItemsToConsider = -1;

    /**
     * @return the number of similar items to consider for the
     * similarity search.
     */
    public int getSimilarItemsToConsider() {
        return this.similarItemsToConsider;
    }
    
    /**
     * Set the number of similar items to consider for the
     * similarity search.
     * @param similarItemsToConsider 
     */
    public void setSimilarItemsToConsider(int similarItemsToConsider) {
        this.similarItemsToConsider = similarItemsToConsider;
    }
    
    /**
     * Default constructor.
     */
    public SimilarityParameters() {
        super();
    }
    
    /**
     * Copy constructor.
     * @param parameters Parameters to use to copy from. 
     */
    public SimilarityParameters(AbstractParameters parameters) {
        super();
        this.trainingFile = parameters.getTrainingFile();
        this.trainingParser = parameters.getTrainingParser();
        this.medoidFile = parameters.getMedoidFile();
        this.medoidParser = parameters.getMedoidParser();
        this.testFile = parameters.getTestFile();
        this.testParser = parameters.getTestParser();
        this.classificationFile = parameters.getClassificationFile();
        this.training_limit = parameters.getTrainingLimit();
        this.test_limit = parameters.getTestLimit();
    }
    
    /**
     * Initialize the parameters.
     * This is necessary before classification can be started!
     */
    @Override
    public void init() {
        // Check necessary parameters        
        if (trainingFile == null)
            throw new RuntimeException("Training file is not set.");
        
        if (trainingParser == null)
            throw new RuntimeException("Parser class for training file is not set.");
        
        if (medoidFile == null)
            throw new RuntimeException("Medoid file is not set.");
        
        if (medoidParser == null)
            throw new RuntimeException("Parser class for medoid file is not set.");
        
        if (testFile == null)
            throw new RuntimeException("Test file is not set.");
        
        if (testParser == null)
            throw new RuntimeException("Parser class for test file is not set.");
        
        if (classificationFile == null)
            throw new RuntimeException("Classification output file is not set.");
        
        if (similarityDir == null)
            throw new RuntimeException("Directory template to use for index creation is not set.");
        
        if (similarItemsToConsider < 0)
            throw new RuntimeException("The number of similar items to consider not set.");
        
        // Check if the similarity cache dir exists
        File dir = new File(similarityDir.substring(0, 
                similarityDir.lastIndexOf("/")));
        if (!dir.exists())
            // If not, create the directories
            dir.mkdirs();
        
        // Load the medoids
        List<Point> medoids = FileIO.loadMedoids(medoidFile, medoidParser);
        this.classmapper = new ClassMapper(medoids);
        
        // Features are null, so all data is loaded - if the LineParser implements this
    }
}