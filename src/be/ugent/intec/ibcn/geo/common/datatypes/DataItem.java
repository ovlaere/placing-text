package be.ugent.intec.ibcn.geo.common.datatypes;

import java.util.Arrays;

/**
 * This class represents a generic data item, which can come from any type of
 * dataset. The datatype in this class links location information (by means of
 * a Point) to the data, which is represented as an object array.
 * 
 * A common example of this would be for example a Flickr photo, which has a
 * latitude,longitude pair and some tags associated with it.
 * 
 * Also @see Point.
 * 
 * @author Olivier Van Laere <oliviervanlaere@gmail.com>
 */
public class DataItem extends Point{
    
    /**
     * Holds the data associated with this DataItem.
     */
    protected Object [] data;

    /**
     * @return the data associated with this DataItem.
     */
    public Object[] getData() {
        return data;
    }

    /**
     * Set the data associated with this object.
     * @param data An Object array of data to set.
     */
    public void setData(Object[] data) {
        this.data = data;
    }

    /**
     * Constructor.
     * @param id Id of the DataItem
     * @param lat Latitude
     * @param lon Longitude
     * @param data Data to associate with this DataItem.
     */
    public DataItem(int id, double lat, double lon, Object[] data) {
        super(id, lat, lon);
        this.data = data;
    }

    /**
     * @return a String representation of this DataItem.
     */
    @Override
    public String toString() {
        return "DataItem{" + "id=" + id + ", lat=" + latitude + ", lon=" 
                + longitude + ", data=" + Arrays.deepToString(data) + '}';
    }
}