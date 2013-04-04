package be.ugent.intec.ibcn.geo.features;

import be.ugent.intec.ibcn.geo.common.datatypes.GeoClass;
import be.ugent.intec.ugent.ibcn.geo.common.Util;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * This class provides the Dunning Log Likelihood for feature selection.
 *
 * For details on the algorithm
 *  @see http://www.sciencedirect.com/science/article/pii/S002002551300162X#s0075
 * 
 * @author Olivier Van Laere <oliviervanlaere@gmail.com>
 */
public class LogLikelihoodFeatureRanker extends ChiSquareFeatureRanker {

    /**
     * @return the name of this ranking method
     */
    @Override
    protected String getMethodName() {
        return "Log Likelihood";
    }
    
    /**
     * Constructor.
     * @param inputfile Input file with the training data
     * @param inputParser Parser implementation to use for the input data
     * @param medoidfile Input file with the medoids (cluster centra)
     * @param medoidParser Parser implementation to use for the medoid data 
     */
    public LogLikelihoodFeatureRanker(String inputfile, String inputParser, 
            String medoidfile, String medoidParser) {
        super(inputfile, inputParser, medoidfile, medoidParser);
    }

    /**
     * Helper method calculating the log likelihood value for a specific GeoClass.
     * @param geoclass the geoclass to calculate the score value for
     * @return a Map containing the tags of this area, 
     * ordered in descending order (best chi2 first), along with their scores
     */
    @Override
    protected Map<Object, Double> calculateScoreForClass(GeoClass geoclass) {
        // Prepare a map for the results
        Map<Object, Double> results  = new HashMap<Object, Double>();
        // Create a tag count map for this area
        Map<Object, Integer> classTagCount = this.otc.getClassTagCount(geoclass, data);
        // Fetch the number of items in this class
        long photos_in_area = geoclass.getElements().size();
        long N = -1;
        // Calculate the log likelihood values
        for (Object tag : classTagCount.keySet()) {
            long a = classTagCount.get(tag);
            long b = otc.getTagCount(tag) - a;
            long c = photos_in_area - a;
            long d = total_photos - photos_in_area - b;

            if (N == -1) {
                N = a+b+c+d;
            }
            else if ((a+b+c+d) != N)
                throw new RuntimeException("N != a+b+c+d: N="+ N);
            
            double aLog = Math.log(a);
            double bLog = Math.log(b);
            double cLog = Math.log(c);
            double dLog = Math.log(d);
            double abLog = Math.log(a+b);
            double acLog = Math.log(a+c);
            double bdLog = Math.log(b+d);
            double cdLog = Math.log(c+d);

            aLog = Double.isInfinite(aLog) ? 0 : aLog;
            bLog = Double.isInfinite(bLog) ? 0 : bLog;
            cLog = Double.isInfinite(cLog) ? 0 : cLog;
            dLog = Double.isInfinite(dLog) ? 0 : dLog;
            abLog = Double.isInfinite(abLog) ? 0 : abLog;
            acLog = Double.isInfinite(acLog) ? 0 : acLog;
            bdLog = Double.isInfinite(bdLog) ? 0 : bdLog;
            cdLog = Double.isInfinite(cdLog) ? 0 : cdLog;

            double log_likelihood = 2 * (a * aLog + b * bLog + 
                    c * cLog + d * dLog + N * Math.log(N) - 
                    (a+b) * abLog - (a+c) * acLog - 
                    (b+d) * bdLog - (c+d) * cdLog);
            
            results.put(tag, log_likelihood);
        }
        results = Util.sortByValueDescending(results);

        // There was some good reason to filter this once more, but
        // I forgot why it was?
        Map<Object, Double> filtered = new HashMap<Object, Double>();
        Iterator<Object> it = results.keySet().iterator();
        while (it.hasNext()){
            Object tag = it.next();
            filtered.put(tag,results.get(tag));
        }
        filtered = Util.sortByValueDescending(filtered);
        return filtered;
    }
}