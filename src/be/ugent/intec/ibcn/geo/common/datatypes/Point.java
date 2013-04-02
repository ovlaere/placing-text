package be.ugent.intec.ibcn.geo.common.datatypes;

/**
 * This class represents a Point on the world.
 *
 * A point is presented by a latitude, longitude and an ID.
 *
 * @author Olivier Van Laere <oliviervanlaere@gmail.com>
 */
public class Point {

    public static final int DISTANCE_DEFAULT = 0;
    public static final int DISTANCE_EUCLIDIAN = 1;
    public static final int DISTANCE_CIRCLE = 2;
    public static final int DISTANCE_HAVERSINE = 3;

    /**
     * Variable keeping track of the default distance measure to be used.
     * By default, the circle distance is used.
     */
    protected int distanceMeasure = DISTANCE_CIRCLE;

    /**
     * Set the default function of measuring distances.
     * @param distanceMeasure ID of the distance function to be used.
     */
    public void setDistanceMeasure(int distanceMeasure) {
        this.distanceMeasure = distanceMeasure;
    }

    /**
     * Holds the latitude of this Point.
     */
    protected double latitude;

    /**
     * @return the latitude of this Point.
     */
    public double getLatitude() {
        return latitude;
    }
    
    /**
     * Sets the latitude value of this Point.
     * @param latitude The value to set the latitude to.
     */
    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    /**
     * Holds the longitude of this Point.
     */
    protected double longitude;

    /**
     * @return the longitude of this Point.
     */
    public double getLongitude() {
        return longitude;
    }

    /**
     * Sets the longitude value of this Point.
     * @param longitude The value to set the latitude to.
     */
    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
    
    /**
     * Holds the id of this Point.
     */
    protected int id;

    /**
     * Return the id of this Point.
     * @return
     */
    public int getId() {
        return id;
    }
    
    /**
     * Set the id of this Point.
     * @param id The id value to set.
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Constructor
     * @param id The id of this Point
     * @param latitude The latitude of this Point
     * @param longitude The longitude of this Point
     */
    public Point(int id, double latitude, double longitude) {
        this.id = id;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    /**
     * Copy constructor
     * @param p Another Point p to instantiate a copy from
     */
    public Point(Point p) {
        this(p.id, p.latitude, p.longitude);
    }

    /**
     * Constant storing the radius of the earth.
     */
    public static final double EARTH_R = 6371.0;

    /**
     * Constant storing the amount of kilometers that is represented by a
     * difference of a single degree of latitude around the equator.
     */
    public static final double DEGREE_LATITUDE_KM = 111.0;

    /**
     * Return the distance between this Point and another, based on the
     * configured distance function.
     */
    public double distance(Point q) {
        switch (this.distanceMeasure) {
            case DISTANCE_DEFAULT:
                return distanceDefault(q);
            case DISTANCE_EUCLIDIAN:
                return distanceEuclidian(q);
            case DISTANCE_CIRCLE:
                return distanceCircle(q);
            case DISTANCE_HAVERSINE:
                return distanceHaversine(q);
            default:
                return distanceDefault(q);
        }
    }

    /**
     * Return the default distance between this Point and the Point q.
     * @param q The Point q to which we want to calculate the distance
     * @return The distance between this Point and the Point q
     */
    public double distanceDefault(Point q) {
        double px = longitude * DEGREE_LATITUDE_KM * Math.cos(latitude*(Math.PI/180));
        double py = latitude * DEGREE_LATITUDE_KM;
        double qx = q.longitude * DEGREE_LATITUDE_KM * Math.cos(q.latitude*(Math.PI/180));
        double qy = q.latitude * DEGREE_LATITUDE_KM;
        return Math.sqrt(Math.pow((px - qx), 2) + Math.pow((py - qy), 2));
    }

    /**
     * Calculates the euclidian distance between this Point and the Point q.
     * @param q The Point q to calculate the distance to
     * @return The distance in kilometers between this Point and the Point q.
     */
    public double distanceEuclidian(Point q){
        double px = longitude;
        double py = latitude;
        double qx = q.longitude;
        double qy = q.latitude;
        // Return the Euclidean distance between p and q
        return Math.sqrt(Math.pow((px - qx), 2) + Math.pow((py - qy), 2));
    }

    /**
     * Calculates the circle distance between this Point and the Point q.
     * @param q The Point q to calculate the distance to
     * @return The distance in kilometers between this Point and the Point q.
     */
    public double distanceCircle(Point q) {
        double p_phi = latitude * Math.PI / 180.0;
        double p_lambda = longitude * Math.PI / 180.0;
        double q_phi = q.latitude * Math.PI / 180.0;
        double q_lambda = q.longitude * Math.PI / 180.0;

        double delta_lambda = q_lambda - p_lambda;
        double v1 = Math.cos(q_phi) * Math.sin(delta_lambda);
        double v2 = Math.cos(p_phi) * Math.sin(q_phi) -
                                Math.sin(p_phi) * Math.cos(q_phi) * Math.cos(delta_lambda);
        double v3 = Math.sin(p_phi) * Math.sin(q_phi) +
                                Math.cos(p_phi) * Math.cos(q_phi) * Math.cos(delta_lambda);
        double angularDifference = Math.atan2(Math.sqrt(v1*v1 + v2*v2), v3);
        return angularDifference * EARTH_R;
    }

    /**
     * Calculates the Haversine distance between this Point and the Point q.
     * @param q The Point q to calculate the distance to
     * @return The distance in kilometers between this Point and the Point q.
     */
    public double distanceHaversine(Point q) {
        double lat1 = latitude;
        double lat2 = q.latitude;
        double lon1 = longitude;
        double lon2 = q.longitude;
        double dLat = Math.toRadians(lat2-lat1);
        double dLon = Math.toRadians(lon2-lon1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
        Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
        Math.sin(dLon/2) * Math.sin(dLon/2);
        double c = 2 * Math.asin(Math.sqrt(a));
        return EARTH_R * c;
    }

    /**
     * Implementation of hashCode.
     */
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 13 * hash + this.id;
        return hash;
    }

    /**
     * Implementation of equals.
     * @return true if two points have the same id.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Point other = (Point) obj;
        if (id == other.id)
            return true;
        return false;
    }

    /**
     * Default toString() implementation.
     * @return
     */
    @Override
    public String toString() {
        return "<Point> " + id + " ["+latitude+","+longitude+"]";
    }
}