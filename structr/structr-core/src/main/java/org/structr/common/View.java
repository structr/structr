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

import org.structr.core.property.Property;
import org.structr.core.GraphObject;

/**
 *
 * @author Christian Morgner
 */
public class View {

	private Property[] properties = null;
	private String name = null;

	public View(Class<? extends GraphObject> type, String name, Property... keys) {

		this.properties = keys;
		this.name = name;
	}

	public Property[] properties() {
		return properties;
	}
	
	public String name() {
		return name;
	}
}
