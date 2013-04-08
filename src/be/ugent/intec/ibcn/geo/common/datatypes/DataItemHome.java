package be.ugent.intec.ibcn.geo.common.datatypes;

/**
 * This class represents an extension of a regular DataItem. The extension
 * exists under the form of adding information from the home location of the
 * owner.
 * 
 * However, this class also provides and example of a way to extend the generic
 * DataItem class with information that is on file.
 * 
 * Also 
 * @see Point
 * @see DataItem
 * 
 * @author Olivier Van Laere <oliviervanlaere@gmail.com>
 */
public class DataItemHome extends DataItem {
    
    /**
     * Point datatype that is either null or the home location for this DataItem.
     */
    private Point homeLocation = null;
    
    /**
     * Set the home location of this DataItem.
     * @param p A point of lat/lon info to use as the home information.
     */
    public void setHomeLocation(Point p) {
        this.homeLocation = p;
    }
    
    /**
     * @return A Point datatype that is either null or the home location 
     * for this DataItem.
     */
    public Point getHomeLocation() {
        return this.homeLocation;
    }
        
    /**
     * Constructor.
     * @param id Id of the DataItem
     * @param lat Latitude
     * @param lon Longitude
     * @param data Data to associate with this DataItem.
     */
    public DataItemHome(int id, double lat, double lon, Object[] data) {
        super(id, lat, lon, data);
    }
}