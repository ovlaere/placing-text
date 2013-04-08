package be.ugent.intec.ibcn.geo.classifier;

import be.ugent.intec.ibcn.geo.common.ClassMapper;
import be.ugent.intec.ibcn.geo.common.datatypes.Point;
import be.ugent.intec.ibcn.geo.common.io.FeaturesIO;
import be.ugent.intec.ibcn.geo.common.io.FileIO;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Olivier Van Laere <oliviervanlaere@gmail.com>
 */
public class ClassifierParameters {

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
     * The filename of the file containing the features for classification.
     */
    protected String featureFile;
    
    /**
     * Set the filename of the file containing the features for classification.
     * @param featureFile 
     */
    public void setFeatureFile(String featureFile) {
        this.featureFile = featureFile;
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
     * The filename of the file that will contain, in order, the class 
     * predictions for the test items.
     */
    protected String nbPredictionsFile;
    
    /**
     * Set the filename of the file that will contain, in order, the class 
     * predictions for the test items.
     */
    public void setOutputFile(String nbPredictionsFile) {
        this.nbPredictionsFile = nbPredictionsFile;
    }
    
    /**
     * The number of classes to use for classification.
     */
    protected int classCount = -1;
    
    /**
     * Set the number of classes to use for classification.
     * @param classCount 
     */
    public void setClassCount(int classCount) {
        this.classCount = classCount;
    }
    
    /**
     * The number of features to use for classification.
     * This depends on the amount of memory. If you get OutOfMemory exceptions,
     * lower this number.
     */
    protected int featureCount = -1;
    
    /**
     * Set the number of features to use for classification.
     * @param featureCount 
     */
    public void setFeatureCount(int featureCount) {
        this.featureCount = featureCount;
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
     * Classmapper object for on the fly association of training items
     * with the medoids.
     */
    protected ClassMapper classmapper;
    
    /**
     * Map holding the features in use for classification.
     */
    protected Map<Object, Integer> features;
    
    /**
     * Smoothing method that is currently applied.
     * @see http://www.sciencedirect.com/science/article/pii/S002002551300162X#s0125
     */
    protected int smoothingMethod = -1;

    /**
     * Set the smoothing method to be used.
     * @param smoothingMethod a constant from NaiveBayes
     * @see NaiveBayes
     */
    public void setSmoothingMethod(int smoothingMethod) {
        this.smoothingMethod = smoothingMethod;
    }

    /**
     * lambda parameter for the Jelinek-Mercer smoothing technique.
     */
    protected double jelinekLambda = -1;

    /**
     * Set the lambda parameter for the Jelinek-Mercer smoothing technique.
     * @param jelinekLambda the lambda parameter for the Jelinek-Mercer smoothing technique.
     */
    public void setJelinekLambda(double jelinekLambda) {
        this.jelinekLambda = jelinekLambda;
    }

    /**
     * mu parameter for the Dirichlet smoothing technique.
     */
    protected double dirichletMu = -1;

    /**
     * Set the mu parameter for the Dirichlet smoothing technique.
     * @param dirichletMu the mu parameter for the Dirichlet smoothing technique.
     */
    public void setDirichletMu(double dirichletMu) {
        this.dirichletMu = dirichletMu;
        System.out.println("dirichletMu = " + dirichletMu);
    }

    /**
     * Tracks the prior mode currently in use for classification.
     */
    protected int prior_mode = -1;
    
    /**
     * Set the prior mode to use for classification.
     * @param prior_mode a constant from NaiveBayes
     * @see NaiveBayes
     */
    public void setPriorMode(int prior_mode) {
        this.prior_mode = prior_mode;
    }
    
    /**
     * Optionally, set the weight value to use for the home prior.
     * @see http://www.sciencedirect.com/science/article/pii/S002002551300162X#s0110
     */
    protected double home_weight = -1;
    
    /**
     * Set the weight value to use for the home prior.
     * @param home_weight a double value larger than 0.
     */
    public void setHomeWeight(double home_weight) {
        this.home_weight = home_weight;
    }
    
    /**
     * Initialize the parameters.
     * This is necessary before classification can be started!
     */
    public void init() {
        // Check necessary parameters
        if (trainingFile == null)
            throw new RuntimeException("Training file is not set for classification.");
        
        if (trainingParser == null)
            throw new RuntimeException("Parser class for training file is not set for classification.");
        
        if (medoidFile == null)
            throw new RuntimeException("Medoid file is not set for classification.");
        
        if (medoidParser == null)
            throw new RuntimeException("Parser class for medoid file is not set for classification.");
        
        if (featureFile == null)
            throw new RuntimeException("Feature file is not set for classification.");
        
        if (testFile == null)
            throw new RuntimeException("Test file is not set for classification.");
        
        if (testParser == null)
            throw new RuntimeException("Parser class for test file is not set for classification.");
        
        if (nbPredictionsFile == null)
            throw new RuntimeException("Output file is not set for classification.");
        
        if (classCount < 0)
            throw new RuntimeException("Number of classes is not set for classification.");
        
        if (featureCount < 0)
            throw new RuntimeException("Number of features is not set for classification.");
        // Check necessary smoothing parameters
        switch (smoothingMethod) {
            case -1:
                throw new RuntimeException("Smoothing method is not set.");
            case NaiveBayes.SMOOTHING_DIRICHLET:
                if (dirichletMu < 0)
                    throw new RuntimeException("Mu parameter for smoothing is not set.");
                break;
            case NaiveBayes.SMOOTHING_JELINEK:
                if (jelinekLambda < 0)
                    throw new RuntimeException("Lambda parameter for smoothing is not set.");
                break;    
        }
        // Check necessary prior parameters
        switch(prior_mode) {
            case -1:
                throw new RuntimeException("Prior mode is not set.");
            case NaiveBayes.PRIOR_HOME:
                if (home_weight < 0)
                    throw new RuntimeException("K-value for home prior is not set.");
                break;
        }
        
        // Load the medoids
        List<Point> medoids = FileIO.loadMedoids(medoidFile, medoidParser);
        this.classmapper = new ClassMapper(medoids);
        
        // Load the features
        this.features = FeaturesIO.loadFeaturesFromFile(featureFile, featureCount);
    }
}