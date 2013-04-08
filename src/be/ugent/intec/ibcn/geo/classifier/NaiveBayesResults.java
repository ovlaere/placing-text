package be.ugent.intec.ibcn.geo.classifier;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * This class serves the results of the classification step as an object.
 * 
 * @see NaiveBayes
 * 
 * @author Olivier Van Laere <oliviervanlaere@gmail.com>
 */
public class NaiveBayesResults {

    /**
     * Map containing the predicted area for every specific photo.
     */
    private Map<Integer, Integer> predictions;

    /**
     * Return the predicted area id for the specified test item.
     * @param id the id of the item you want the prediction for
     * @return the id/areaID of the prediction for the specified item.
     */
    public int getPrediction(int id) {
        return this.predictions.get(id);
    }
    
    /**
     * Put the predicted class for a test item by ID.
     * @param id The ID of the test item to put the prediction for.
     * @param classId The predicted class for the test item.
     */
    public void putPrediction(int id, int classId) {
        this.predictions.put(id, classId);
    }
    
    /**
     * @return the number of predictions in this object.
     */
    public int size() {
        return this.predictions.size();
    }
    
    /**
     * Map containing, for each of the different classes, the items predicted 
     * in that class. This aggregation might come handy when doing similarity 
     * search in a single class.
     */
    private Map<Integer, List<Integer>> class_items;
    
    /**
     * Get a list of training items in a given class. Useful for processing all
     * test items predicted in a single class at once.
     * @param classId The ID of the class for which you want to get a list
     * of the test items predicted there.
     * @return A list of test items predicted in the given class.
     */
    public List<Integer> getItemsForClass(int classId) {
        return this.class_items.get(classId);
    }

    /**
     * Keeps track of the number of features that were used when predicting
     * the possible location for the specified test items.
     */
    private Map<Integer, Integer> features;

    /**
     * @param id the ID of the test item for which you want to get the
     * feature count
     * @return the number of features that were used when predicting
     * the possible location for the specified test item.
     */
    public int getFeatureCount(int id) {
        return this.features.get(id);
    }

    /**
     * Put the tag count for a test item by ID.
     * @param id The ID of the test item for which you want to put the count.
     * @param featureCount the actual feature count you want to put.
     */
    public void putFeatureCount(int id, int featureCount) {
        this.features.put(id, featureCount);
    }
    
    /**
     * Keeps track of the scores that were assigned to the predictions.
     */
    private Map<Integer, Double> scores;

    /**
     * @param id The ID of the test item for which you want to get the score.
     * @return the score that was assigned to the prediction
     */
    public double getScore(int id) {
        return this.scores.get(id);
    }

    /**
     * Add a score for a test item by ID.
     * @param id ID of the test item to score.
     * @param score Double (log) score for the test item.
     */
    public void putScore(int id, double score) {
        this.scores.put(id, score);
    }
    
    /**
     * Default constructor.
     */
    public NaiveBayesResults() {
        this.predictions = new TreeMap<Integer, Integer>();
        this.class_items = new TreeMap<Integer, List<Integer>>();
        this.features = new TreeMap<Integer, Integer>();
        this.scores = new TreeMap<Integer, Double>();
    }
    
    /**
     * Constructor.
     * @param filename The filename containing the NB results.
     */
    public NaiveBayesResults(String filename) {
        this();
        System.out.println("Loading NB Results from " + filename);
        // Load the data from file
        initNaiveBayesResults(filename);
    }
    
    /**
     * Private helper method for loading existing NaiveBayesResults from file.
     * 
     * The file format should be
     *  ID ClassID Score #Features_used
     * 
     * @param filename The filename to load the existing results from.
     */
    private void initNaiveBayesResults(String filename) {
        try {
            // Set up reader
            BufferedReader in = new BufferedReader(new FileReader(filename));
            String line = in.readLine();
            // While we have lines
            while (line != null) {
                // Split lines on tab
                String [] values = line.split("\t");
                int id = Integer.parseInt(values[0]);
                int prediction = Integer.parseInt(values[1]);
                double score = Double.parseDouble(values[2]);
                int tagCount = Integer.parseInt(values[3]);
                List<Integer> list = class_items.get(prediction);
                if (list == null)
                    list = new ArrayList<Integer>();
                list.add(id);
                // Put data in data structures
                class_items.put(prediction, list);
                predictions.put(id, prediction);
                features.put(id, tagCount);
                scores.put(id, score);
                line = in.readLine();
            }
            in.close();
        }
        catch (IOException ex) {
            System.err.println("IOException: " + ex.getMessage());
        }
    }
    
    /**
     * Write the contents of the NaiveBayesResults objects to file.
     * @param filename The file to write the contents to.
     */
    public void writeNaiveBayesResultsToFile(String filename) {
        try {
            // Open writer
            PrintWriter out = new PrintWriter(new FileWriter(filename));
            // Write ID classID score #features_used
            for (int id : predictions.keySet())
                out.println(id + "\t" + predictions.get(id) + "\t" + 
                            scores.get(id) + "\t" + features.get(id));
            out.close();
        } catch (IOException e) {
            System.err.println("IOException: " + e.getMessage());
        }
    }
}