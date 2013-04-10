package be.ugent.intec.ibcn.geo.common;

import be.ugent.intec.ibcn.geo.common.datatypes.Point;
import be.ugent.intec.ibcn.geo.common.io.FileIO;
import java.util.List;

/**
 * This base class contains the basic parameters for most of the components of
 * the framework. Components can extend this class to add parameters and methods
 * while still sharing the most basic parameters.
 * 
 * @author Olivier Van Laere <oliviervanlaere@gmail.com>
 */
public abstract class AbstractParameters {
    
    /**
     * The filename of the training file for classification.
     */
    protected String trainingFile;

    /**
     * Set the filename of the training file for classification.
     * @param trainingFile 
     */
    public void setTrainingFile(String trainingFile) {
        this.trainingFile = trainingFile;
    }
    
    /**
     * @return the name of the trainingFile.
     */
    public String getTrainingFile() {
        return this.trainingFile;
    }
    
    /**
     * The package and classname of the parser to use for the training file.
     */
    protected String trainingParser;

    /**
     * Set the package and classname of the parser to use for the training file.
     * @param trainingParser 
     */
    public void setTrainingParser(String trainingParser) {
        this.trainingParser = trainingParser;
    }
    
    /**
     * @return the package and classname of the parser to use for the training file.
     */
    public String getTrainingParser() {
        return this.trainingParser;
    }
    
    /**
     * The filename of the file containing the medoids for classification.
     */
    protected String medoidFile;
    
    /**
     * Set the filename of the file containing the medoids for classification.
     * @param medoidFile 
     */
    public void setMedoidFile(String medoidFile) {
        this.medoidFile = medoidFile;
    }
    
    /**
     * @return the filename of the file containing the medoids for classification.
     */
    public String getMedoidFile() {
        return this.medoidFile;
    }

    /**
     * The package and classname of the parser to use for the medoid file.
     */
    protected String medoidParser;

    /**
     * Set the package and classname of the parser to use for the medoid file.
     * @param medoidParser 
     */
    public void setMedoidParser(String medoidParser) {
        this.medoidParser = medoidParser;
    }
    
    /**
     * @return the package and classname of the parser to use for the medoid file.
     */
    public String getMedoidParser() {
        return this.medoidParser;
    }

    /**
     * The filename of the file containing the test items for classification.
     */
    protected String testFile;
    
    /**
     * Set the filename of the file containing the test items for classification.
     * @param testFile 
     */
    public void setTestFile(String testFile) {
        this.testFile = testFile;
    }
    
    /**
     * @return the filename of the file containing the test items for classification.
     */
    public String getTestFile() {
        return this.testFile;
    }
    
    /**
     * The package and classname of the parser to use for the test file.
     */
    protected String testParser;
    
    /**
     * Set the package and classname of the parser to use for the test file.
     * @param testParser 
     */
    public void setTestParser(String testParser) {
        this.testParser = testParser;
    }
    
    /**
     * @return the package and classname of the parser to use for the test file.
     */
    public String getTestParser() {
        return this.testParser;
    }
    
    /**
     * The filename of the file that will contain, in order, the class 
     * predictions for the test items.
     */
    protected String classificationFile;
    
    /**
     * Set the filename of the file that will contain, in order, the class 
     * predictions for the test items.
     */
    public void setClassificationFile(String classificationFile) {
        this.classificationFile = classificationFile;
    }
    
    /**
     * @return the filename of the file that will contain, in order, the class 
     * predictions for the test items.
     */
    public String getClassificationFile() {
        return this.classificationFile;
    }
    
    /**
     * Classmapper object for on the fly association of training items
     * with the medoids.
     */
    protected ClassMapper classmapper;
    
    /**
     * @return the Classmapper object for on the fly association of training 
     * items with the medoids.
     */
    public ClassMapper getClassMapper() {
        return this.classmapper;
    }
    
    /**
     * Optionally, set a limit on the amount of training data to use. -1 means
     * all training data, otherwise the first 'training_limit' lines.
     */
    protected int training_limit = -1;

    /**
     * Set a limit on the amount of training data to use. -1 means
     * all training data, otherwise the first 'training_limit' lines.
     */
    public void setTraining_limit(int training_limit) {
        this.training_limit = training_limit;
    }
    
    /**
     * @return the value of the optional training limit.
     */
    public int getTrainingLimit() {
        return this.training_limit;
    }
    
    /**
     * Optionally, set a limit on the amount of test data to use. -1 means
     * all test data, otherwise the first 'test_limit' lines.
     */
    protected int test_limit = -1;
    
    /**
     * Set a limit on the amount of test data to use. -1 means
     * all test data, otherwise the first 'test_limit' lines.
     * @param test_limit 
     */
    public void setTest_limit(int test_limit) {
        this.test_limit = test_limit;
    }
    
    /**
     * @return the value of the optional test limit.
     */
    public int getTestLimit() {
        return this.test_limit;
    }
    
    /**
     * Initialize the parameters.
     * This is necessary before most of the components can be started!
     */
    public void init() {
        // Check necessary parameters        
        if (medoidFile == null)
            throw new RuntimeException("Medoid file is not set.");
        
        if (medoidParser == null)
            throw new RuntimeException("Parser class for medoid file is not set.");
        
        if (testFile == null)
            throw new RuntimeException("Test file is not set.");
        
        if (testParser == null)
            throw new RuntimeException("Parser class for test file is not set.");
        
        if (classificationFile == null)
            throw new RuntimeException("Output file is not set.");
        
        // Load the medoids
        List<Point> medoids = FileIO.loadMedoids(medoidFile, medoidParser);
        this.classmapper = new ClassMapper(medoids);
    }
}