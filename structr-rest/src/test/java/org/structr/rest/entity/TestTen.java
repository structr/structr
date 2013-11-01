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

import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.core.entity.AbstractNode;
import org.structr.core.notion.PropertySetNotion;
import org.structr.core.property.EntityProperty;
import org.structr.rest.common.TestRestRelType;

/**
 *
 * @author Axel Morgner
 */
public class TestTen extends AbstractNode {

	public static final EntityProperty<TestSeven>     testSeven        = new EntityProperty<TestSeven>("testSeven", TestSeven.class, TestRestRelType.HAS, new PropertySetNotion(true, TestSeven.uuid, TestSeven.aString), true);
	
	public static final View defaultView = new View(TestTen.class, PropertyView.Public,
		name, testSeven
	);
	
}
