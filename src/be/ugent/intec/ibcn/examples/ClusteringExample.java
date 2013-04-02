package be.ugent.intec.ibcn.examples;

import be.ugent.intec.ibcn.geo.clustering.*;
import be.ugent.intec.ibcn.geo.common.datatypes.DataItem;
import be.ugent.intec.ibcn.geo.common.datatypes.Point;
import be.ugent.intec.ibcn.geo.common.interfaces.LineParserPoint;
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
        // Prepare the parameters - use the default values
        ClusteringParameters cp = new ClusteringParameters();
        // But we set out own input parser
        cp.setLineParserClassNameForInput(
                "be.ugent.intec.ibcn.examples.clustering.MyClusterInputParser");
        
        // Provide the full path and filename of your training data
        String inputfile = "<your file here>";
        // Provide the full path and filename for the output
        String outputfile = "<your file here>";
        
        // Prepare the ClusteringIO
        ClusteringIO cio = new ClusteringIO();
        
        /**
         * Load the input data
         */
        
        // Load all the data from the file to cluster
        Point [] data = cio.loadDataFromFile(inputfile, cp.getLineParserClassNameForInput());
        
        // You could also load the first x lines of your training data using
//        int x = 100000;
//        Point [] data = cio.loadDataFromFile(inputfile, cp.getLineParserClassNameForInput(), x);
        
        /**
         * Example of GridClustering with 1 degree latitude and 1 degree longitude
         */
        
        AbstractClustering clusteringGrid = new GridClustering(cp, data, 1, 1);
        clusteringGrid.cluster(outputfile + ".grid");
        
        /**
         * Example of PamClustering. This one need special PamParameters.
         */
        PamParameters pp = new PamParameters();
        pp.setWriteFullClusteringToFile(true);
        // Init a Pam with k = 2500
        int k = 2500;
        AbstractClustering clusteringPam = new PamClustering(pp, data, k);
        clusteringPam.cluster(outputfile + ".pam");
    }
    
    /**
     * This class provides a dummy implementation of LineParserPoint.
     * 
     * My sample file format is ID,...,lat,lon,...
     * 
     * @author Olivier Van Laere <oliviervanlaere@gmail.com>
     */
    private class MyClusterInputParser extends LineParserPoint {

        /**
        * Constructor.
        */
        public MyClusterInputParser() {
            super();
        }

        /**
        * Implementation of parse.
        */
        @Override
        public DataItem parse(String line) {
            // Prepare an empty DataItem.
            DataItem item = null;
            try {
                // Split the line
                String [] values = pattern_comma.split(line.toLowerCase());
                // Get the id from the first item
                int id = Integer.parseInt(values[0]);
                // Parse latitude from index 2
                double lat = Double.parseDouble(values[2]);
                // Parse latitude from index 3
                double lon = Double.parseDouble(values[3]);
                // Instantiate the DataItem
                item = new DataItem(id, lat, lon, null);
            }
            // In case a parse error occurs, ignore the error but mark this error.
            // At the end of the data loading, this error count will be presented
            // and provided feedback about the errors during parsing. Error items
            // will not be instantiated.
            catch (Exception e) {
                this.errors++;
            }
            // Keep track of a processed count
            this.processed++;
            return item;
        }
    }
}