package be.ugent.intec.ibcn.analyzer;

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
    
    /**
     * Initialize the parameters.
     * Override to remove exception on optional parameters.
     */
    @Override
    public void init() {
        // Check necessary parameters        
        if (medoidFile == null)
            throw new RuntimeException("Medoid file is not set for classification.");
        
        if (medoidParser == null)
            throw new RuntimeException("Parser class for medoid file is not set for classification.");
        
        if (testFile == null)
            throw new RuntimeException("Test file is not set for classification.");
        
        if (testParser == null)
            throw new RuntimeException("Parser class for test file is not set for classification.");
        
        // Load the medoids
        List<Point> medoids = FileIO.loadMedoids(medoidFile, medoidParser);
        this.classmapper = new ClassMapper(medoids);
    }
}
