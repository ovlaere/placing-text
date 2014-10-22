package be.ugent.intec.ibcn.geo.clustering.datatypes;

import be.ugent.intec.ibcn.geo.common.datatypes.Point;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * This class represents a cluster created by the Clustering algorithm.
 *
 * A Cluster has a specific Point as its center, and a Set of Points as
 * the elements that belong to this cluster.
 * 
 * @author Olivier Van Laere <oliviervanlaere@gmail.com>
 */
public class Cluster {

    /**
     * Set of Points that represents the Points that belong to this cluster.
     */
    protected Set<Point> elements = new HashSet<Point>();

    /**
     * Clear the elements that belong to this cluster.
     */
    public void clearElements() {
        this.elements.clear();
    }

    /**
     * @return the Set of Points that belong to this Cluster.
     */
    public Set<Point> getElements() {
        return this.elements;
    }

    /**
     * Synchronized method that adds a Point to the Cluster.
     * @param p Point to be added to the Cluster
     * @return true if the element was added to the Cluster
     */
    public synchronized boolean addElement(Point p) {
        return elements.add(p);
    }

    public synchronized boolean addAllElements(Collection<Point> c) {
        return elements.addAll(c);
    }

    /**
     * Synchronized method that removes a Point from the Cluster.
     * @param p Point to be removed to the Cluster
     * @return true if the element was removed from the Cluster
     */
    public synchronized boolean removeElement(Point p) {
        return elements.remove(p);
    }

    /**
     * @return The size of the Cluster elements
     */
    public int size() {
        return elements.size();
    }

    /**
     * Reference to the Point center of this Cluster.
     */
    protected Point center;

    /**
     * @return the center Point of this Cluster
     */
    public Point getCenter() {
        return center;
    }

    /**
     * Set the center Point of this Cluster
     * @param center a Point that will become the new Cluster center
     */
    public void setCenter(Point center) {
        this.center = center;
    }

    /**
     * Reference to the original Cluster center, in case a cluster improvement
     * needs to be undone.
     */
    protected Point center_original = null;

    /**
     * Keep track of the Cluster cost, as calculated by the latest call to the
     * calculateCost method.
     */
    protected double cost = 0;

    /**
     * @return the cost of the cluster.
     */
    public double getCost() {
        return this.cost;
    }
    
    /**
     * Constructor.
     * @param center The initial Cluster center
     */
    public Cluster(Point center) {
        this.center = center;
    }

    /**
     * Copy constructor.
     * @param other another Cluster on which this copy is based.
     */
    public Cluster(Cluster other){
        this.elements = new HashSet<Point>(other.elements);
        this.center = other.center;
        this.center_original = other.center_original;
        this.cost = other.cost;
    }

    /**
     * @return Return (and store) the cost of the Cluster. The cost in this
     * context is defined as the sum of the distance of each element to the
     * Cluster center.
     */
    public double calculateCost() {
        double sum = 0;
        for (Point p : elements)
            sum += center.distance(p);
        this.cost = sum;
        return cost;
    }

    /**
     * Swap the current Cluster center for a new Point. This method will
     * store the original center and automatically invoke a new calculation
     * of the Cluster cost.
     * @param p The Point that should become the new Cluster center.
     */
    public void swapCenter(Point p) {
        this.center_original = center;
        this.center = p;
        removeElement(p);
        addElement(center_original);
        calculateCost();
    }

    /**
     * Undo the last center swap. This method will restore the original
     * Cluster center.
     */
    public void undoCenterSwap() {
        if (center_original != null) {
            Point p = this.center;
            this.center = center_original;
            removeElement(center_original);
            addElement(p);
            center_original = null;
        }
    }

    /**
     * Clear some variables that make it possible to undo a center swap.
     */
    public void clearUndoSwap() {
        this.center_original = null;
    }

    /**
     * equals() implementation
     * @param obj
     * @return true if the center of the two clusters are equal (which means
     * equal ID)
     * 
     * @see Point
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Cluster other = (Cluster) obj;
        if (this.center != other.center && (this.center == null || 
                !this.center.equals(other.center))) {
            return false;
        }
        return true;
    }

    /**
     * hashcode() implementation
     * @return a hashcode
     */
    @Override
    public int hashCode() {
        int hash = 3;
        hash = 79 * hash + (this.center != null ? this.center.hashCode() : 0);
        return hash;
    }

    /**
     * @return A String representation of this cluster, with some added 
     * statistics.
     */
    public String clustering() {
        StringBuilder result = new StringBuilder();
        result.append("[Cluster] Center: ");
        result.append(center);
        result.append("\n");
        result.append("[Cluster] Size: ");
        result.append(elements.size());
        result.append("\n");
        result.append("[Cluster] Cost: ");
        result.append(cost);
        result.append("\n");
        for (Point p : elements) {
            result.append(" - ");
            result.append(p);
            result.append("\n");
        }
        return result.toString();
    }

    /**
     * @return A String representation of this cluster.
     */
    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        // ID, lat, lon of center element
        result.append(center.getId());
        result.append(",");
        result.append(center.getLatitude());
        result.append(",");
        result.append(center.getLongitude());
        result.append("\n");
        // Of all other elements
        for (Point p : elements) {
            // That are not the center
            if (p.getId() != center.getId()) {
                // ID of center, ID, lat, lon of element
                result.append(center.getId());
                result.append(",");
                result.append(p.getId());
                result.append(",");
                result.append(p.getLatitude());
                result.append(",");
                result.append(p.getLongitude());
                result.append("\n");
            }
        }
        return result.toString();
    }    
}