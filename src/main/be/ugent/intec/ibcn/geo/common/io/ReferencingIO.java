package be.ugent.intec.ibcn.geo.common.io;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.ugent.intec.ibcn.geo.common.datatypes.Point;

/**
 * IO Class for the georeferencing package.
 * 
 * @author Olivier Van Laere <oliviervanlaere@gmail.com>
 */
public class ReferencingIO {

	/**
	 * Logger.
	 */
	protected static final Logger LOG = LoggerFactory.getLogger(ReferencingIO.class);

    /**
     * Write a given map of location predictions to file.
     * @param predictions A map containing a Point prediction for each ID
     * @param outputFileName the filename to write the predictions to.
     */
    public static void writeLocationsToFile(Map<Integer, Point> predictions,
            String outputFileName) {
    	PrintWriter out = null;
        try {
            out = new PrintWriter(new FileWriter(outputFileName));
            for (int testId : predictions.keySet()) {
                Point predictedLocation = predictions.get(testId);
                if (predictedLocation != null)
                    out.println((testId) + " " + 
                            predictedLocation.getLatitude() + " " + 
                            predictedLocation.getLongitude());
                else {
                    throw new RuntimeException("This should not happen!? "
                            + "EMPTY PREDICTION" + testId);
                }
            }
            out.close();
        }
        catch (IOException e) {
            LOG.error("IOException: {}", e.getMessage());
        }
        finally {
        	if (out != null)
            	out.close();
        }
    }
}