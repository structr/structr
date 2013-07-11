/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.rest.entity;

import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.core.entity.AbstractNode;
import org.structr.core.property.DateProperty;
import org.structr.core.property.DoubleProperty;
import org.structr.core.property.GroupProperty;
import org.structr.core.property.IntProperty;
import org.structr.core.property.StringProperty;

/**
 *
 * @author alex
 */
public class TestGroupPropOne extends AbstractNode{
	
	public static final GroupProperty gP1 = new GroupProperty("gP1", TestGroupPropOne.class, new StringProperty("sP"),new IntProperty("iP"));
	public static final GroupProperty gP2 = new GroupProperty("gP2", TestGroupPropOne.class, new DoubleProperty("dblP"),new DateProperty("dP","dd.MM.yyyy"));
	
	
	public static final View defaultView = new View(TestGroupPropOne.class, PropertyView.Public,name,gP1,gP2 );
		
		
}
