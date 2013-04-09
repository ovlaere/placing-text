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
        this.medoidFile = parameters.getMedoidFile();
        this.medoidParser = parameters.getMedoidParser();
        this.testFile = parameters.getTestFile();
        this.testParser = parameters.getTestParser();
        this.classificationFile = parameters.getClassificationFile();
    }    
}
