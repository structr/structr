/*
 * Copyright (C) 2010-2025 Structr GmbH
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
package org.structr.core.function;

import org.structr.api.graph.RelationshipType;
import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Group;
import org.structr.core.entity.Principal;
import org.structr.core.entity.SuperUser;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.StructrTraits;
import org.structr.docs.Signature;
import org.structr.schema.action.ActionContext;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class IsInGroupFunction extends AdvancedScriptingFunction {

	public static final String ERROR_MESSAGE    = "Usage: ${is_in_group(group, principal [, checkHierarchy = false ])}";
	public static final String ERROR_MESSAGE_JS = "Usage: ${{Structr.isInGroup(group, principal [, checkHierarchy = false ]);}}";

	@Override
	public String getName() {
		return "is_in_group";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllLanguages("group, user [, checkHierarchy = false ]");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasMinLengthAndMaxLengthAndAllElementsNotNull(sources, 2, 3);

			if (!(sources[0] instanceof NodeInterface g && g.is(StructrTraits.GROUP))) {

				logParameterError(caller, sources, "Expected node of type Group as first argument!", ctx.isJavaScriptContext());

			} else if (!(sources[1] instanceof NodeInterface p && p.is(StructrTraits.PRINCIPAL))) {

				logParameterError(caller, sources, "Expected node of type Principal as second argument!", ctx.isJavaScriptContext());

			} else if ((sources[1] instanceof SuperUser)) {

				logParameterError(caller, sources, "Expected node of type Principal as second argument - SuperUser can not be member of a group!", ctx.isJavaScriptContext());

			} else {

				boolean checkHierarchy = (sources.length > 2 && sources[2] instanceof Boolean) ? (boolean) sources[2] : false;

				final RelationshipType type = StructrApp.getInstance().getDatabaseService().getRelationshipType("CONTAINS");
				final Group group           = ((NodeInterface)sources[0]).as(Group.class);
				final Principal principal   = ((NodeInterface)sources[1]).as(Principal.class);

				return principalInGroup(new HashSet<>(), group, principal, type, checkHierarchy);
			}

		} catch (ArgumentNullException pe) {

			// silently ignore null arguments

		} catch (ArgumentCountException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());
		}

		return false;
	}

	private boolean principalInGroup (final Set<String> seenGroups, final Group group, final Principal principal, final RelationshipType relType, final boolean checkHierarchy) {

		boolean isInGroup = group.hasRelationshipTo(relType, principal);

		if (!isInGroup && checkHierarchy) {

			for (final Group principalGroup : principal.getParents()) {

				if (!isInGroup && !seenGroups.contains(principalGroup.getUuid())) {

					seenGroups.add(principalGroup.getUuid());

					isInGroup = principalInGroup(seenGroups, group, principalGroup, relType, checkHierarchy);
				}
			}
		}

		return isInGroup;
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_JS : ERROR_MESSAGE);
	}

	@Override
	public String getShortDescription() {
		return "Returns true if a user is in the given group. If the optional parameter checkHierarchy is set to false, only a direct group membership is checked. Otherwise the group hierarchy is checked.";
	}
}
