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

import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.docs.ontology.FunctionCategory;
import org.structr.schema.action.ActionContext;

import java.util.List;

public class UnlockReadonlyPropertiesFunction extends AdvancedScriptingFunction {

	@Override
	public String getName() {
		return "unlockReadonlyPropertiesOnce";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasLengthAndAllElementsNotNull(sources, 1);

			if (sources[0] instanceof NodeInterface n) {

				n.unlockReadOnlyPropertiesOnce();
				return "";

			} else {

				logger.warn("Parameter 1 is not a node. Parameters: {}", getParametersAsString(sources));
				return usage(ctx.isJavaScriptContext());
			}

		} catch (ArgumentNullException pe) {

			// silently ignore null arguments
			return null;

		} catch (ArgumentCountException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${unlockReadonlyPropertiesOnce(node)}. Example ${unlockReadonlyPropertiesOnce(this)}"),
			Usage.javaScript("Usage: ${{$.unlockReadonlyPropertiesOnce(node)}}. Example ${{$.unlockReadonlyPropertiesOnce($.this)}}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Unlocks any read-only property for a single access.";
	}

	@Override
	public String getLongDescription() {
		return "";
	}

	@Override
	public List<Signature> getSignatures() {
		// empty signature, no parameters
		return Signature.forAllScriptingLanguages("");
	}

	@Override
	public boolean isHidden() {
		return true;
	}

	@Override
	public FunctionCategory getCategory() {
		return FunctionCategory.System;
	}
}
