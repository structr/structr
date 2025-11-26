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

import org.structr.common.error.FrameworkException;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.schema.action.ActionContext;

import java.util.List;

public class HasErrorFunction extends AdvancedScriptingFunction {

	@Override
	public String getName() {
		return "has_error";
	}

	@Override
	public List<Signature> getSignatures() {
		// empty signature, no parameters
		return Signature.forAllScriptingLanguages("");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		return ctx.hasError();
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${has_error()}"),
			Usage.javaScript("Usage: ${{ $.has_error() }}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Allows checking if an error occurred in the scripting context.";
	}

	@Override
	public String getLongDescription() {
		return "";
	}
}