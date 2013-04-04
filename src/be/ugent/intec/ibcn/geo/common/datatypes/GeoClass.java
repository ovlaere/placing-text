package be.ugent.intec.ibcn.geo.common.datatypes;

import java.util.HashSet;
import java.util.Set;

/**
 * This class represents the concept of a cluster (/area/class) under the generic
 * name of GeoClass. Every training item can belong to one of the possible classes.
 * 
 * A class is represented by a medoid, which is normally determined by some
 * clustering algorithm. The medoid is identified by its unique ID in the training
 * data. This id is remapped on the fly by the ClassMapperOnline class. This
 * facilitates indexing and retrieval in array structures of the different classes
 * without loosing the link to the original medoid points in the training data.
 * 
 * @see be.ugent.intec.ugent.ibcn.geo.common.ClassMapperOnline
 * 
 * @author Olivier Van Laere <oliviervanlaere@gmail.com>
 */
public class GeoClass {

    /**
     * Original ID of the medoid point.
     */
    private int originalId = -1;
    
    /**
     * @return the original ID of the medoid (as on file).
     */
    public int getOriginalId() {
        return this.originalId;
    }
    
    /**
     * Actual ID of this GeoClass.
     */
    private int id;
    
    /**
     * @return the actual ID of this GeoClass. 
     */
    public int getId() {
        return this.id;
    }
    
    /**
     * Constructor.
     * @param id actual ID of this GeoClass.
     */
    public GeoClass(int id) {
        this.id = id;
    }
    
    /**
     * Constructor.
     * @param id actual ID of this GeoClass.
     * @param originalId original ID of the medoid point (as on file)
     */
    public GeoClass(int id, int originalId) {
        this(id);
        this.originalId = originalId;
    }
    
    /**
     * IDs of the elements in this class.
     */
    private Set<Integer> elements = new HashSet<Integer>();
    
    /**
     * 
     * @return the IDs of the elements in this class.
     */
    public Set<Integer> getElements() {
        return this.elements;
    }
    
    /**
     * Add an element this this class (by ID).
     * @param id ID of the element to add to this class.
     */
    public void addElement(Integer id) {
        this.elements.add(id);
    }
    
    /**
     * Return the size of this GeoClass.
     */
    public int size() {
        return this.elements.size();
    }
}