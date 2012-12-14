/*
 *  Copyright (C) 2010-2012 Axel Morgner, structr <structr@structr.org>
 * 
 *  This file is part of structr <http://structr.org>.
 * 
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.entity;

import java.util.Collections;
import java.util.List;
import org.structr.core.property.Property;
import org.structr.common.SecurityContext;
import org.structr.core.property.IntProperty;
import org.structr.core.property.StringProperty;

/**
 *
 * @author Christian Morgner
 */
public class Cache extends AbstractNode {

	public static final Property<String>  sortKey = new StringProperty("sortKey");
	public static final Property<Integer> size    = new IntProperty("size");
	
	private int internalSize = 0;
	
	public List<AbstractNode> getCachedList(SecurityContext securityContext, int numResults) {
		
		return Collections.emptyList();
	}
	
	public void insertNode(AbstractNode node) {
		
	}
	
	public void removeNode(AbstractNode node) {
		
	}
	
	public int size() {
		return internalSize;
	}
	
	@Override
	public void onNodeInstantiation() {
		Integer _size = getIntProperty(Cache.size);
		this.internalSize = _size != null ? _size : 0;
	}
	
}
