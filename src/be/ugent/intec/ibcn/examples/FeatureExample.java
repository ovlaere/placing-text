package be.ugent.intec.ibcn.examples;

import be.ugent.intec.ibcn.geo.common.Util;
import be.ugent.intec.ibcn.geo.features.*;

/**
 * This class provides a simple main method that illustrates the use of the
 * different feature ranking algorithms. The output of the algorithms
 * are a ranking of the existing features in the training file, and should be
 * in a file like
 *  <rank> <feature>
 * This ranking is then used by the classifier.
 * 
 * @author Olivier Van Laere <oliviervanlaere@gmail.com>
 */
public class FeatureExample {

    public static void main(String[] args) {
        
        // Shorthand for the dir prefix for the filenames
        String dataDir = "/"; // Your actual dataDir here
        
        // Provide the full path and filename of the files that will be used
        String trainingFile = dataDir + "training"; // Your actual training file here
        String medoidfile = dataDir + "medoids"; // Your actual medoid file here
        String featureTemplate  = dataDir + "features.@1";
        
        // Parser classes
        String trainingParser = "be.ugent.intec.ibcn.geo.common.io.parsers.LineParserTrainingItem";
        String medoidParser = "be.ugent.intec.ibcn.geo.common.io.parsers.LineParserMedoid";

//        /**
//         * Chi2 example
//         */
//        
//        ChiSquareFeatureRanker chi2 = new ChiSquareFeatureRanker(trainingFile, trainingParser, 
//                medoidfile, medoidParser);
//        chi2.process(Util.applyTemplateValues(featureTemplate, new String[]{"chi2"}));
//
//        /**
//         * Max-Chi2 example
//         */
//        
//        MaxChiSquareFeatureRanker maxchi2 = new MaxChiSquareFeatureRanker(
//                trainingFile, trainingParser, medoidfile, medoidParser);
//        maxchi2.process(Util.applyTemplateValues(featureTemplate, new String[]{"maxchi2"}));
//        
//        /**
//         * LogLikelihood example
//         */
//        
//        LogLikelihoodFeatureRanker loglike = new LogLikelihoodFeatureRanker(
//                trainingFile, trainingParser, medoidfile, medoidParser);
//        loglike.process(Util.applyTemplateValues(featureTemplate, new String[]{"loglike"}));
//        
//        /**
//         * Information Gain example
//         */
//        
//        InformationGainFeatureRanker ig = new InformationGainFeatureRanker(
//                trainingFile, trainingParser, medoidfile, medoidParser);
//        ig.process(Util.applyTemplateValues(featureTemplate, new String[]{"ig"}));
//        
//        /**
//         * Most Frequently Used (MFU) example
//         */
//        MostFrequentlyUsedFeatureRanker mfu = new MostFrequentlyUsedFeatureRanker(
//                trainingFile, trainingParser);
//        mfu.process(Util.applyTemplateValues(featureTemplate, new String[]{"mostused"}));
//        
//        /**
//         * Geospread example
//         */
//        
//        GeoSpreadFeatureRanker gsf = new GeoSpreadFeatureRanker();
//        gsf.process(trainingFile, trainingParser, -1, Util.applyTemplateValues(featureTemplate, new String[]{"geo"}));
//        
//        /**
//         * Ripley K example
//         */
//        
//        RipleyKFeatureRanker rf = new RipleyKFeatureRanker(trainingFile, trainingParser);
//        rf.process(Util.applyTemplateValues(featureTemplate, new String[]{"ripley_KN"}), 1, RipleyKFeatureRanker.RIPLEY_K_N_SCORE);
// 
//        rf = new RipleyKFeatureRanker(trainingFile, trainingParser);
//        rf.process(Util.applyTemplateValues(featureTemplate, new String[]{"ripley_KlogN"}), 1, RipleyKFeatureRanker.RIPLEY_K_LOG_N_SCORE);
    }
}
