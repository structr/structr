/*
 * Copyright (C) 2010-2023 Structr GmbH
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
import org.structr.schema.action.ActionContext;

/**
 *
 */
public class DisableUuidValidationFunction extends AdvancedScriptingFunction {

	public static final String ERROR_MESSAGE_DISABLE_UUID_VALIDATION    = "Usage: ${disable_uuid_validation()}";
	public static final String ERROR_MESSAGE_DISABLE_UUID_VALIDATION_JS = "Usage: ${Structr.disableUuidValidation()}";

	@Override
	public String getName() {
		return "disable_uuid_validation";
	}

	@Override
	public String getSignature() {
		return null;
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		ctx.getSecurityContext().disableUuidValidation(true);

		return "";
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_DISABLE_UUID_VALIDATION_JS : ERROR_MESSAGE_DISABLE_UUID_VALIDATION);
	}

	@Override
	public String shortDescription() {
		return "Disables the validation of user-supplied UUIDs when creating objects. (Note: this is a performance optimization for large imports, use at your own risk!)";
	}

}
