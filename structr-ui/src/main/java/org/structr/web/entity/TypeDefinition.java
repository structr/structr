/*
 *  Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
 *
 *  This file is part of structr <http://structr.org>.
 *
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 *
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.structr.web.entity;

import java.util.logging.Logger;
import org.neo4j.graphdb.Direction;
import org.structr.core.property.Property;
import org.structr.common.PropertyView;
import org.structr.common.RelType;
import org.structr.core.property.StringProperty;
import org.structr.core.entity.AbstractNode;
import org.structr.core.property.CollectionProperty;

/**
 *
 * @author Christian Morgner
 */
public class TypeDefinition extends AbstractNode {
	
	private static final Logger logger = Logger.getLogger(TypeDefinition.class.getName());
	
	public static final Property<String>            validationExpression   = new StringProperty("validationExpression");
	public static final Property<String>            validationErrorMessage = new StringProperty("validationErrorMessage");
	public static final Property<String>            converter              = new StringProperty("converter");
	public static final Property<String>            converterDefaultValue  = new StringProperty("converterDefaultValue");

	public static final CollectionProperty<Content> contents               = new CollectionProperty<Content>("contents", Content.class, RelType.IS_A, Direction.INCOMING, false);
	
	public static final org.structr.common.View publicView = new org.structr.common.View(TypeDefinition.class, PropertyView.Public,
	    type, validationExpression, validationErrorMessage, converter, converterDefaultValue
	);
}
