package be.ugent.intec.ibcn.geo.features;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.ugent.intec.ibcn.geo.common.Util;
import be.ugent.intec.ibcn.geo.common.datatypes.GeoClass;
import be.ugent.intec.ibcn.geo.common.io.FeaturesIO;

/**
 * This class extends the Chi Square functionality for feature ranking in such
 * a way that the maximum value of each feature is taken as an indicator of 
 * where to rank it.
 *
 * @see be.ugent.intec.ibcn.geo.features.ChiSquareFeatureRanker
 * 
 * For details on the algorithm
 * @see http://www.sciencedirect.com/science/article/pii/S002002551300162X#s0070
 * 
 * @author Olivier Van Laere <oliviervanlaere@gmail.com>
 */
public class MaxChiSquareFeatureRanker extends ChiSquareFeatureRanker {

	/**
	 * Logger.
	 */
	protected static final Logger LOG = LoggerFactory.getLogger(MaxChiSquareFeatureRanker.class);
	
    /**
     * @return the name of this ranking method
     */
    @Override
    protected String getMethodName() {
        return "MaxChi2";
    }
    
    /**
     * Constructor.
     * @param inputfile Input file with the training data
     * @param inputParser Parser implementation to use for the input data
     * @param medoidfile Input file with the medoids (cluster centra)
     * @param medoidParser Parser implementation to use for the medoid data 
     */
    public MaxChiSquareFeatureRanker(String inputfile, String inputParser, 
            String medoidfile, String medoidParser) {
        super(inputfile, inputParser, medoidfile, medoidParser);
    }

    /**
     * Actual MaxChi2 feature selection (and convert it to a ranking).
     */
    @Override
    public void process(String outputfile) {
        LOG.info("Doing {} feature selection.", getMethodName());
        // Start timer
        long start = System.currentTimeMillis();        
        // Prepare a map with the overall best scores and features
        Map<Object, Double> overall_best = new HashMap<Object, Double>();
        
        // Prepare a thread pool
        ExecutorService executor = Executors.newFixedThreadPool(NR_THREADS);
        // Track futures
        List<Future<ScoringResult>> list = 
                new ArrayList<Future<ScoringResult>>();
        // For each GeoClass
        for (GeoClass geoclass : this.classmapper.getClasses()) {
            // Do chi2 calculation
            Callable<ScoringResult> worker = new ScoringHelper(geoclass);
            Future<ScoringResult> submit = executor.submit(worker);
            list.add(submit);
        }
        // Now, retrieve the results
        for (Future<ScoringResult> future : list) {
            try {
                ChiSquareFeatureRanker.ScoringResult result = future.get();
                Map<Object, Double> class_ranking = result.getScores();
                
                // Now for each feature in that ranking
                for (Object feature : class_ranking.keySet()) {
                    // If there is no value yet in the global map or or the chi2 
                    // value is better than the previous one
                    if ((!overall_best.containsKey(feature) || 
                        class_ranking.get(feature) > overall_best.get(feature)) 
                            && ((String)feature).trim().length() > 0) {
                        // Put or replace the chi2 value by the current one
                        overall_best.put(feature, class_ranking.get(feature));
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
        // This will make the executor accept no new threads
        // and finish all existing threads in the queue
        executor.shutdown();
        // Wait until all threads are finish
        while (!executor.isTerminated()) {}
        
        // Sort overall map by chi2 descending
        overall_best = Util.sortByValueDescending(overall_best);
        // Fetch the features
        List<Object> features = new ArrayList(overall_best.keySet());
        // Export the features to file
        FeaturesIO.exportFeaturesToFile(features, outputfile);
        // Stop the timer
        long stop = System.currentTimeMillis();
        LOG.info("Retained features: {} ({} ms.)", features.size(), (stop-start));
    }
}