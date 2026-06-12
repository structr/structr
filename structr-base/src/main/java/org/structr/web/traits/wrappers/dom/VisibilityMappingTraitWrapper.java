/*
 * Copyright (C) 2010-2026 Structr GmbH
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
package org.structr.web.traits.wrappers.dom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.api.AbstractMethod;
import org.structr.core.api.Methods;
import org.structr.core.api.NamedArguments;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.Traits;
import org.structr.core.traits.wrappers.AbstractNodeTraitWrapper;
import org.structr.schema.action.ActionContext;
import org.structr.web.entity.dom.VisibilityMapping;

import java.util.HashMap;
import java.util.Map;

/**
 * Wrapper for VisibilityMapping nodes. The interesting state lives on the
 * concrete trait registered by feature modules (e.g. process engine);
 * structr-base only needs to know that "this node has an evaluate method
 * that decides whether the host DOMNode should render".
 */
public class VisibilityMappingTraitWrapper extends AbstractNodeTraitWrapper implements VisibilityMapping {

	private static final Logger logger = LoggerFactory.getLogger(VisibilityMappingTraitWrapper.class);

	public VisibilityMappingTraitWrapper(final Traits traits, final NodeInterface node) {
		super(traits, node);
	}

	@Override
	public boolean evaluate(final SecurityContext securityContext, final NodeInterface contextObject) {

		// Resolve the evaluate method registered by the concrete trait. If no
		// trait registered one, the mapping has no opinion: return false (a
		// mapping with no logic should not light up).
		final AbstractMethod method = Methods.resolveMethod(wrappedObject.getTraits(), "evaluate");
		if (method == null) {
			return false;
		}

		try {
			// HashMap (not Map.of) because contextObject can be null on pages
			// that don't render in a process-instance / task-instance scope.
			final Map<String, Object> args = new HashMap<>();
			args.put("contextObject", contextObject);
			final Object result = method.execute(
				new ActionContext(securityContext),
				wrappedObject,
				NamedArguments.fromMap(args)
			);
			return Boolean.TRUE.equals(result);

		} catch (FrameworkException ex) {
			logger.warn("VisibilityMapping evaluate() failed for '{}': {}", wrappedObject.getUuid(), ex.getMessage());
			return false;
		}
	}
}
