/**
 * Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
 *
 * This file is part of structr <http://structr.org>.
 *
 * structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with structr.  If not, see <http://www.gnu.org/licenses/>.
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
 * @author alex
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
