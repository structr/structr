/**
 * Copyright (C) 2010-2015 Morgner UG (haftungsbeschränkt)
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
package org.structr.core.entity;

import java.util.List;
import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.core.entity.relationship.SchemaPropertyLabel;
import org.structr.core.property.Property;
import org.structr.core.property.StartNodes;
import org.structr.core.property.StringProperty;

/**
 *
 * @author Christian Morgner
 */
public class SchemaLabel extends AbstractNode {

	public static final Property<List<SchemaProperty>> labeledProperties = new StartNodes<>("labeledProperties", SchemaPropertyLabel.class);
	public static final Property<String>               locale            = new StringProperty("locale");

	public static final View defaultView = new View(SchemaLabel.class, PropertyView.Public,
		labeledProperties, locale, name
	);

	public static final View uiView = new View(SchemaLabel.class, PropertyView.Ui,
		labeledProperties, locale, name
	);
}
