package be.ugent.intec.ibcn.examples;

import be.ugent.intec.ibcn.geo.features.*;

/**
 * TODO Add comment
 * @author Olivier Van Laere <oliviervanlaere@gmail.com>
 */
public class FeatureExample {

    public static void main(String[] args) {
        // Provide the full path and filename of your training data
        String inputfile = "<your file here>";
        // Provide the full path and filename for the feature ranking
        String outputfile = "<your file here>";
        // Provide the full path and filename for the medoid file
        String medoidfile = "<your file here>";
        
        inputfile = "training";
        outputfile = "features";
        medoidfile = "medoids.2500";
        
        String trainingParser = 
                "be.ugent.intec.ibcn.geo.common.io.parsers.LineParserTrainingDefault";
        
        String medoidParser = 
                "be.ugent.intec.ibcn.geo.common.io.parsers.LineParserMedoid";
                

//        /**
//         * Chi2 example
//         */
//        
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
