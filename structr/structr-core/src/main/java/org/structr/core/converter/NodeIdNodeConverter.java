/*
 *  Copyright (C) 2011 Axel Morgner
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

package org.structr.core.converter;

import org.structr.core.PropertyConverter;
import org.structr.core.Services;
import org.structr.core.Value;
import org.structr.core.entity.AbstractNode;
import org.structr.core.node.FindNodeCommand;

/**
 * Converts between nodeId and node
 *
 * @author Axel Morgner
 */
public class NodeIdNodeConverter extends PropertyConverter<Long, AbstractNode> {

	@Override
	public Long convertForSetter(AbstractNode node, Value value) {
		if (node == null) return null;
		return node.getId();
	}

	@Override
	public AbstractNode convertForGetter(Long nodeId, Value value) {
		if (nodeId == null) return null;
		return (AbstractNode) Services.command(securityContext, FindNodeCommand.class).execute(nodeId);
	}
}
