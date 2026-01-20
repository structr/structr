/*
 * Copyright (C) 2010-2026 Structr GmbH
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
package org.structr.common;

import org.structr.core.GraphObject;
import org.structr.core.property.Property;
import org.structr.docs.Documentation;

/**
 * Defines a named set of {@link Property} keys for a given type. This class is
 * needed for the REST server to define inheritable "views" for an entity.
 */
@Documentation(name="View", shortDescription="A view is a named collection of attributes that can be accessed via REST and from within the scripting environment, controlling which attributes are included in REST interface output.")
public class View {

	private Property[] properties = null;
	private String name = null;

	/**
	 * Registers the given list of property keys for the given type, using
	 * the name parameter.
	 *
	 * @param type the type this view will be registered for
	 * @param name the name under which the view will be registered
	 * @param keys the list of property keys to register
	 */
	public View(Class<? extends GraphObject> type, String name, Property... keys) {

		this.properties = keys;
		this.name = name;
	}

	/**
	 * @return the property keys registered in this view
	 */
	public Property[] properties() {
		return properties;
	}

	/**
	 * @return the name of this view
	 */
	public String name() {
		return name;
	}
}
