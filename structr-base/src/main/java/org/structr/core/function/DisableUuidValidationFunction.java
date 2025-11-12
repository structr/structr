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

/**
 *
 */
public class DisableUuidValidationFunction extends AdvancedScriptingFunction {

	@Override
	public String getName() {
		return "disable_uuid_validation";
	}

	@Override
	public List<Signature> getSignatures() {
		// empty signature, no parameters
		return Signature.forAllLanguages("");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		ctx.getSecurityContext().disableUuidValidation(true);

		return "";
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${disable_uuid_validation()}"),
			Usage.javaScript("Usage: ${Structr.disableUuidValidation()}")
		);
	}

	@Override
	public List<String> getNotes() {
		return List.of(
			"This is a performance optimization for large imports, use at your own risk!"
		);
	}

	@Override
	public String getShortDescription() {
		return "Disables the validation of user-supplied UUIDs when creating objects.";
	}

	@Override
	public String getLongDescription() {
		return "";
	}
}
