package be.ugent.intec.ibcn.referencing;

import be.ugent.intec.ibcn.geo.common.AbstractParameters;

/**
 * Extension of the base parameters for the referencing component.
 * 
 * At the moment, there are no additional parameters. This class is a 
 * placeholder and implements a copy constructor given existing parameters.
 * 
 * @see AbstractParameters
 * 
 * @author Olivier Van Laere <oliviervanlaere@gmail.com>
 */
public class ReferencingParameters extends AbstractParameters{
   
    /**
     * Default constructor.
     */
    public ReferencingParameters() {
        super();
    }
    
    /**
     * Copy constructor.
     * @param parameters 
     */
    public ReferencingParameters(AbstractParameters parameters) {
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
}