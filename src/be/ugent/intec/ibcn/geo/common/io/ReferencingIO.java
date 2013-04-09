package be.ugent.intec.ibcn.geo.common.io;

import be.ugent.intec.ibcn.geo.common.datatypes.Point;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

/**
 * TODO Add comment
 * @author Olivier Van Laere <oliviervanlaere@gmail.com>
 */
public class ReferencingIO {
    
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