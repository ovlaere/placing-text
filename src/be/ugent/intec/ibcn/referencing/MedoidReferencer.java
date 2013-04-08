package be.ugent.intec.ibcn.referencing;

import be.ugent.intec.ibcn.geo.classifier.NaiveBayesResults;
import be.ugent.intec.ibcn.geo.common.datatypes.DataItem;
import be.ugent.intec.ibcn.geo.common.datatypes.Point;
import be.ugent.intec.ibcn.geo.common.io.DataLoading;
import be.ugent.intec.ibcn.geo.common.io.ReferencingIO;
import java.util.Map;
import java.util.TreeMap;

/**
 * TODO Add comment
 * @author Olivier Van Laere <oliviervanlaere@gmail.com>
 */
public class MedoidReferencer extends AbstractReferencer{

    /**
     * Constructor.
     * @param parameters Parameters for referencing.
     */
    public MedoidReferencer(ReferencingParameters parameters) {
        super(parameters);
    }

    @Override
    public void run(String outputFileName) {
        // Load the test data
        System.out.println("Loading test from " + parameters.getTestFile());
        DataLoading dl = new DataLoading();
        // Data is loaded WITHOUT feature selection
        DataItem [] test_data = dl.loadDataFromFile(parameters.getTestFile(), 
                parameters.getTestParser(), parameters.getTestLimit(), null);
        
        NaiveBayesResults classifier_output = 
                new NaiveBayesResults(parameters.getOutputFile());
    
        Map<Integer, Point> predictions = new TreeMap<Integer, Point>();
        
        for (int i = 0; i < test_data.length; i++) {
            DataItem item = test_data[i];
            // Sanity check
            if (item != null) {
                // Determine class Id
                int classId = classifier_output.getPrediction(item.getId());
                // This info is ignored in this implementation
                int numberOfTagsUsed = classifier_output.getFeatureCount(item.getId());
                Point prediction = predictions.get(item.getId());
                // If there is no prediction yet - which is standard for this
                // implementation
                if (prediction == null) {
                    predictions.put(item.getId(), 
                            parameters.getClassMapper().getMedoids().get(classId));
                }
            }
            else
                throw new RuntimeException("This should not happen? Test item id " + i);
        }
        // Write to file
        ReferencingIO.writeLocationsToFile(predictions, outputFileName);
    }
}