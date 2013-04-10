package be.ugent.intec.ibcn.analyzer;

import be.ugent.intec.ibcn.geo.common.AbstractParameters;
import be.ugent.intec.ibcn.geo.common.ClassMapper;
import be.ugent.intec.ibcn.geo.common.datatypes.Point;
import be.ugent.intec.ibcn.geo.common.io.FileIO;
import be.ugent.intec.ibcn.referencing.ReferencingParameters;
import java.util.List;

/**
 * TODO Comment
 * @author Olivier Van Laere <oliviervanlaere@gmail.com>
 */
public class AnalyzerParameters extends ReferencingParameters{
    
    public AnalyzerParameters() {
        super();
    }
    
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
            throw new RuntimeException("Test file is not set for classification.");
        
        if (testParser == null)
            throw new RuntimeException("Parser class for test file is not set for classification.");

        if (medoidFile != null && medoidParser != null) {
            // Load the medoids
            List<Point> medoids = FileIO.loadMedoids(medoidFile, medoidParser);
            this.classmapper = new ClassMapper(medoids);
        }
    }
}
