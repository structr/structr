/**
 * Copyright (C) 2010-2016 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.rest.entity;

import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.core.entity.AbstractNode;
import org.structr.core.property.EndNode;
import org.structr.core.property.GroupProperty;
import org.structr.core.property.IntProperty;
import org.structr.core.property.StringProperty;

/**
 *
 *
 */
public class TestGroupPropThree extends AbstractNode{
	
	public static final GroupProperty gP = new GroupProperty("gP", TestGroupPropThree.class, 
			new StringProperty("sP"),
			new IntProperty("iP"), 
			new EndNode<>("gpNode", GroupThreeOneOneToOne.class));
	
	public static final GroupProperty ggP = 
			new GroupProperty("ggP", TestGroupPropThree.class, 
				new GroupProperty("igP", TestGroupPropThree.class, 
					new EndNode<>("gpNode", GroupThreeTwoOneToOne.class),
					new StringProperty("isP")));
	
	public static final View defaultView = new View(TestGroupPropThree.class, PropertyView.Public,name,gP,ggP );
		
}
