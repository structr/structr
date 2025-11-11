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

import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.Group;
import org.structr.core.entity.Principal;
import org.structr.core.entity.SuperUser;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.StructrTraits;
import org.structr.docs.*;
import org.structr.schema.action.ActionContext;

import java.util.List;

public class AddToGroupFunction extends AdvancedScriptingFunction {

	@Override
	public String getName() {
		return "add_to_group";
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(
			Parameter.mandatory("group", "The group to add to"),
			Parameter.mandatory("principal", "The user or group to add to the given group")
		);
	}

	@Override
	public List<Example> getExamples() {
		return null;
	}

	@Override
	public List<String> getNotes() {
		return null;
	}

	@Override
	public List<Language> getLanguages() {
		return Language.all();
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllLanguages("group, user");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasLengthAndAllElementsNotNull(sources, 2);

			if (!(sources[0] instanceof NodeInterface n1 && n1.is(StructrTraits.GROUP))) {

				logParameterError(caller, sources, "Expected node of type Group as first argument!", ctx.isJavaScriptContext());

			} else if (!(sources[1] instanceof NodeInterface n2 && n2.is(StructrTraits.PRINCIPAL))) {

				logParameterError(caller, sources, "Expected node of type Principal as second argument!", ctx.isJavaScriptContext());

			} else if ((sources[1] instanceof SuperUser)) {

				logParameterError(caller, sources, "Expected node of type Principal as second argument - SuperUser can not be member of a group!", ctx.isJavaScriptContext());

			} else {

				final NodeInterface group = (NodeInterface)sources[0];
				final NodeInterface user  = (NodeInterface)sources[1];

				group.as(Group.class).addMember(ctx.getSecurityContext(), user.as(Principal.class));
			}

		} catch (ArgumentNullException pe) {

			// silently ignore null arguments

		} catch (ArgumentCountException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());
		}

		return "";
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${add_to_group(group, user)}"),
			Usage.javaScript("Usage: ${{$.addToGroup(group, user);}}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Adds a user to a group.";
	}

	@Override
	public String getLongDescription() {
		return "";
	}
}
