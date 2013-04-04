package be.ugent.intec.ugent.ibcn.geo.common;

import be.ugent.intec.ibcn.geo.common.datatypes.Coordinate;
import be.ugent.intec.ibcn.geo.common.datatypes.DataItem;
import be.ugent.intec.ibcn.geo.common.datatypes.GeoClass;
import be.ugent.intec.ibcn.geo.common.datatypes.Point;
import edu.wlu.cs.levy.CG.KDTree;
import edu.wlu.cs.levy.CG.KeyDuplicateException;
import edu.wlu.cs.levy.CG.KeySizeException;
import java.util.ArrayList;
import java.util.List;

/**
 * Object capable of on the fly nearest-neighbour assignments to the medoids
 * discovered by a clustering algorithm.
 * 
 * The algorithm makes use of a KD-tree for retrieving the nearest neighbours. 
 * To download the implementation of this data structure
 *  @see http://home.wlu.edu/~levys/software/kd/

 * @author Olivier Van Laere <oliviervanlaere@gmail.com>
 */
public class ClassMapper {
    
    /**
     * The KD-tree used for NN-retrieval
     */
    private KDTree<Integer> kd = new KDTree<Integer>(3);
    
    /**
     * List of known classes.
     */
    private List<GeoClass> classes = new ArrayList<GeoClass>();
    
    /**
     * @return the list of known classes
     */
    public List<GeoClass> getClasses() {
        return this.classes;
    }
    
    /**
     * Get a specific GeoClass by ID of the class.
     * @param classId the ID of the class to retrieve
     * @return The GeoClass object with the given ID
     */
    public GeoClass getClass(int classId) {
        return this.classes.get(classId);
    }
    
    /**
     * Constructor.
     * @param medoids List of medoids to construct the Mapper with.
     */
    public ClassMapper(List<Point> medoids) {
        // Init the remapped IDs (to facilitate indexing/retrieval)
        int remappedId = 0;
        // For each of the medoids
        for (Point p : medoids) {
            // Store the medoid with a remapped ID in the List
            classes.add(new GeoClass(remappedId, p.getId()));
            try {
                // Insert the medoid into the KD tree with its new ID
                kd.insert(new Coordinate(p).doubleKey(), remappedId);
            } catch (KeySizeException ex) {
                System.err.println("Error: " + ex.getMessage());
            } catch (KeyDuplicateException ex) {
                System.err.println("Error: " + ex.getMessage());
            }
            // Increment the remapped ID counter
            remappedId++;
        }
    }
    
    /**
     * Finds the ID of the nearest class for the given Point.
     * @param p A Point object
     * @return the ID of the GeoClass that 'contains' the given Point or -1 if
     * no class was found.
     */
    public int findClassId(Point p) {
        // Sanity check
        if (p != null) {
            try {
                // Fetch the nearest neighbours from the KD tree based on the
                // Point lat/lon
                List<Integer> neighbours = kd.nearest(
                        new Coordinate(p).doubleKey(), 1);
                // Fetch the remapped ID of the GeoClass that is the best match
                int classId = 0;
                try {
                    classId = neighbours.get(0);
                } catch (java.lang.ArrayIndexOutOfBoundsException e) {
                    System.err.println("Uh oh...");
                }
                // Return the GeoClass from the list by direct indexing
                return classId;
            } catch (KeySizeException ex) {
                System.err.println("Error: " + ex.getMessage());
            }
        }
        // Return null if not found
        return -1;
    }
    
    /**
     * Finds the nearest class for the given Point.
     * @param p A Point object
     * @return the actual GeoClass that 'contains' the given Point or null if
     * no class was found.
     */
    public GeoClass findClass(Point p) {
        // Re-use the findClassId method
        int classId = findClassId(p);
        if (classId == -1)
            // Return -1 if not found
            return null;
        else
            return this.classes.get(classId);
    }
    
    /**
     * Method used for attaching all the actual data items to the GeoClasses.
     * @param data The actual training data.
     */
    public void attachElements(DataItem [] data) {
        System.out.println("Fully attaching training data to GeoClasses...");
        long t1 = System.currentTimeMillis();
        for (DataItem item : data) {
            // Sanity check
            if (item != null) {
                // On the fly associate the clustering
                GeoClass myClass = findClass(item);
                // Add this ID to the class
                myClass.addElement(item.getId());
            }
        }
        long t2 = System.currentTimeMillis();
        System.out.println("Finished. Took "+(t2-t1)+" ms.");
    }
}