package be.ugent.intec.ibcn.analyzer;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.ugent.intec.ibcn.geo.common.datatypes.Coordinate;
import be.ugent.intec.ibcn.geo.common.datatypes.Point;
import edu.wlu.cs.levy.CG.KDTree;
import edu.wlu.cs.levy.CG.KeyDuplicateException;
import edu.wlu.cs.levy.CG.KeySizeException;

/**
 * This class allows for quick retrieval of the closest class given a certain
 * location.
 * 
 * @author Olivier Van Laere <oliviervanlaere@gmail.com>
 */
public class NearestMedoidFinder {
	
	/**
	 * Logger.
	 */
	protected static final Logger LOG = LoggerFactory.getLogger(NearestMedoidFinder.class);
    
	/**
     * KD-Tree for nearest medoid retrieval.
     */
    private KDTree<Integer> kd = new KDTree<Integer>(3);

    /**
     * Get the ID of the nearest neighbour given a certain Point.
     * @param p Point for which we seek the nearest medoid.
     * @return the ID of the medoid that is closest to the given Point or -1 if
     * none could be found.
     */
    public int getNearestMedoid(Point p) {
        List<Integer> nbrs = null;
        try {
            nbrs = kd.nearest(new Coordinate(p).doubleKey(), 1);
        } catch (KeySizeException e) {
            LOG.error("Error: {}", e.getMessage());
        }

        if (nbrs != null)
            return nbrs.get(0);
        else
            return -1;
    }

    /**
     * Constructor.
     * @param medoids List of Points that represent the medoids that should be
     * searchable for nearest neighbour.
     */
    public NearestMedoidFinder(List<Point> medoids) {
        int duplicates = 0;
        for (int classId = 0; classId < medoids.size(); classId++) {
            Point p = medoids.get(classId);
            try {
                kd.insert(new Coordinate(p).doubleKey(), classId);
            } catch (KeySizeException e) {
            	LOG.error("Error: {}", e.getMessage());
            } catch (KeyDuplicateException e) {
                duplicates++;
            }
        }
        if (duplicates > 0)
        	LOG.error("Warning: duplicate keys: {}", duplicates);
    }
}