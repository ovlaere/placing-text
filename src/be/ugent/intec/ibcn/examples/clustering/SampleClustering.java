package be.ugent.intec.ibcn.examples.clustering;

import be.ugent.intec.ibcn.geo.clustering.AbstractClustering;
import be.ugent.intec.ibcn.geo.clustering.ClusteringParameters;
import be.ugent.intec.ibcn.geo.clustering.GridClustering;
import be.ugent.intec.ibcn.geo.common.datatypes.Point;
import be.ugent.intec.ibcn.geo.common.io.ClusteringIO;

/**
 * This class provides a mini-howto on how to run the clustering algorithms.
 * 
 * Depending on the amount of input data, you might want to set the -Xmx (and
 * most likely -Xms) VM parameters to a higher value than the default one.
 * 
 * @author Olivier Van Laere <oliviervanlaere@gmail.com>
 */
public class SampleClustering {

    public static void main(String[] args) {
        // Prepare the parameters - use the default values
        ClusteringParameters cp = new ClusteringParameters();
        // But we set out own input parser
        cp.setLineParserClassNameForInput(
                "be.ugent.intec.ibcn.examples.clustering.MyClusterInputParser");
        // Provide the full path and filename of your training data
//        String inputfile = "<your file here>";
        
        String inputfile = "/Users/ovlaere/achterberg/workspaces/netbeans/Curiosity/data/placing2012_generic/data_mediaeval2012_training.txt.run1";
        
        // Prepare the ClusteringIO
        ClusteringIO cio = new ClusteringIO();
        
        /*
         * Load the input data
         */
        
        // Load all the data from the file to cluster
//        Point [] data = cio.loadDataFromFile(inputfile, 
//                cp.getLineParserClassNameForInput());
        
        // You could also load the first x lines of your training data using
        int x = 100000;
        Point [] data = cio.loadDataFromFile(inputfile, 
                cp.getLineParserClassNameForInput(), x);
        
        // Set your outpufile
        String outputfile = "/Users/ovlaere/achterberg/workspaces/netbeans/Curiosity/data/placing2012_generic/test_clustering";
        
        // Example of GridClustering with 1 degree latitude and 1 degree longitude
        
        AbstractClustering clusteringGrid = new GridClustering(cp, data, 1, 1);
        clusteringGrid.cluster(outputfile + ".grid");
        
        
        
    }
}