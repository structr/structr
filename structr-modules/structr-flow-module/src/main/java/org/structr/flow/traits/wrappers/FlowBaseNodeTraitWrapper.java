/*
 * Copyright (C) 2010-2024 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.flow.traits.wrappers;

import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.Traits;
import org.structr.core.traits.wrappers.AbstractNodeTraitWrapper;
import org.structr.flow.impl.FlowBaseNode;

import java.util.Map;
import java.util.TreeMap;

/**
 */
public class FlowBaseNodeTraitWrapper extends AbstractNodeTraitWrapper implements FlowBaseNode {

	public FlowBaseNodeTraitWrapper(final Traits traits, final NodeInterface node) {
		super(traits, node);
	}

	@Override
	public Map<String, Object> exportData() {

		Map<String, Object> result = new TreeMap<>();

		result.put("id",                          wrappedObject.getUuid());
		result.put("type",                        wrappedObject.getClass().getSimpleName());
		result.put("visibleToPublicUsers",        wrappedObject.isVisibleToPublicUsers());
		result.put("visibleToAuthenticatedUsers", wrappedObject.isVisibleToAuthenticatedUsers());

		return result;
	}
}
