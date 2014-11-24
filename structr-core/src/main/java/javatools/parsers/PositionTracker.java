package javatools.parsers;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * This class is part of the Java Tools (see
 * http://mpii.de/yago-naga/javatools). It is licensed under the Creative
 * Commons Attribution License (see http://creativecommons.org/licenses/by/3.0)
 * by the YAGO-NAGA team (see http://mpii.de/yago-naga).
 * 
 * This class implements position change trackers that keep track of position
 * changes within a String, e.g. caused through normalization etc.
 * This allows for instance, given a position int the normalized string
 * to get the corresponding position in the original non-normalized string 
 * 
 *
 * 
 * backward position tracker - 
 * tracking several replacement/text changes allowing to trace a position in the modified 
 * text back to the corresp. position in the original text 
 * for the other direction see ForwardPositionTracker 
 * 
 * @author smetzger */
public class PositionTracker {
	
	
	private SortedMap<Integer,Integer>positionMap;
	private SortedMap<Integer,Integer>positionChanges;
	private SortedMap<Integer,Integer>old2NewMap;
	private int accumulatedModifier=0;
	  
	public PositionTracker(){
		positionMap=new TreeMap<Integer,Integer>();
		positionChanges=new TreeMap<Integer,Integer>();
		old2NewMap=new TreeMap<Integer,Integer>();		     
	}
	
	  
	public void addPositionChange(int pos, int modifier){
	      if(modifier!=0){	    	  
          int oldModifier=0;         
	    	  old2NewMap.put(pos, modifier);
          accumulatedModifier+=modifier;
          if(positionChanges.containsKey(pos+accumulatedModifier))
            oldModifier=positionChanges.get(pos+accumulatedModifier);
	    	  positionChanges.put(pos+accumulatedModifier,modifier*-1+oldModifier);	    	  
	      }		  
	}

	  
	  
	/** Closes the current changing run by Merging new position changes into the existing position change map
   *  after each round (one round=consecutive changes along the text) you need to call closeRun() before submitting more position changes from a new round,
   *  i.e. whenever you passed the string to be modified once call closeRun() before starting to run over the string again with more replacements
	 * Do this every time you ran once over the text making changes to be tracked*/ 
	public void closeRun() {
		if(positionChanges.isEmpty())
		  return;
	  		
		  
		SortedMap<Integer,Integer> temp=positionChanges;
		 
		//adapt old positions to new mapping
		while(!positionMap.isEmpty()){
		  Integer key=positionMap.firstKey();
	  	  Collection<Integer> modifiers=old2NewMap.headMap(key+1).values();
	  	  Integer newposition=key;    	
	  	  for(Iterator<Integer> it=modifiers.iterator(); it.hasNext(); newposition+=it.next()){}
	  	  Integer value=positionMap.get(key);
	  	  if(positionChanges.containsKey(newposition))
	  		  value+=positionChanges.get(newposition);
	  	  positionChanges.put(newposition, value);
	  	  positionMap.remove(key);
		}

		positionChanges=positionMap;
		positionMap=temp;
		old2NewMap.clear();
		accumulatedModifier=0;
		return;
	}
	  
	  
	  
	  
	/** Merges new position changes (given with the inversed old2new mapping) into the existing position change map*/ 
/*	private void addPositionMappings(SortedMap<Integer,Integer> newPosChanges, 
	   		SortedMap<Integer,Integer> old2NewMap) {
	  
		
		TreeMap<Integer,Integer> newMap=new TreeMap<Integer,Integer>();
		 
		//adapt old positions to new mapping
		while(!positionMap.isEmpty()){
		  Integer key=positionMap.firstKey();
	  	  Collection<Integer> modifiers=old2NewMap.headMap(key+1).values();
	  	  Integer newposition=key;    	
	  	  for(Iterator<Integer> it=modifiers.iterator(); it.hasNext(); newposition+=it.next()){}
	  	  Integer value=positionMap.get(key);
	  	  if(newMap.containsKey(newposition))
	  		  value+=newMap.get(newposition);
	  	  newMap.put(newposition, value);
	  	  positionMap.remove(key);
		}
		while(!newPosChanges.isEmpty()){
		  Integer key=newPosChanges.firstKey();
		  Integer value=newPosChanges.get(key);
		  if(newMap.containsKey(key))
		     value+=newMap.get(key);
		  newMap.put(key, value);
		  newPosChanges.remove(key);						
		}
		positionMap=newMap;
		old2NewMap.clear();
		return;
	}
	  */
	  
	public Integer translatePosition(Integer pos) {
		SortedMap<Integer,Integer> headMap=positionMap.headMap(pos+1);
		  Integer modifier=0;    	
		  for(Iterator<Integer> it=headMap.values().iterator(); it.hasNext(); modifier+=it.next()){}
/*		  if(headMap.size()>1){      TODO: Possible Optimization if we assume positions are asked in ascending order
			  headMap.clear();
			  posMap.put(pos, modifier);
		  }*/ 
		  return pos+modifier;		  
	}
  

    

  
  
  
  
  
  
  
  /** forward position change tracking - keeping track of several rounds of text modifications allowing to trace a position in the original
   *  text along the modifications to the corresp. position in the modified text 
   *  after each round (one round=consecutive changes along the text) you need to call closeRun() before submitting more position changes from a new round,
   *  i.e. whenever you passed the string to be modified once call closeRun() before starting to run over the string again with more replacements
   *  REMARK: NOT TESTED WITH MORE THAN ONE ROUND! may be ERRORNOUS with multiple rounds -> use with care (works with a single round though)
   * @author smetzger
   *
   */
  public static class ForwardPositionTracker {
    
    
    private SortedMap<Integer,Integer>positionMap;
    private SortedMap<Integer,Integer>positionChanges;
    //private SortedMap<Integer,Integer>new2OldMap;
    private PositionTracker new2OldTracker=null;
  //  private int accumulatedModifier=0;
      
    public ForwardPositionTracker(){
      positionMap=new TreeMap<Integer,Integer>();
      positionChanges=new TreeMap<Integer,Integer>();
     // new2OldMap=new TreeMap<Integer,Integer>();
      new2OldTracker=new PositionTracker();
     
    }
    
      
    public void addPositionChange(int pos, int modifier){
          if(modifier!=0){                                         
            positionChanges.put(pos,modifier);
  //          accumulatedModifier+=modifier;
            /*if(new2OldMap.containsKey(pos+accumulatedModifier))
              oldModifier=new2OldMap.get(pos+accumulatedModifier);
            new2OldMap.put(pos+accumulatedModifier, -1*modifier+oldModifier);                        
          }     */
            new2OldTracker.addPositionChange(pos, modifier);
          }
    }
    




      
      
    /** Closes the current changing run by Merging new position changes into the existing position change map
     * Do this every time you ran once over the text making changes to be tracked*/ 
    public void closeRun() {
      if(positionChanges.isEmpty())
        return;

      
      for(Map.Entry<Integer, Integer> change:positionChanges.entrySet()){
        Integer positionInOrigStream=new2OldTracker.translatePosition(change.getKey());
        if(positionMap.containsKey(positionInOrigStream))
          positionMap.put(positionInOrigStream, change.getValue()+positionMap.get(positionInOrigStream));
        else
          positionMap.put(positionInOrigStream, change.getValue());
      }
       
      positionChanges.clear();
//      accumulatedModifier=0;
      new2OldTracker.closeRun();
        
      return;
    }
      
    
      
    /** tells whether a position in the original stream has been cut away by some change operation, 
     *  such that translating it usually would make not to much sense
     *  @return true, iff the given position has been cut away, false otherwise (i.e. false if it should be mappable)
     *  TODO: current version ONLY WORKS SECURELY WHEN THERE IS ONLY ONE POSITION CHANGE RUN WITHOUT OVERLAPPING CHANGES!
     *  as soon as there are more than one change runs, or changes that overlap, we would need to check all following changes instead of only the next one */
    public boolean hasBeenCutAway(Integer pos){
        SortedMap<Integer,Integer> tailMap=positionMap.tailMap(pos+1);
        if(tailMap.isEmpty())
          return false;
        Integer key=tailMap.firstKey();
        Integer modifier=tailMap.get(key);
        if(modifier<0 && key+modifier<=pos )
          return true;
        else 
          return false;
        /* this does not work for the general case (had it the wrong way aroung), but can be used to implement it
        Integer key=null;
        Iterator<Integer> it=tailMap.keySet().iterator();
        while(it.hasNext()){
          key=it.next();
          Integer mod=tailMap.get(key);
          if(mod<0 && key-mod>=pos)
            return true;
        }  
        return false;*/
      }
      
    public Integer translatePosition(Integer pos) {
      SortedMap<Integer,Integer> headMap=positionMap.headMap(pos+1);
        Integer modifier=0;     
        for(Iterator<Integer> it=headMap.values().iterator(); it.hasNext(); modifier+=it.next()){}
  /*      if(headMap.size()>1){      Optimization if we assume positions are asked in ascending order
          headMap.clear();
          posMap.put(pos, modifier);
        }*/ 
        return pos+modifier;      
    }
    
    /** also handles positions inside text parts that have been cut out properly 
     * 
     *  TODO: current version ONLY WORKS SECURELY WHEN THERE IS ONLY ONE POSITION CHANGE RUN WITHOUT OVERLAPPING CHANGES!
     *  as soon as there are more than one change runs, or changes that overlap, we would need to check all following changes instead of only the next one */
    public Integer translatePositionExactly(Integer pos) {

      SortedMap<Integer,Integer> tailMap=positionMap.tailMap(pos+1);
      if(tailMap.isEmpty())
        return translatePosition(pos);
      else{
        Integer key=tailMap.firstKey();
        Integer modifier=tailMap.get(key);
        return translatePosition(Math.min(pos,key+modifier));
      }
      
/*        
 *        That version does it the wrong way around
 *        SortedMap<Integer,Integer> headMap=positionMap.headMap(pos+1);
          Integer modifier=0;     
          Integer key=null, value=null;
          Iterator<Integer> it=headMap.keySet().iterator();
          while(it.hasNext()){
            key=it.next();
        	  value=headMap.get(key);
        	  if(value<0)
        		  modifier+=Math.max(key-pos, value);
          }*/
    /*      if(headMap.size()>1){      Optimization if we assume positions are asked in ascending order
            headMap.clear();
            posMap.put(pos, modifier);
          }
          return pos+modifier;    */ 
            
      }

  }
  
  
}
