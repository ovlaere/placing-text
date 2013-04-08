package be.ugent.intec.ibcn.analyzer;

import be.ugent.intec.ibcn.geo.common.datatypes.Coordinate;
import be.ugent.intec.ibcn.geo.common.datatypes.Point;
import edu.wlu.cs.levy.CG.KDTree;
import edu.wlu.cs.levy.CG.KeyDuplicateException;
import edu.wlu.cs.levy.CG.KeySizeException;
import java.util.List;

/**
 * TODO Comment.
 * 
 * This class allows for quick retrieval of the closest class given a certain
 * location.
 * 
 * @author Olivier Van Laere <oliviervanlaere@gmail.com>
 */
public class NearestMedoidFinder {

    private KDTree<Integer> kd = new KDTree<Integer>(3);

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
                //System.err.println("Error: " + ex.getMessage() + " " + key[0] + " " +key[1]);
                duplicates++;
            }
        }
        if (duplicates > 0)
            System.err.println("Warning: duplicate keys: " + duplicates);
    }
}