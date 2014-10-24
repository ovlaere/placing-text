package be.ugent.intec.ibcn.geo.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.ugent.intec.ibcn.geo.common.datatypes.Coordinate;
import be.ugent.intec.ibcn.geo.common.datatypes.DataItem;
import be.ugent.intec.ibcn.geo.common.datatypes.GeoClass;
import be.ugent.intec.ibcn.geo.common.datatypes.Point;
import edu.wlu.cs.levy.CG.KDTree;
import edu.wlu.cs.levy.CG.KeyDuplicateException;
import edu.wlu.cs.levy.CG.KeySizeException;

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
	 * Logger.
	 */
	protected static final Logger LOG = LoggerFactory.getLogger(ClassMapper.class);

    /**
     * Number of threads, for multi-threaded processing.
     */
    protected static final int NR_THREADS = 
            Runtime.getRuntime().availableProcessors();

    /**
     * The KD-tree used for NN-retrieval
     */
    private KDTree<Integer> kd = new KDTree<Integer>(3);
    
    /**
     * List of known classes.
     */
    private List<GeoClass> classes = new ArrayList<GeoClass>();
    
    /**
     * Holds the size of the classes list.
     */
    private int size = 0;
    
    /**
     * @return the number of classes currently used.
     */
    public int size() {
        return this.size;
    }
    
    /**
     * Holds the list of medoids used to construct this mapper.
     */
    private List<Point> medoids;
    
    /**
     * @return The List of Points that represents the medoids used for
     * clustering.
     */
    public List<Point> getMedoids() {
        return this.medoids;
    }
    
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
        this.medoids = medoids;
        this.kd = initTree(false);
        this.size = this.classes.size();
    }
    
    /**
     * @param ignoreClasses if set to true, the classes will be stored in the
     * classes list to track them. This is the expected behaviour unless we
     * are making copies of the KD tree for multi-threaded processing.
     * @return a KD tree with the medoids.
     */
    private KDTree<Integer> initTree(boolean ignoreClasses) {
        KDTree<Integer> localTree = new KDTree<Integer>(3);
        // Init the remapped IDs (to facilitate indexing/retrieval)
        int remappedId = 0;
        // For each of the medoids
        for (Point p : medoids) {
            // If the classes should be stored
            if (!ignoreClasses)
                // Store the medoid with a remapped ID in the List
                classes.add(new GeoClass(remappedId, p.getId()));
            try {
                // Insert the medoid into the KD tree with its new ID
                localTree.insert(new Coordinate(p).doubleKey(), remappedId);
            } catch (KeySizeException e) {
                LOG.error("Error: {}", e.getMessage());
            } catch (KeyDuplicateException e) {
                LOG.error("Error: {}", e.getMessage());
            }
            // Increment the remapped ID counter
            remappedId++;
        }
        return localTree;
    }
    
    /**
     * Finds the ID of the nearest class for the given Point.
     * @param p A Point object
     * @return the ID of the GeoClass that 'contains' the given Point or -1 if
     * no class was found.
     */
    public int findClassId(Point p) {
        return findClassId(p, this.kd);
    }
    
    /**
     * Finds the ID of the nearest class for the given Point.
     * @param p A Point object
     * @param the KD tree to use for lookup
     * @return the ID of the GeoClass that 'contains' the given Point or -1 if
     * no class was found.
     */
    private int findClassId(Point p, KDTree<Integer> kd) {
        int classId = -1;
        // Sanity check
        if (p != null) {
            try {
                // Fetch the nearest neighbours from the KD tree based on the
                // Point lat/lon
                List<Integer> neighbours = kd.nearest(
                        new Coordinate(p).doubleKey(), 1);
                // Fetch the remapped ID of the GeoClass that is the best match
                try {
                    classId = neighbours.get(0);
                } catch (java.lang.IndexOutOfBoundsException e) {
                    LOG.warn("Could not get a nearest neighbour, this should not happen?");
                }
                // Return the GeoClass from the list by direct indexing
                return classId;
            } catch (KeySizeException e) {
                LOG.error("Error: {}", e.getMessage());
            }
        }
        // Return null if not found
        return classId;
    }
    
    /**
     * Finds the nearest class for the given Point.
     * @param p A Point object
     * @return the actual GeoClass that 'contains' the given Point or null if
     * no class was found.
     */
    public GeoClass findClass(Point p) {
        return findClass(p, this.kd);
    }
    
    /**
     * Finds the nearest class for the given Point.
     * @param p A Point object
     * @param the KD tree to use for lookup
     * @return the actual GeoClass that 'contains' the given Point or null if
     * no class was found.
     */
    private GeoClass findClass(Point p, KDTree<Integer> kd) {
        // Re-use the findClassId method
        int classId = findClassId(p, kd);
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
        LOG.info("Fully attaching training data to GeoClasses...");
        long t1 = System.currentTimeMillis();
        // Prepare a thread pool
        ExecutorService executor = Executors.newFixedThreadPool(NR_THREADS);
        // Prepare a list for the futures
        List<Future<Map<Integer, Set<Integer>>>> list = 
                new ArrayList<Future<Map<Integer, Set<Integer>>>>();
        // Determine block length
        int length = (int) (data.length * 1.0 / NR_THREADS);
        for (int i = 0; i < NR_THREADS; i++) {
            int begin = i * length;
            if (i == NR_THREADS - 1) {
                length = data.length - (i * length);
            }
            int end = begin + length;
            // Instantiate a callable, track the future, execute
            Callable<Map<Integer, Set<Integer>>> worker = 
                    new AttachHelper(data, begin, end, initTree(true));
            Future<Map<Integer, Set<Integer>>> submit = executor.submit(worker);
            list.add(submit);
        }
        // Now retrieve the results form the futures
        for (Future<Map<Integer, Set<Integer>>> future : list) {
            try {
                // Fetch the local assignments
                Map<Integer, Set<Integer>> local_result = future.get();
                // Assign them for real to the correct GeoClass objects
                for (int classId : local_result.keySet()) {
                    classes.get(classId).addAll(local_result.get(classId));
                }
            }
            catch (Exception e) {
                LOG.error("Exception: {}", e.getMessage());
                System.exit(1);
            }
        }
        // This will make the executor accept no new threads
        // and finish all existing threads in the queue
        executor.shutdown();
        // Wait until all threads are finish
        while (!executor.isTerminated()) {}
        long t2 = System.currentTimeMillis();
        LOG.info("Finished. Took {} ms.", (t2-t1));
    }
    
    /**
     * Helper Callable for multi-threaded on the fly cluster association.
     */
    private class AttachHelper implements Callable<Map<Integer, Set<Integer>>> {

        /**
         * Reference to the training data.
         */
        private DataItem [] data;
        
        /**
         * Start index for processing.
         */
        private int begin;

        /**
         * End index for processing.
         */
        private int end;
        
        /**
         * Reference to the local KD tree.
         */
        private KDTree<Integer> tree;
        
        /**
         * 
         * @param data Training data
         * @param begin Start index for processing.
         * @param end End index for processing.
         * @param tree Local copy of the KD tree
         */
        public AttachHelper(DataItem [] data, int begin, int end, 
                KDTree<Integer> tree) {
            this.data = data;
            this.begin = begin;
            this.end = end;
            this.tree = tree;
        }
        
        /**
         * Actual cluster association.
         */
        @Override
        public Map<Integer, Set<Integer>> call() throws Exception {
            Map<Integer, Set<Integer>> assignments = 
                    new HashMap<Integer, Set<Integer>>();
            for (int i = begin; i < end; i++) {
                DataItem item = data[i];
                // Sanity check
                if (item != null) {
                    // On the fly associate the clustering, provide the tree
                    // to avoid locking on the shared tree
                    int classId = findClassId(item, tree);
                    // Fetch the set of elements for this GeoClass
                    Set<Integer> elements = assignments.get(classId);
                    // Init if not set is found
                    if (elements == null) {
                        elements = new HashSet<Integer>();
                        assignments.put(classId, elements);
                    }
                    // Add this element to the set
                    elements.add(item.getId());
                }
            }
            // Return the local results
            return assignments;
        }
    }
}