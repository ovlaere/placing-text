package be.ugent.intec.ibcn.geo.common;

import be.ugent.intec.ibcn.geo.common.interfaces.LineParser;
import java.util.*;

/**
 * This class provides some generic helper methods.
 * 
 * @author Olivier Van Laere <oliviervanlaere@gmail.com>
 */
public class Util {

    /**
     * Helper method that sorts a Map by its values, not the keys.
     * @param map The Map that needs to be sorted by its values.
     * @return A new map that is sorted based on its values.
     */
    @SuppressWarnings({"unused", "unchecked"})
    public static Map sortByValueDescending(Map map) {
        List list = new LinkedList(map.entrySet());
        Collections.sort(list, new Comparator() {

            @Override
            public int compare(Object o1, Object o2) {
                return ((Comparable) ((Map.Entry) (o1)).getValue()).compareTo(((Map.Entry) (o2)).getValue()) * -1;
            }
        });
        Map result = new LinkedHashMap();
        for (Iterator it = list.iterator(); it.hasNext();) {
            Map.Entry entry = (Map.Entry) it.next();
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

    /**
     * Helper method that sorts a Map by its values, not the keys.
     * @param map The Map that needs to be sorted by its values.
     * @return A new map that is sorted based on its values.
     */
    @SuppressWarnings({"unused", "unchecked"})
    public static Map sortByValueAscending(Map map) {
        List list = new LinkedList(map.entrySet());
        Collections.sort(list, new Comparator() {

            @Override
            public int compare(Object o1, Object o2) {
                if (((Comparable) ((Map.Entry) (o1)).getValue()).compareTo(((Map.Entry) (o2)).getValue()) == 0)
                    return ((Comparable) ((Map.Entry) (o1)).getKey()).compareTo(((Map.Entry) (o2)).getKey());
                else
                    return ((Comparable) ((Map.Entry) (o1)).getValue()).compareTo(((Map.Entry) (o2)).getValue());
            }
        });
        Map result = new LinkedHashMap();
        for (Iterator it = list.iterator(); it.hasNext();) {
            Map.Entry entry = (Map.Entry) it.next();
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

    /**
     * Helper method that fills in parameters in a template string.
     * @param template The String template
     * @param params The parameters to replace the placeholders with
     * @return the adapted String
     */
    public static String applyTemplateValues(String template, String[] params) {
        for (int i = 0; i < params.length; i++) {
            if (params[i].length() > 0) {
                template = template.replace("@" + (i + 1), params[i]);
            } else {
                template = template.replace("@" + (i + 1), "");
            }
        }
        return template;
    }
    
    /**
     * Helper method that instantiates a input parser given a certain implementation.
     * @param parserClassName full package and class name of the parser implementation
     * to use.
     * @return An instance of the specified parser. 
     */
    public static LineParser getParser(String parserClassName) {
        // Prepare the parser
        LineParser parser = null;
        // Instantiate the parser
        try {
            parser = (LineParser) Class.forName(parserClassName).newInstance();
        } catch (InstantiationException ex) {
            System.err.println("InstantiationException " + ex.getMessage());
            System.exit(1);
        } catch (IllegalAccessException ex) {
            System.err.println("IllegalAccessException " + ex.getMessage());
            System.exit(1);
        } catch (ClassNotFoundException ex) {
            System.err.println("ClassNotFoundException " + ex.getMessage());
            System.exit(1);
        }
        // If parser is null
        if (parser == null) {
            throw new RuntimeException("Parser is null. This should not happen at this point!");
        }
        return parser;
    }
}