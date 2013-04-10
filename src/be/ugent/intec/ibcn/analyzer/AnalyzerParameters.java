package be.ugent.intec.ibcn.analyzer;

import be.ugent.intec.ibcn.geo.common.AbstractParameters;
import be.ugent.intec.ibcn.geo.common.ClassMapper;
import be.ugent.intec.ibcn.geo.common.datatypes.Point;
import be.ugent.intec.ibcn.geo.common.io.FileIO;
import be.ugent.intec.ibcn.referencing.ReferencingParameters;
import java.util.List;

/**
 * Extension of the referencing parameters for the analyzer component. These are
 * more or less similar to the referencing component, and therefore an extension
 * of ReferencingParameters rather than the base AbstractParameters.
 * 
 * There are no additional fields necessary at the moment, but the init method
 * is overriden to adapt the failure behaviour on missing parameters, as not all
 * the parameters for referencing are necessary for analyzing.
 * 
 * @author Olivier Van Laere <oliviervanlaere@gmail.com>
 */
public class AnalyzerParameters extends ReferencingParameters{
    
    /**
     * Default constructor.
     */
    public AnalyzerParameters() {
        super();
    }
    
    /**
     * Copy constructor.
     * @param parameters 
     */
    public AnalyzerParameters(AbstractParameters parameters) {
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
     * Override to remove exception on optional parameters.
     */
    @Override
    public void init() {
        // Check necessary parameters        
        if (testFile == null)
            throw new RuntimeException("Test file is not set.");
        
        if (testParser == null)
            throw new RuntimeException("Parser class for test file is not set.");

        // Only load if the optional fields are set
        if (medoidFile != null && medoidParser != null) {
            // Load the medoids
            List<Point> medoids = FileIO.loadMedoids(medoidFile, medoidParser);
            this.classmapper = new ClassMapper(medoids);
        }
    }
}
