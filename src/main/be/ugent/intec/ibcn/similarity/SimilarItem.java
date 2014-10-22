package be.ugent.intec.ibcn.similarity;

import be.ugent.intec.ibcn.geo.common.datatypes.DataItem;

/**
 * This class represents a comparable DataItem. The comparison is done using
 * the similarity scores of the items.
 * 
 * @see Similarity
 * @see DataItem
 * 
 * @author Olivier Van Laere <oliviervanlaere@gmail.com>
 */
public class SimilarItem implements Comparable<SimilarItem> {

    /**
     * The item for which the score is kept in here.
     */
    private DataItem item;
    
    /**
     * @return The DataItem this SimilarItem refers.
     */
    public DataItem getItem() {
        return item;
    }

    /**
     * Set the DataItem this SimilarItem refers.
     * @param item a DataItem to keep a score for.
     */
    public void setItem(DataItem item) {
        this.item = item;
    }

    /**
     * The score for this DataItem.
     */
    private double score;

    /**
     * @return The score we keep for this DataItem.
     */
    public double getScore() {
        return score;
    }

    /**
     * Set the score for the DataItem we keep.
     * @param score a double score value.
     */
    public void setScore(double score) {
        this.score = score;
    }

    /**
     * Constructor.
     * @param item DataItem to keep a score for
     * @param score Actual similarity score for the given DataItem.
     */
    public SimilarItem(DataItem item, double score) {
        this.item = item;
        this.score = score;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final SimilarItem other = (SimilarItem) obj;
        if (this.item != other.item && (this.item == null || 
                !this.item.equals(other.item))) {
            return false;
        }
        if (Double.doubleToLongBits(this.score) != 
                Double.doubleToLongBits(other.score)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 37 * hash + (this.item != null ? this.item.hashCode() : 0);
        hash = 37 * hash + (int) (Double.doubleToLongBits(this.score) ^ 
                (Double.doubleToLongBits(this.score) >>> 32));
        return hash;
    }

    @Override
    public String toString() {
        return "SimilarItem{" + "item=" + item + ", score=" + score + '}';
    }

    /**
     * compareTo implementation.
     * Compares items based on similarity score.
     */
    @Override
    public int compareTo(SimilarItem t) {
        if (this.score > t.score)
            return -1;
        else if (this.score < t.score)
            return 1;
        else
            return 0;
    }
}