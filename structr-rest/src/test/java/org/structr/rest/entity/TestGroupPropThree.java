/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.rest.entity;

import org.structr.common.PropertyView;
import org.structr.common.RelType;
import org.structr.common.View;
import org.structr.core.entity.AbstractNode;
import org.structr.core.property.EntityProperty;
import org.structr.core.property.GroupProperty;
import org.structr.core.property.IntProperty;
import org.structr.core.property.StringProperty;

/**
 *
 * @author alex
 */
public class TestGroupPropThree extends AbstractNode{
	
	public static final GroupProperty gP = new GroupProperty("gP", TestGroupPropThree.class, 
			new StringProperty("sP"),
			new IntProperty("iP"), 
			new EntityProperty<TestGroupPropThree>("gpNode", TestGroupPropThree.class, RelType.OWNS,false));
	
	public static final GroupProperty ggP = 
			new GroupProperty("ggP", TestGroupPropThree.class, 
				new GroupProperty("igP", TestGroupPropThree.class, 
					new EntityProperty<TestGroupPropThree>("gpNode", TestGroupPropThree.class, RelType.OWNS,false),
					new StringProperty("isP")));
	
	public static final View defaultView = new View(TestGroupPropThree.class, PropertyView.Public,name,gP,ggP );
		
}
