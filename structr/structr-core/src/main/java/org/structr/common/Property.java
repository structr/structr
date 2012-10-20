/*
 *  Copyright (C) 2012 Axel Morgner
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
package org.structr.common;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 *
 * @author Christian Morgner
 */
public class Property<T> implements PropertyKey<T> {

	private Set<String> views = new LinkedHashSet<String>();
	private String name       = null;
	
	public Property(String name) {
		this.name = name;
	}
	
	@Override
	public String name() {
		return name;
	}
	
	@Override
	public Object defaultValue() {
		return null;
	}
	
	@Override
	public int hashCode() {
		return name.hashCode();
	}
	
	@Override
	public boolean equals(Object o) {
		
		if (o instanceof Property) {
		
			return o.hashCode() == hashCode();
		}
		
		return false;
	}
	
	public void addToView(String name) {
		views.add(name);
	}

	public Set<String> getViews() {
		return views;
	}
}
