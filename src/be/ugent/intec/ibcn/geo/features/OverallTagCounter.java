package be.ugent.intec.ibcn.geo.features;

import be.ugent.intec.ibcn.geo.common.datatypes.DataItem;
import be.ugent.intec.ibcn.geo.common.datatypes.GeoClass;
import be.ugent.intec.ugent.ibcn.geo.common.ClassMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class OverallTagCounter {

    private static final int NR_THREADS = Runtime.getRuntime().availableProcessors();
    
    private Map<Object, Integer> map;
    
    private DataItem [] data;
    
    public int getTagCount(Object tag) {
        return map.get(tag);
    }
    
    public OverallTagCounter(ClassMapper classmapper, DataItem [] data) {
        System.out.println("Generating overall tag count table");
        this.map = new HashMap<Object, Integer>();
        this.data = data;
        ExecutorService executor = Executors.newFixedThreadPool(NR_THREADS);
        List<Future<Map<Object, Integer>>> list = new ArrayList<Future<Map<Object, Integer>>>();
        for (GeoClass geoclass : classmapper.getClasses()) {
            Callable<Map<Object, Integer>> worker = new ClassTagCounter(geoclass);
            Future<Map<Object, Integer>> submit = executor.submit(worker);
            list.add(submit);
        }
        for (Future<Map<Object, Integer>> future : list) {
            try {
                Map<Object, Integer> result = future.get();
                for (Object key : result.keySet()) {
                    // If we already have a key
                    if (map.containsKey(key))
                        // +1
                        map.put(key, map.get(key)+result.get(key));
                    else
                        // Else start a new count
                        map.put(key, result.get(key));
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
        // This will make the executor accept no new threads
        // and finish all existing threads in the queue
        executor.shutdown();
        // Wait until all threads are finish
        while (!executor.isTerminated()) {}
        System.out.println("Done. Mapped tags: " + map.size());
    }
    
    public Map<Object, Integer> getClassTagCount(GeoClass geoclass, DataItem [] data){
        Map<Object, Integer> local_map = new HashMap<Object, Integer>();
        for (Integer id : geoclass.getElements()) {
            DataItem item = data[id-1];
            if (item != null && item.getData().length > 0) {
                // for all the tags of this item
                for (Object tag : item.getData()) {
                    // If we already have a key
                    if (local_map.containsKey(tag))
                        // +1
                        local_map.put(tag, local_map.get(tag)+1);
                    else
                        // Else start a new count
                        local_map.put(tag, 1);
                }
            }
        }
        return local_map;
    }

//    public static Map<Object, Map<Integer, Integer>> getTagAreaTagCount(List<NBClass> classes, DataItem [] data) {
//        System.out.println("Generating overall tag area tag count table");
//        Map<Object, Map<Integer, Integer>> map = new HashMap<Object, Map<Integer, Integer>>();
//        ExecutorService executor = Executors.newFixedThreadPool(NR_THREADS);
//        List<Future<Map<Object, Integer>>> list = new ArrayList<Future<Map<Object, Integer>>>();
//        for (NBClass nbclass : classes) {
//            Callable<Map<Object, Integer>> worker = new OverallTagCounterGenericHelper(nbclass, data);
//            Future<Map<Object, Integer>> submit = executor.submit(worker);
//            list.add(submit);
//        }
//        for (int i = 0; i < list.size(); i ++) {
//            Future<Map<Object, Integer>> future = list.get(i);
//            try {
//                Map<Object, Integer> result = future.get();
//                for (Object tag : result.keySet()) {
//                    Map<Integer, Integer> class_map = null;
//                    // If we already have a key
//                    if (map.containsKey(tag)) {
//                        class_map = map.get(tag);
//                    }
//                    else
//                        class_map = new HashMap<Integer, Integer>();
//                    
//                    class_map.put(i, result.get(tag));
//                    // Else start a new count
//                    map.put(tag, class_map);
//                }
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            } catch (ExecutionException e) {
//                e.printStackTrace();
//            }
//        }
//        // This will make the executor accept no new threads
//        // and finish all existing threads in the queue
//        executor.shutdown();
//        // Wait until all threads are finish
//        while (!executor.isTerminated()) {}
//        System.out.println("Done. Mapped tags: " + map.size());
//        return map;
//    }
    
    private class ClassTagCounter implements Callable<Map<Object, Integer>>{

        private GeoClass geoclass;

        public ClassTagCounter(GeoClass geoclass) {
            this.geoclass = geoclass;
        }

        @Override
        public Map<Object, Integer> call() throws Exception {
            return getClassTagCount(geoclass, data);
        }
    }
}