package be.ugent.intec.ibcn.referencing;

import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.ugent.intec.ibcn.geo.classifier.NaiveBayesResults;
import be.ugent.intec.ibcn.geo.common.datatypes.DataItem;
import be.ugent.intec.ibcn.geo.common.datatypes.Point;
import be.ugent.intec.ibcn.geo.common.io.DataLoading;
import be.ugent.intec.ibcn.geo.common.io.ReferencingIO;

/**
 * Actual implementation of a Medoid based georeferencer.
 * 
 * This simple georeferencer takes the output from the classification process
 * and looks after the class to which each test item is assigned. It will then
 * assign the location of the medoid of that class to the given test item.
 * 
 * For details on the process
 * @see http://www.sciencedirect.com/science/article/pii/S002002551300162X#s0135
 * 
 * @see AbstractReferencer
 * 
 * @author Olivier Van Laere <oliviervanlaere@gmail.com>
 */
public class MedoidReferencer extends AbstractReferencer{

	/**
	 * Logger.
	 */
	protected static final Logger LOG = LoggerFactory.getLogger(MedoidReferencer.class);

    /**
     * Constructor.
     * @param parameters Parameters for referencing.
     */
    public MedoidReferencer(ReferencingParameters parameters) {
        super(parameters);
    }

    /**
     * Actual georeferencing implementation.
     * @param outputFileName Filename of the file to write the results to.
     */
    @Override
    public void run(String outputFileName) {
        // Load the test data
        LOG.info("Loading test from {}", parameters.getTestFile());
        DataLoading dl = new DataLoading();
        // Data is loaded WITHOUT feature selection (features = null)
        DataItem [] test_data = dl.loadDataFromFile(parameters.getTestFile(), 
                parameters.getTestParser(), parameters.getTestLimit(), null);
        // Load the classification results
        NaiveBayesResults classifier_output = 
                new NaiveBayesResults(parameters.getClassificationFile());
        // Prepare a map for the final predictions
        Map<Integer, Point> predictions = new TreeMap<Integer, Point>();
        // For each of the test items
        for (int i = 0; i < test_data.length; i++) {
            DataItem item = test_data[i];
            // Sanity check
            if (item != null) {
                // Determine class Id
                int classId = classifier_output.getPrediction(item.getId());
                // This info is ignored in this implementation
                int numberOfFeaturesUsed = classifier_output.getFeatureCount(
                        item.getId());
                Point prediction = predictions.get(item.getId());
                // If there is no prediction yet - which is standard for this
                // implementation
                if (prediction == null) {
                    predictions.put(item.getId(), 
                            parameters.getClassMapper().getMedoids().
                            get(classId));
                }
            }
            else
                throw new RuntimeException(
                        "This should not happen? Test item id " + i);
        }
        // Write to file
        ReferencingIO.writeLocationsToFile(predictions, outputFileName);
    }
}