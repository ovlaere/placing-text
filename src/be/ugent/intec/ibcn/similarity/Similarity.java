package be.ugent.intec.ibcn.similarity;

import be.ugent.intec.ibcn.geo.common.datatypes.DataItem;
import java.util.*;

/**
 * Calculate the Jaccard Similarity.
 * 
 * For details on the Jaccard measure
 *  @see http://en.wikipedia.org/wiki/Jaccard_index
 *  @see http://www.sciencedirect.com/science/article/pii/S002002551300162X#s0140
 * 
 * @author Olivier Van Laere <oliviervanlaere@gmail.com>
 */
public class Similarity {

    /**
     * Calculate the jaccard Similarity between two items, loaded with their
     * features as Strings.
     * @param item1 DataItem 1, with String features loaded.
     * @param item2 DataItem 2, with String features loaded.
     * @return The jaccard similarity measure, based on their features.
     */
    public static double jaccard(DataItem item1, DataItem item2) {
        Set set1 = new HashSet(Arrays.asList(item1.getData()));
        Set set2 = new HashSet(Arrays.asList(item2.getData()));
        int alen = set1.size();
        int blen = set2.size();
        Set<Object> set = new HashSet<Object>(alen + blen);
        set.addAll(set1);
        set.addAll(set2);
        double overlap = alen +  blen - set.size();
        if(overlap <= 0)
            return 0.0;
        else
            return overlap / set.size();
    }
    
    /**
     * Given an array of DataItems, return a given number of items, sorted in 
     * descending order, that are most similar to the given DataItem 'item' with
     * respect to the Jaccard Similarity score calculated of the te String
     * features of the DataItems.
     * @param items Array of DataItems with String features, to select the most
     * similar items from
     * @param item The DataItem to find the most similar DataItems for
     * @param items_to_retain The number of most similar DataItems to retain.
     * @return A Sorted Set (descencing order of similarity scores) of the
     * 'items_to_retain' most similar DataItems to the given DataItem 'item'.
     */
    public static SortedSet<SimilarItem> jaccard(DataItem [] items, DataItem item, int items_to_retain) {
        // Prepare a sorted set
        SortedSet<SimilarItem> similar_items = new TreeSet<SimilarItem>();
        // For each of the items to consider
        for (int i = 0; i < items.length; i++) {
            // Sanity check
            if (items[i] != null) {
                // Create the SimilarItem
                SimilarItem simitem = new SimilarItem(items[i], Similarity.jaccard(item, items[i]));
                // Only add if this item will for sure be in the set
                if (similar_items.size() < items_to_retain || 
                        simitem.getScore() >= similar_items.last().getScore()) {
                    // If we are over the set size, remove the last one
                    if (similar_items.size() == items_to_retain)
                        similar_items.remove(similar_items.last());
                    similar_items.add(simitem);
                }
            }
        }
        return similar_items;
    }
}