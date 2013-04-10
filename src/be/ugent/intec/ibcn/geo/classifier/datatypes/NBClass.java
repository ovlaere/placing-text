package be.ugent.intec.ibcn.geo.classifier.datatypes;

import be.ugent.intec.ibcn.geo.common.datatypes.GeoClass;

/**
 * This class extends the GeoClass concept to a NaiveBayes class. These
 * are the classes that are used for classification. This extension adds
 * a field that holds the prior probability of the class used by the classifier.
 * 
 * @author Olivier Van Laere <oliviervanlaere@gmail.com>
 */
public class NBClass extends GeoClass {

    /**
     * Prior probability (in log space) of this NBClass.
     */
    private double prior = 0;

    /**
     * Set the prior probability (in log space) of this NBClass.
     * @param prior 
     */
    public void setPrior(double prior) {
        this.prior = prior;
    }
    
    /**
     * @return the prior probability (in log space) of this NBClass.
     */
    public double getPrior() {
        return this.prior;
    }    
    
    /**
     * Constructor.
     * @param id ID of the medoid used for this NBClass.
     */
    public NBClass(int id) {
        super(id);
    }

    /**
     * Constructor.
     * @param id ID of the medoid used for this NBClass.
     * @param originalId original ID (from file) for the medoid used for this 
     * NBClass.
     */
    public NBClass(int id, int originalId) {
        super(id, originalId);
    }
}