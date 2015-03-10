/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
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
package org.structr.core.entity.relationship;

import org.structr.core.entity.OneToMany;
import org.structr.core.entity.SchemaMethod;
import org.structr.core.entity.SchemaNode;

/**
 *
 * @author Christian Morgner
 */
public class SchemaNodeMethod extends OneToMany<SchemaNode, SchemaMethod> {

	@Override
	public Class<SchemaNode> getSourceType() {
		return SchemaNode.class;
	}

	@Override
	public Class<SchemaMethod> getTargetType() {
		return SchemaMethod.class;
	}

	@Override
	public String name() {
		return "HAS_METHOD";
	}
}