package be.ugent.intec.ibcn.examples;

import be.ugent.intec.ibcn.geo.clustering.*;
import be.ugent.intec.ibcn.geo.common.Util;
import be.ugent.intec.ibcn.geo.common.datatypes.Point;
import be.ugent.intec.ibcn.geo.common.io.ClusteringIO;

/**
 * This class provides a mini-howto on how to run the clustering algorithms.
 * 
 * Depending on the amount of input data, you might want to set the -Xmx (and
 * most likely -Xms) VM parameters to a higher value than the default one.
 * 
 * The input file needs to contain at least info such as ID,lat,lon for each
 * training item.
 * 
 * @author Olivier Van Laere <oliviervanlaere@gmail.com>
 */
public class ClusteringExample {

    public static void main(String[] args) {
        
        // Shorthand for the dir prefix for the filenames
        String dataDir = "./"; // Your actual dataDir here
        
        // Provide the full path and filename of the files that will be used
        String trainingFile = dataDir + "training"; // Your actual training file here
        String medoidTemplate = dataDir + "medoids.@1";
        // Parser classes
        String clusterParser = "be.ugent.intec.ibcn.geo.common.io.parsers.LineParserClusterPoint";
                
        // Prepare the parameters - use the default values
        ClusteringParameters cp = new ClusteringParameters();
        // But we set out own input parser
        cp.setLineParserClassNameForInput(clusterParser);
        // Prepare the ClusteringIO
        ClusteringIO cio = new ClusteringIO();
        
        /**
         * Load the input data
         */
        
        // Load all the data from the file to cluster
        Point [] data = cio.loadDataFromFile(trainingFile, cp.getLineParserClassNameForInput());
        
        // You could also load the first x lines of your training data using
//        int x = 100000;
//        Point [] data = cio.loadDataFromFile(trainingFile, cp.getLineParserClassNameForInput(), x);
        
        /**
         * Example of GridClustering with 1 degree latitude and 1 degree longitude
         */
        
//        AbstractClustering clusteringGrid = new GridClustering(cp, data, 1, 1);
//        clusteringGrid.cluster(Util.applyTemplateValues(medoidTemplate, new String[]{"grid"}));
        
        /**
         * Example of PamClustering. This one need special PamParameters.
         */
//        PamParameters pp = new PamParameters();
//        // Init a Pam with k = 2500
//        int k = 2500;
//        AbstractClustering clusteringPam = new PamClustering(pp, data, k);
//        clusteringPam.cluster(Util.applyTemplateValues(medoidTemplate, new String[]{""+k}));
    }
}