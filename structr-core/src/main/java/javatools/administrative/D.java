package javatools.administrative;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import javatools.parsers.Char;

/** 
 This class is part of the Java Tools (see http://mpii.de/yago-naga/javatools).
 It is licensed under the Creative Commons Attribution License 
 (see http://creativecommons.org/licenses/by/3.0) by 
 the YAGO-NAGA team (see http://mpii.de/yago-naga).
 
 
 

 
 This class provides convenience methods for Input/Output.
 Allows to do basic I/O with easy procedure calls
 -- nearly like in normal programming languages.
 Furthermore, the class provides basic set operations for EnumSets, NULL-safe
 comparisons and adding to maps.<BR>
 Example:
 <PRE>
 D.p("This is an easy way to write a string");
 // And this is an easy way to read one:
 String s=D.r();
 
 // Here is a cool way to print something inline
 computeProduct(factor1,(Integer)D.p(factor2));
 
 // Here are some tricks with enums
 enum T {a,b,c};
 EnumSet&lt;T> i=D.intersection(EnumSet.of(T.a,T.b),EnumSet.of(T.b,T.c));
 EnumSet&lt;T> u=D.union(EnumSet.of(T.a,T.b),EnumSet.of(T.b,T.c));
 
 // Here is how to compare things, even if they are NULL
 D.compare(object1, object2);
 
 // Here is how to add something to maps that contain lists
 Map&lt;String,List&lt;String>> string2list=new TreeMap&lt;String,List&lt;String>>();
 D.addKeyValue(string2list,"key","new list element",ArrayList.class); 
 // now, the map contains "key" -> [ "new list element" ]
 D.addKeyValue(string2list,"key","again a new list element",ArrayList.class);
 // now, the map contains "key" -> [ "new list element", "again a new list element" ]  

 // Here is how to add something to maps that contain integers
 Map&lt;String,Integer> string2list=new TreeMap&lt;String,Integer>();
 D.addKeyValue(string2list,"key",7); // map now contains "key" -> 7
 D.addKeyValue(string2list,"key",3); // map now contains "key" -> 10

 </PRE>  
 */
public class D {

  /** Indentation margin. All methods indent their output by indent spaces */
  public static int indent = 0;

  /** Prints <indent> spaces */
  protected static void i() {
    for (int i = 0; i < indent; i++)
      System.out.print(" ");
  }

  /** Prints some Objects, returns them */
  public static Object p(Object... a) {
    pl(a);
    System.out.println("");
    if (a == null || a.length == 0) return (null);
    if (a.length == 1) return (a[0]);
    return (a);
  }

  /** Prints some Objects */
  public static Object println(Object... a) {
    return (p(a));
  }

  /** Prints some Objects on one line */
  public static void pl(Object... a) {    
    System.out.print(toString(a));
  }

  /** Prints an array of integers*/
  public static int[] p(int[] a) {
    i();
    if (a == null) System.out.print("null-array");
    else for (int i = 0; i < a.length; i++)
      System.out.print(a[i] + ", ");
    System.out.println("");
    return (a);
  }

  /** Prints an array of doubles*/
  public static double[] p(double[] a) {
    i();
    if (a == null) System.out.print("null-array");
    else for (int i = 0; i < a.length; i++)
      System.out.print(a[i] + ", ");
    System.out.println("");
    return (a);
  }

  /** Reads a line from the keyboard */
  public static String r() {
    String s = "";
    i();
    try {
      s = new BufferedReader(new InputStreamReader(System.in)).readLine();
    } catch (Exception whocares) {
    }
    return (s);
  }

  /** Reads a line from the keyboard */
  public static String read() {
    return (r());
  }

  /** Reads a long from the keyboard */
  public static String read(String question) {
    System.out.print(question+" ");
    return (D.read());
  }

  /** Reads a long from the keyboard */
  public static boolean readBoolean(String question) {
    System.out.print(question+" ");
    return (D.read().startsWith("y"));
  }

  /** Reads a long from the keyboard */
  public static long readLong(String question) {
    System.out.print(question);
    return (Long.parseLong(D.r()));
  }

  /** Reads a double from the keyboard */
  public static double readDouble(String question) {
    System.out.print(question);
    return (Double.parseDouble(D.r()));
  }

  /** Waits for a number of milliseconds */
  public static void waitMS(long milliseconds) {
    try {
      Thread.sleep(milliseconds);
    } catch (InterruptedException ex) {
    }
  }

