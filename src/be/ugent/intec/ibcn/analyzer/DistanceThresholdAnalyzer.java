package be.ugent.intec.ibcn.analyzer;

import be.ugent.intec.ibcn.geo.common.datatypes.DataItem;
import be.ugent.intec.ibcn.geo.common.datatypes.Point;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This class demonstrates how a simple analyzer for location predictions is
 * written.
 * 
 * As an extension of AbstractAnalyzer, the test data is available to this 
 * class.
 * 
 * The DistanceThresholdAnalyzer takes an array of distance thresholds in 
 * the constructor that are used to evaluate the location predictions.
 * 
 * For each prediction for the test data, the distance is calculate to the 
 * ground truth location, and stored in a list. From this list, the median
 * error is determined. Also, the distance is checked against the different 
 * thresholds and counters are used to report the number of items within the
 * given thresholds.
 * 
 * @author Olivier Van Laere <oliviervanlaere@gmail.com>
 */
public class DistanceThresholdAnalyzer extends AbstractAnalyzer {

    /**
     * Object capable of finding the closest class for a given test ID.
     */
    protected NearestMedoidFinder nmf;

    /**
     * Array of double thresholds to evaluate the location predictions in.
     */
    protected double [] distances;

    /**
     * Counter that tracks the number of items within certain distance thresholds.
     */
    protected int [] counters;
    
    /**
     * General counter that tracks the number of processed items.
     */
    protected int processed = 0;
    
    /**
     * Counter that tracks the classifier accuracy (# correct classes/#total).
     */
    protected int classCorrect;
    
    /**
     * Constructor.
     * @param training_data Training data
     * @param test_data Test data
     */
    public DistanceThresholdAnalyzer(AnalyzerParameters parameters, double [] distances) {
        super(parameters);
        this.distances = distances;
        if (parameters.getClassMapper() != null)
            this.nmf = new NearestMedoidFinder(parameters.getClassMapper().getMedoids());
    }
    
    @Override
    public void run(String filename) {
        // Init the different threshold counters
        this.counters = new int [distances.length];
        // init accuracy counter
        this.classCorrect = 0;
        // Init processed counter
        this.processed = 0;
        System.out.println("Processing..." );
        try {
            // Read the predictions
            BufferedReader in = new BufferedReader(new FileReader(filename));
            List<Double> tmp_distances = new ArrayList<Double>();
            String line = in.readLine();
            while (line != null) {
                String [] values = line.split(" ");
                int id = Integer.parseInt(values[0]);
                double latitude = Double.parseDouble(values[1]);
                double longitude = Double.parseDouble(values[2]);
                Point predictedLocation = new Point(-1, latitude, longitude);
                double distance;
                // Sanity check
                if (test_data != null) {
                    DataItem item = test_data[id - 1];
                    // Sanity check
                    if (item != null) {
                        // Fetch the distance between the ground truth and
                        // the prediction
                        distance = item.distance(predictedLocation);
                        // If we have a NearestMedoidFinder
                        if (this.nmf != null) {
                            // Fetch the class of the predicted location
                            int predictedClass = nmf.getNearestMedoid(predictedLocation);
                            // Fetch the class where it should be (ground truth)
                            int actualClass = nmf.getNearestMedoid(item);
                            if (predictedClass == actualClass)
                                classCorrect++;
                        }
                    
                        // Add the distance we found
                        tmp_distances.add(distance);
                        // Track the distance in the buckets we have defined
                        for (int i = 0; i < this.distances.length; i++) {
                            if (distance <= this.distances[i]) {
                                counters[i]++;
                            }
                        }
                        processed++;
                    }
                }
                // Read the next line of the predictions
                line = in.readLine();
            }
            // Close the input
            in.close();
            // Report the results
            results(tmp_distances, filename);
        }
        catch (IOException ex) {
            System.err.println("IOException: " + ex.getMessage());
        }
    }
    
    /**
     * Result reporting to the console.
     * @param tmp_distances The list of error distances over all the items
     * @param filename the filename with the results that is being analyzed
     */
    protected void results(List<Double> tmp_distances, String filename) {
        // Sort the list of distances
        Collections.sort(tmp_distances);
        double distance = -1;
        // Determine the median distance
        if (tmp_distances.size() > 0) {
            if (tmp_distances.size() % 2 == 0) {
                double v1 = tmp_distances.get((tmp_distances.size() / 2) - 1);
                double v2 = tmp_distances.get((tmp_distances.size() / 2));
                distance = (v1 + v2) / 2;
            } else {
                distance = tmp_distances.get(tmp_distances.size() / 2);
            }
        }
        System.out.print(filename + "\t");
        for (int i = 0; i < this.distances.length; i++)
            System.out.print(formatter.format(counters[i] * 100. / processed) + "\t");
        System.out.println(formatter.format(distance));
        System.out.println("");
        // If there was a need for accuracy on clustering level
        if (this.nmf != null) {
            // Report the accuracy at the clustering level
            System.out.println("Acc "+ parameters.getClassMapper().size() +":\t" + 
                    classCorrect+"\t"+processed+"\t"+ 
                    formatter.format(classCorrect * 100. / processed));
            System.out.println("");
        }
        // Report the distance stats
        for (int i = 0; i < this.distances.length; i++)
            System.out.println(this.distances[i] + "\t" + formatter.format(counters[i] * 100. / processed) + "\t"+counters[i]+"\t"+processed);
        // Report quartile statistics
        System.out.println(filename + " Q1\t" + formatter.format(tmp_distances.get((int)(tmp_distances.size()*0.25))));
        System.out.println(filename + " Q2\t" + formatter.format(distance));
        System.out.println(filename + " Q3\t" + formatter.format(tmp_distances.get((int)(tmp_distances.size()*0.75))));
    }
}