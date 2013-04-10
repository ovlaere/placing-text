package be.ugent.intec.ibcn.geo.common.datatypes;

/**
 * This class represents a Cartesian coordinate. In order to enable
 * a reliable nearest neighbour search with a KD-tree, it is necessary
 * to convert the latitude/longitude coordinates of the Point datatype
 * to its Cartesian equivalent.
 * 
 * Without this conversion, two points that are on each side of the dateline, 
 * e.g. lat 0, lon 179 and lat 0, lon -179 would be never be considered nearest
 * neighbours and therefore assigned to different clusters.
 * 
 * This class provides methods to construct a Coordinate, as well as methods to
 * convert back and forth to and from the Point datatype.
 * 
 * @see Point
 * 
 * @author Olivier Van Laere <oliviervanlaere@gmail.com>
 */
public class Coordinate {

    /**
     * Id of this Coordinate.
     */
    protected int id;

    /**
     * @return the Id of this Coordinate.
     */
    public int getId() {
        return id;
    }

    /**
     * Set the Id of this Coordinate to the value provided
     * @param id 
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * x-component
     */
    protected double x;

    /**
     * @return the x-component of this cartesian Coordinate
     */
    public double getX() {
        return x;
    }

    /**
     * Set the value for the x-component of this cartesian Coordinate
     * @param x 
     */
    public void setX(double x) {
        this.x = x;
    }

    /**
     * y-component
     */
    protected double y;

    /**
     * @return the y-component of this cartesian Coordinate
     */
    public double getY() {
        return y;
    }

    /**
     * Set the value for the y-component of this cartesian Coordinate
     * @param y 
     */
    public void setY(double y) {
        this.y = y;
    }

    /**
     * y-component
     */
    protected double z;

    /**
     * @return the z-component of this cartesian Coordinate
     */
    public double getZ() {
        return z;
    }

    /**
     * Set the value for the z-component of this cartesian Coordinate
     * @param z 
     */
    public void setZ(double z) {
        this.z = z;
    }

    /**
     * @return the components of this Coordinate as an array of double values
     */
    public double [] doubleKey() {
        return new double []{x, y, z};
    }

    /**
     * Constructor
     * @param id Id of this Coordinate
     * @param x value for the x-component
     * @param y value for the y-component
     * @param z value for the z-component
     */
    public Coordinate(int id, double x, double y, double z) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /**
     * Copy constructor.
     * @param c another Coordinate
     */
    public Coordinate(Coordinate c) {
        this(c.id, c.x, c.y, c.z);
    }

    /**
     * Copy constructor with specified id for the new Coordinate.
     * @param id Id to use for the new Coordinate
     * @param c another Coordinate
     */
    public Coordinate(int id, Coordinate c) {
        this(id, c.x, c.y, c.z);
    }
    
    /**
     * Constructor.
     * @param id Id to use for the new Coordinate
     * @param latitude Latitude to use for the new Coordinate
     * @param longitude Longitude to use for the new Coordinate
     * 
     * @see http://www.geomidpoint.com/calculation.html for details on the
     * conversion from lat/lon to x,y,z
     */
    public Coordinate(int id, double latitude, double longitude) {
        this.id = id;
        double lat = Math.toRadians(latitude);
        double lon = Math.toRadians(longitude);
        this.x = Point.EARTH_R * Math.cos(lat) * Math.cos(lon);
        this.y = Point.EARTH_R * Math.cos(lat) * Math.sin(lon);
        this.z = Point.EARTH_R * Math.sin(lat);
    }

    /**
     * Copy constructor - automated conversion from a Point.
     * @param p A Point to construct this Coordinate from.
     */
    public Coordinate(Point p) {
        this(p.getId(), p.getLatitude(), p.getLongitude());
    }

    /**
     * @return an automated conversion of this Coordinate to a Point.
     */
    public Point toPoint() {
        double longitude = Math.toDegrees(Math.atan2(y, x));
        double hyp = Math.sqrt(x*x + y*y);
        double latitude = Math.toDegrees(Math.atan2(z, hyp));
        return new Point(id, latitude, longitude);
    }

    public double distance(Coordinate q){
        // Return the Euclidean distance between p and q
        return Math.sqrt(Math.pow((x - q.x), 2) + Math.pow((y - q.y), 2) + 
                Math.pow((z - q.z), 2));
    }

    @Override
    public String toString() {
        return "{" + id + " " + x + ", " + y + ", " + z + "}";
    }
}