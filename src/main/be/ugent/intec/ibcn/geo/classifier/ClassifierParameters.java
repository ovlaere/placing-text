package be.ugent.intec.ibcn.geo.classifier;

import be.ugent.intec.ibcn.geo.common.AbstractParameters;
import be.ugent.intec.ibcn.geo.common.io.FeaturesIO;
import java.util.Map;

/**
 * Extend the common parameters with parameters that are specific to the 
 * classification process.
 * 
 * @see AbstractParameters
 * 
 * @author Olivier Van Laere <oliviervanlaere@gmail.com>
 */
public class ClassifierParameters extends AbstractParameters {

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
     * Map holding the features in use for classification.
     */
    protected Map<Object, Integer> features;
    
    /**
     * Smoothing method that is currently applied.
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
     * @param jelinekLambda the lambda parameter for the Jelinek-Mercer 
     * smoothing technique.
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
     * @param dirichletMu the mu parameter for the Dirichlet smoothing 
     * technique.
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
    @Override
    public void init() {
        // Call the super method for init.
        super.init();
        if (trainingFile == null)
            throw new RuntimeException("Training file is not set.");
        
        if (trainingParser == null)
            throw new RuntimeException(
                    "Parser class for training file is not set.");
        
        if (featureFile == null)
            throw new RuntimeException("Feature file is not set.");
        
        if (classCount < 0)
            throw new RuntimeException("Number of classes is not set.");
        
        if (featureCount < 0)
            throw new RuntimeException("Number of features is not set.");
        // Check necessary smoothing parameters
        switch (smoothingMethod) {
            case -1:
                throw new RuntimeException("Smoothing method is not set.");
            case NaiveBayes.SMOOTHING_DIRICHLET:
                if (dirichletMu < 0)
                    throw new RuntimeException(
                            "Mu parameter for smoothing is not set.");
                break;
            case NaiveBayes.SMOOTHING_JELINEK:
                if (jelinekLambda < 0)
                    throw new RuntimeException(
                            "Lambda parameter for smoothing is not set.");
                break;    
        }
        // Check necessary prior parameters
        switch(prior_mode) {
            case -1:
                throw new RuntimeException("Prior mode is not set.");
            case NaiveBayes.PRIOR_HOME:
                if (home_weight < 0)
                    throw new RuntimeException(
                            "K-value for home prior is not set.");
                break;
        }
        // Load the features
        this.features = FeaturesIO.loadFeaturesFromFile(featureFile, featureCount);
    }
}