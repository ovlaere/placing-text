package be.ugent.intec.ibcn.geo.clustering.datatypes;

import be.ugent.intec.ibcn.geo.common.datatypes.Point;

/**
 * This class represents a special type of Point: an InitialPoint.
 *
 * An InitialPoint is used in the PAM algorithm as an initial medoid center.
 * The implementation of the algorithm requires however that no point with
 * equal lat/lon are selected as initial points. For this reason, this
 * extension is created. The only difference with the Point class is that
 * the equals method is overriden to consider Points with equal lat/lon as
 * equal, even though though their id can differ.
 * 
 * @see PamClustering
 * 
 * @author ovlaere
 */
public class InitialPoint extends Point{

    /**
     * Constuctor.
     * @param id id of the Point
     * @param latitude latitude of the Point
     * @param longitude longitude of the Point
     */
    public InitialPoint(int id, double latitude, double longitude) {
        super(id, latitude, longitude);
    }

    /**
     * Copy constructor
     * @param p Another Point of which this Point is going to be a copy.
     */
    public InitialPoint(Point p) {
        this(p.getId(), p.getLatitude(), p.getLongitude());
    }

    /**
     * Override of the equals method in the Point class. The difference is
     * that two Points with equal lat/lon (to a certain precision) will be
     * considered as equals Points, even though their id can differ.
     *
     * @param obj Another object to compare this InitialPoint to.
     * @return true if the objects have the same id or have a similar lat/lon
     *
     * @see Point
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        InitialPoint other = (InitialPoint) obj;
        if (id == other.id)
            return true;
        if (Math.abs(other.latitude - latitude) <= 0.0000001 &&
            Math.abs(other.longitude - longitude) <= 0.0000001)
            return true;
        return false;
    }

    /**
     * @return hashCode implementation of the Point class.
     */
    @Override
    public int hashCode() {
        return super.hashCode();
    }

    /**
     * @return A String representation of this cluster, with some added statistics.
     */
    @Override
    public String toString() {
        return "<InitialPoint> " + id + " ["+latitude+","+longitude+"]";
    }
}