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
import org.structr.common.PropertyKey;
import org.structr.common.SecurityContext;

/**
 *
 * @author Christian Morgner
 */
public class Cache extends AbstractNode {

	public enum Key implements PropertyKey {
		sortKey, size
	}
	
	static {
		
	}

	
	private int size = 0;
	
	public List<AbstractNode> getCachedList(SecurityContext securityContext, int numResults) {
		
		return Collections.emptyList();
	}
	
	public void insertNode(AbstractNode node) {
		
	}
	
	public void removeNode(AbstractNode node) {
		
	}
	
	public int size() {
		return size;
	}
	
	@Override
	public void onNodeInstantiation() {
		Integer size = getIntProperty(Key.size);
		this.size = size != null ? size : 0;
	}
	
}
