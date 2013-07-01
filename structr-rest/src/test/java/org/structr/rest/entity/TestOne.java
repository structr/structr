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
package org.structr.rest.entity;

import java.util.Date;
import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.core.property.ISO8601DateProperty;
import org.structr.core.property.IntProperty;
import org.structr.core.property.LongProperty;
import org.structr.core.entity.AbstractNode;
import org.structr.core.property.Property;

/**
 * A simple entity for the most basic tests.
 * 
 * @author Axel Morgner
 */
public class TestOne extends AbstractNode {
	
	public static final Property<Integer> anInt = new IntProperty("anInt").indexed();
	public static final Property<Long> aLong    = new LongProperty("aLong").indexed();
	public static final Property<Date> aDate    = new ISO8601DateProperty("aDate").indexed();
	
	public static final View defaultView = new View(TestOne.class, PropertyView.Public,
		name, anInt, aLong, aDate
	);
}