  /** Returns the intersection of two enumsets */
  public static <E extends Enum<E>> EnumSet<E> intersection(EnumSet<E> s1, EnumSet<E> s2) {
    // We have to clone, since retainAll modifies the set
    EnumSet<E> s = s1.clone();
    s.retainAll(s2);
    // I tried coding this for arbitrary sets, but it failed because
    // the interface Cloneable does not make sure that the clone-method
    // is visible (!)
    return (s);
  }

  /** Returns the union of two enumsets */
  public static <E extends Enum<E>> EnumSet<E> union(EnumSet<E> s1, EnumSet<E> s2) {
    EnumSet<E> s = s1.clone();
    s.addAll(s2);
    return (s);
  }

  /** Tells whether the intersection is non-empty */
  public static <E extends Enum<E>> boolean containsOneOf(EnumSet<E> s1, EnumSet<E> s2) {
    return (!intersection(s1, s2).isEmpty());
  }

  /** Exits with error code 0 */
  public static void exit() {
    System.exit(0);
  }

  /** Writes a line to a writer. Yes, this is possible */
  public static void writeln(Writer out, Object s) throws IOException {
    out.write(s.toString());
    out.write("\n");
  }

  /** Writes a line to a writer. Yes, this is possible */
  public static void writeln(OutputStream out, Object s) throws IOException {
    String string = Char.encodeUTF8(s.toString());
    for (int i = 0; i < string.length(); i++)
      out.write(string.charAt(i));
    out.write('\n');
  }

  /** Writes a line silently to a writer. */
  public static void silentWriteln(Writer out, Object s) {
    try {
      out.write(s.toString());
      out.write("\n");
    } catch (Exception e) {
    }
  }

  /** Executes a command */
  public static void execute(String cmd, File folder) throws Exception {
    Process p = Runtime.getRuntime().exec(cmd, null, folder);
    BufferedReader bri = new BufferedReader(new InputStreamReader(p.getInputStream()));
    BufferedReader bre = new BufferedReader(new InputStreamReader(p.getErrorStream()));
    String s1, s2 = null;
    while (null != (s1 = bri.readLine()) || null != (s2 = bre.readLine())) {
      if (s1 != null) System.out.println(s1);
      if (s2 != null) System.err.println(s2);
    }
    p.waitFor();
  }

