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
package org.structr.core.function;

import org.structr.common.AccessControllable;
import org.structr.common.AccessEntry;
import org.structr.common.Permission;
import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.docs.ontology.FunctionCategory;
import org.structr.schema.action.ActionContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GetDirectAccessEntriesFunction extends AdvancedScriptingFunction {

	@Override
	public String getName() {
		return "getDirectAccessEntries";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("node");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasLengthAndAllElementsNotNull(sources, 1);

			if (!(sources[0] instanceof NodeInterface)) {

				logParameterError(caller, sources, "Expected node as first argument!", ctx.isJavaScriptContext());
				return Collections.emptyList();
			}

			final NodeInterface node = (NodeInterface) sources[0];
			final List<AccessEntry> entries = node.as(AccessControllable.class).getDirectAccessEntries();

			return toScriptValue(entries);

		} catch (ArgumentNullException pe) {

			// silently ignore null arguments

		} catch (ArgumentCountException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());
		}

		return Collections.emptyList();
	}

	static List<Map<String, Object>> toScriptValue(final List<AccessEntry> entries) {

		final List<Map<String, Object>> result = new ArrayList<>(entries.size());

		for (final AccessEntry entry : entries) {

			final List<String> permissionNames = new ArrayList<>(entry.permissions().size());
			for (final Permission p : entry.permissions()) {
				permissionNames.add(p.name());
			}

			final Map<String, Object> row = new LinkedHashMap<>();
			row.put("grantee",     entry.grantee());
			row.put("granteeName", entry.granteeName());
			row.put("granteeType", entry.granteeType());
			row.put("permissions", permissionNames);
			row.put("via",         entry.via());

			result.add(row);
		}

		return result;
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${getDirectAccessEntries(node)}. Example: ${getDirectAccessEntries(this)}"),
			Usage.javaScript("Usage: ${{ $.getDirectAccessEntries(node) }}. Example: ${{ $.getDirectAccessEntries($.this) }}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Returns the direct access entries (owner and direct SECURITY relationships) on the given node.";
	}

	@Override
	public String getLongDescription() {
		return """
		Returns a list of access entries covering tiers 1 and 2 of the permission model: the owner
		(if any) and principals connected to the node via a direct SECURITY relationship.
		Each entry is an object with fields `grantee` (UUID), `granteeName`, `granteeType` (`User` or `Group`),
		`permissions` (array of `read`, `write`, `delete`, `accessControl`) and `via` (provenance string:
		`owner`, `direct`, or a `+`-joined composite when a principal contributes via multiple paths).
		Group membership and schema grants are NOT included - use `getEffectiveAccessEntries()` for those.
		See also `getEffectiveAccessEntries()`, `grant()`, `revoke()` and `isAllowed()`.""";
	}

	@Override
	public List<Example> getExamples() {
		return List.of(
			Example.structrScript("${getDirectAccessEntries(this)}"),
			Example.javaScript("${{ $.getDirectAccessEntries($.this) }}")
		);
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(
			Parameter.mandatory("node", "node to inspect")
		);
	}

	@Override
	public FunctionCategory getCategory() {
		return FunctionCategory.AccessControl;
	}
}
