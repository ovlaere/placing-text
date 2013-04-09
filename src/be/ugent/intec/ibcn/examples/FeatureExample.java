package be.ugent.intec.ibcn.examples;

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
        // Provide the full path and filename of your training data
        String inputfile = "<training_file_here>";
        // Provide the full path and filename for the feature ranking
        String outputfile = "<feature_output_here>";
        // Provide the full path and filename for the medoid file
        String medoidfile = "<clustering_input_here>";
        
        String trainingParser = 
                "be.ugent.intec.ibcn.geo.common.io.parsers.LineParserTrainingItem";
        
        String medoidParser = 
                "be.ugent.intec.ibcn.geo.common.io.parsers.LineParserMedoid";
                

        /**
         * Chi2 example
         */
        
//        ChiSquareFeatureRanker chi2 = new ChiSquareFeatureRanker(inputfile, trainingParser, 
//                medoidfile, medoidParser);
//        chi2.process(outputfile + ".chi2");
//
//        /**
//         * Max-Chi2 example
//         */
//        
//        MaxChiSquareFeatureRanker maxchi2 = new MaxChiSquareFeatureRanker(
//                inputfile, trainingParser, medoidfile, medoidParser);
//        maxchi2.process(outputfile + ".maxchi2");
//        
//        /**
//         * LogLikelihood example
//         */
//        
//        LogLikelihoodFeatureRanker loglike = new LogLikelihoodFeatureRanker(
//                inputfile, trainingParser, medoidfile, medoidParser);
//        loglike.process(outputfile + ".loglike");
//        
//        /**
//         * Information Gain example
//         */
//        
//        InformationGainFeatureRanker ig = new InformationGainFeatureRanker(
//                inputfile, trainingParser, medoidfile, medoidParser);
//        ig.process(outputfile + ".ig");
//        
//        /**
//         * Most Frequently Used (MFU) example
//         */
//        MostFrequentlyUsedFeatureRanker mfu = new MostFrequentlyUsedFeatureRanker(
//                inputfile, trainingParser);
//        mfu.process(outputfile + ".mostused");
//        
//        /**
//         * Geospread example
//         */
//        
//        GeoSpreadFeatureRanker gsf = new GeoSpreadFeatureRanker();
//        gsf.process(inputfile, trainingParser, -1, outputfile + ".geo");
    }
}