  /** Given a map that maps to collections, adds a new key/value pair or introduces the key*/
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public static <K, V, C extends Collection<V>, L extends Collection> void addKeyValue(Map<K, C> map, K key, V value, Class<L> collectionType) {
    C coll = map.get(key);
    if (coll == null) {
      try {
        map.put(key, coll = (C) collectionType.newInstance());
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
    coll.add(value);
  }

  /** Given a map that maps to collections, adds a new key/value pair or introduces the key*/
  @SuppressWarnings({ "rawtypes" })
  public static <K, V, C extends Collection<V>, L extends Collection> void addKeyValues(Map<K, C> map, K key, C values, Class<L> collectionType) {
    for(V val : values) addKeyValue(map,key,val,collectionType);
  }
  
  /** Given a map that maps to integers, adds a new key/value pair or increases the counter*/
  public static <K> void addKeyValue(Map<K, Integer> map, K key, int value) {
    Integer coll = map.get(key);
    if (coll == null) {
      map.put(key, value);
      return;

    }
    map.put(key, coll + value);
  }

  /** Given a map that maps to floats, adds a new key/value pair or increases the counter*/
  public static <K> void addKeyValueFlt(Map<K, Float> map, K key, float value) {
    Float coll = map.get(key);
    if (coll == null) {
      map.put(key, value);
      return;
    }
    map.put(key, coll + value);    
  }
  
  /** Given a map that maps to doubles, adds a new key/value pair or increases the counter*/
  public static <K> void addKeyValueDbl(Map<K, Double> map, K key, double value) {
    Double coll = map.get(key);
    if (coll == null) {
      map.put(key, value);
      return;

    }
    map.put(key, coll + value);    
  }
  
  /** Given a map that maps to comparable objects, sets a key to a given value iff the current value is null or smaller than the given value*/
  public static <K ,V extends Comparable<V>> void setKeyValueIfGreaterThanCurrent(Map<K, V> map, K key, V value) {
    V coll = map.get(key);
    if (coll == null) {
      map.put(key, value);
      return;
    }
    if(coll.compareTo(value)<0)
      map.put(key, value);    
  }
  

  /** Returns the element of a map or 0*/
  public static <K> int getOrZero(Map<K, Integer> map, K key) {
    Integer i = map.get(key);
    if (i == null) return (0);
    return (i);
  }

  /** Returns the element of a map or 0*/
  public static <K> double getOrZeroDouble(Map<K, Double> map, K key) {
    Double i = map.get(key);
    if (i == null) return (0);
    return (i);
  }
  
  /** Returns the element of a map or a default value*/
  public static <K,V> V getOr(Map<K, V> map, K key, V defValue) {
    V i = map.get(key);
    if (i == null) return defValue;
    return (i);
  }

  /** Returns a sorted list of the items*/
  public static<T> List<T> sorted(final Map<T, Integer> map) {
    List<T> list=new ArrayList<T>(map.keySet());
    Collections.sort(list,new Comparator<T>(){

      @Override
      public int compare(T arg0, T arg1) {
        return (map.get(arg1).compareTo(map.get(arg0)));
      }});
    return(list);
  }
  
  /** Returns a sorted list of the items*/
  public static<T> List<T> sortedDouble(final Map<T, Double> map) {
    List<T> list=new ArrayList<T>(map.keySet());
    Collections.sort(list,new Comparator<T>(){

      @Override
      public int compare(T arg0, T arg1) {
        return (map.get(arg1).compareTo(map.get(arg0)));
      }});
    return(list);
  }
  
  /** Returns true if two things are equal, including NULL */
  public static <E> boolean equal(E s1, E s2) {
    if (s1 == s2) return (true);
    if (s1 == null) return (false);
    if (s2 == null) return (false);
    return (s1.equals(s2));
  }

  /** Compares two things, including NULL */
  public static <E extends Comparable<E>> int compare(E s1, E s2) {
    if (s1 == s2) return (0);
    if (s1 == null) return (-1);
    if (s2 == null) return (1);
    return (s1.compareTo(s2));
  }

  /** Compares pairs of comparable things (a1,a2,b1,b2,...), including NULL */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public static int comparePairs(Object... o) {
    for (int i = 0; i < o.length; i += 2) {
      int c = compare((Comparable) o[i], (Comparable) o[i + 1]);
      if (c != 0) return (c);
    }
    return (0);
  }

  /** Compares pairs of comparable things (a1,a2,b1,b2,...) for equality, including NULL */
  public static boolean equalPairs(Object... o) {
    for (int i = 0; i < o.length; i += 2) {
      if (!equal(o[i], o[i + 1])) return (false);
    }
    return (true);
  }

  /** Returns the index of a thing in an array or -1*/
  public static int indexOf(Object o, Object... os) {
    for (int i = 0; i < os.length; i++) {
      if (D.equal(os[i], o)) return (i);
    }
    return (-1);
  }

  /** TRUE if the first enum is before the second*/
  public static <C extends Enum<C>> boolean smaller(Enum<C> e1, Enum<C> e2) {
    return (e1.ordinal() < e2.ordinal());
  }

  /** Returns a reasonable String representation of a sequence of things. Handles arrays, deep arrays and NULL.*/
  public static String toString(Object... o) {
    if (o == null) {
      return ("null");
    }
    StringBuilder b = new StringBuilder();
    for (int i = 0; i < o.length; i++) {
      if (o[i] == null) {
        b.append("null");
        continue;
      }
      if (o[i].getClass().isArray()) {
        b.append("[");
        if (((Object[]) o[i]).length != 0) {
          for (Object obj : (Object[]) o[i]) {
            b.append(toString(obj)).append(", ");
          }
        }
        b.append("]");
      } else {
        b.append(o[i].toString());
      }
      if (i != o.length - 1) b.append(" ");
    }
    return (b.toString());
  }
  
  /** Picks one element from a set or NULL*/
  public static <T> T pick(Collection<T> set) {
    if(set==null || set.isEmpty()) return(null);
    return(set.iterator().next());
  }
  
  /** Returns the size of the intersection*/
  public static<T> int intersectionSize(Collection<T> c1, Collection<T> c2) {
	  if(c1.size()>c2.size()) return(intersectionSize(c2,c1));
	  int result=0;
	  for(T t : c1) if(c2.contains(t)) result++;
	  return(result);
  }
}
