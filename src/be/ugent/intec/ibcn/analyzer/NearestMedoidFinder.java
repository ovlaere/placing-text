package be.ugent.intec.ibcn.analyzer;

import be.ugent.intec.ibcn.geo.common.datatypes.Coordinate;
import be.ugent.intec.ibcn.geo.common.datatypes.Point;
import edu.wlu.cs.levy.CG.KDTree;
import edu.wlu.cs.levy.CG.KeyDuplicateException;
import edu.wlu.cs.levy.CG.KeySizeException;
import java.util.List;

/**
 * This class allows for quick retrieval of the closest class given a certain
 * location.
 * 
 * @author Olivier Van Laere <oliviervanlaere@gmail.com>
 */
public class NearestMedoidFinder {

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
        } catch (KeySizeException ex) {
            System.err.println("Error: " + ex.getMessage());
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
            } catch (KeySizeException ex) {
                System.err.println("Error: " + ex.getMessage());
            } catch (KeyDuplicateException ex) {
                duplicates++;
            }
        }
        if (duplicates > 0)
            System.err.println("Warning: duplicate keys: " + duplicates);
    }
}