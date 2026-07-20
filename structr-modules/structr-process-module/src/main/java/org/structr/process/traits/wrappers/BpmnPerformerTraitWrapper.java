/*
 * Copyright (C) 2010-2026 Structr GmbH
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
package org.structr.process.traits.wrappers;

import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.Traits;
import org.structr.core.traits.wrappers.AbstractNodeTraitWrapper;
import org.structr.process.entity.BpmnPerformer;
import org.structr.process.traits.definitions.BpmnPerformerTraitDefinition;

import java.util.ArrayList;
import java.util.List;

public class BpmnPerformerTraitWrapper extends AbstractNodeTraitWrapper implements BpmnPerformer {

	public BpmnPerformerTraitWrapper(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	@Override
	public String getKind() {
		return wrappedObject.getProperty(traits.key(BpmnPerformerTraitDefinition.KIND_PROPERTY));
	}

	@Override
	public String getExpression() {
		return wrappedObject.getProperty(traits.key(BpmnPerformerTraitDefinition.EXPRESSION_PROPERTY));
	}

	@Override
	public List<NodeInterface> getPrincipals() {
		final List<NodeInterface> out = new ArrayList<>();
		final Iterable<NodeInterface> principals = wrappedObject.getProperty(traits.key(BpmnPerformerTraitDefinition.PRINCIPALS_PROPERTY));
		if (principals != null) {
			for (final NodeInterface principal : principals) {
				out.add(principal);
			}
		}
		return out;
	}
}
