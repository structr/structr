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
package org.structr.core.entity;

import org.neo4j.graphdb.Direction;
import org.structr.common.RelType;
import org.structr.core.property.EntityProperty;

/**
 * A simple entity for the most basic tests.
 * 
 * This class doesn't have the not-null constraint on TestOne, so it should
 * not be deleted over relationships which are tagged with
 * DELETE_IF_CONSTRAINT_WOULD_BE_VIOLATED
 * 
 * 
 * @author Axel Morgner
 */
public class TestThree extends AbstractNode {
	
	public static final EntityProperty<TestOne> testOne = new EntityProperty<TestOne>("testOne", TestOne.class, RelType.IS_AT, Direction.INCOMING, false, Relation.DELETE_IF_CONSTRAINT_WOULD_BE_VIOLATED);
}
