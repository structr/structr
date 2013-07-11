/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.rest.entity;

import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.core.entity.AbstractNode;
import org.structr.core.property.BooleanProperty;
import org.structr.core.property.DoubleProperty;
import org.structr.core.property.EnumProperty;
import org.structr.core.property.GroupProperty;
import org.structr.core.property.IntProperty;
import org.structr.core.property.LongProperty;
import org.structr.core.property.StringProperty;

/**
 *
 * @author alex
 */
public class TestGroupPropTwo extends AbstractNode{
	
	public static final GroupProperty gP1 = new GroupProperty("gP1", TestGroupPropTwo.class, new StringProperty("sP"),new IntProperty("iP"), new LongProperty("lP"), new DoubleProperty("dblP"), new BooleanProperty("bP"));
	
	public static final GroupProperty gP2 = new GroupProperty("gP2", TestGroupPropTwo.class, new EnumProperty<Counter>("eP",Counter.class));
	
	
	public static final View defaultView = new View(TestGroupPropTwo.class, PropertyView.Public,name,gP1,gP2);
		

	public enum Counter {

		one,two,three
	}
}


