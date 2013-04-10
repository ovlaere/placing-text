package be.ugent.intec.ibcn.geo.common.io;

import be.ugent.intec.ibcn.geo.common.datatypes.Point;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

/**
 * IO Class for the georeferencing package.
 * 
 * @author Olivier Van Laere <oliviervanlaere@gmail.com>
 */
public class ReferencingIO {
 
    /**
     * Write a given map of location predictions to file.
     * @param predictions A map containing a Point prediction for each ID
     * @param outputFileName the filename to write the predictions to.
     */
    public static void writeLocationsToFile(Map<Integer, Point> predictions,
            String outputFileName) {
        try {
            PrintWriter out = new PrintWriter(new FileWriter(outputFileName));
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
        catch (IOException ex) {
            System.err.println("IOException: " + ex.getMessage());
        }
    }
}