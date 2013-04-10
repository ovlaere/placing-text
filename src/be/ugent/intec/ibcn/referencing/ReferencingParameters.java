package be.ugent.intec.ibcn.referencing;

import be.ugent.intec.ibcn.geo.common.AbstractParameters;

/**
 * TODO Add comment
 * @author Olivier Van Laere <oliviervanlaere@gmail.com>
 */
public class ReferencingParameters extends AbstractParameters{
    
    public ReferencingParameters() {
        super();
    }
    
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
