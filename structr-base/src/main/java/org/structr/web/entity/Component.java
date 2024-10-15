/*
 * Copyright (C) 2010-2024 Structr GmbH
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
package org.structr.web.entity;

import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.core.property.IntProperty;
import org.structr.core.property.Property;
import org.structr.core.property.StringProperty;
import org.structr.web.entity.dom.DOMElement;

/**
 * Represents a component. A component is an assembly of elements
 */
public class Component extends DOMElement {

	public static final Property<String> kindProperty      = new StringProperty("kind").partOfBuiltInSchema();
	public static final Property<Integer> positionProperty = new IntProperty("position").partOfBuiltInSchema();

	public static final View defaultView = new View(Component.class,PropertyView.Public,
		kindProperty
	);

	public static final View uiView = new View(Component.class,PropertyView.Ui,
		kindProperty
	);
}
