package javatools.datatypes;
import java.util.TreeMap;

/** 
This class is part of the Java Tools (see http://mpii.de/yago-naga/javatools).
It is licensed under the Creative Commons Attribution License 
(see http://creativecommons.org/licenses/by/3.0) by 
the YAGO-NAGA team (see http://mpii.de/yago-naga).
  

  
 

Provides a nicer constructor for a TreeMap. 
Example:
<PRE>
   FinalMap<String,Integer> f=new FinalMap(
     "a",1,
     "b",2,
     "c",3);
   System.out.println(f.get("b"));
   --> 2
</PRE>
*/
public class FinalMap<T1 extends Comparable,T2> extends TreeMap<T1,T2>{
	private static final long serialVersionUID = 1L;

/** Constructs a FinalMap from an array that contains key/value sequences */  
  @SuppressWarnings("unchecked")
  public FinalMap(Object... a) {
    super();    
    for(int i=0;i<a.length-1;i+=2) {
      if(containsKey((T1)a[i])) throw new RuntimeException("Duplicate key in FinalMap: "+a[i]);
      put((T1)a[i],(T2)a[i+1]);
    }
  }
  
  /** Test routine */
  public static void main(String[] args) {
    FinalMap<String,Integer> f=new FinalMap<String,Integer>("a",1,"b",2);
    System.out.println(f.get("b"));
  }
}
