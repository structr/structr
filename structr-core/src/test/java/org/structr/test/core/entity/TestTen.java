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
package org.structr.test.core.entity;

import org.structr.core.entity.AbstractNode;
import org.structr.core.property.EndNode;
import org.structr.core.property.EndNodes;
import org.structr.core.property.Property;
import org.structr.core.property.StartNode;

/**
 * Test class for testing cascading delete with relationships
 * that reference the same type..
 *
 *
 */
public class TestTen extends AbstractNode {

	public static final Property<TestTen> tenTenParent             = new StartNode<>("testTenParent", TenTenOneToMany.class);
	public static final Property<Iterable<TestTen>> tenTenChildren = new EndNodes<>("testTenChildren", TenTenOneToMany.class);

	public static final Property<TestTen> testParent               = new StartNode<>("testParent", TenTenOneToOne.class);
	public static final Property<TestTen> testChild                = new EndNode<>("testChild", TenTenOneToOne.class);
}
