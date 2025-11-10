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

import org.structr.api.config.Settings;
import org.structr.common.error.FrameworkException;
import org.structr.docs.Signature;
import org.structr.schema.action.ActionContext;

import java.util.List;

public class IsValidUuidFunction extends CoreFunction {

	public static final String ERROR_MESSAGE_IS_VALID_UUID = "Usage: ${is_valid_uuid(string)}. Example: ${is_valid_uuid(retrieve('request_parameter_id'))}";

	@Override
	public String getName() {
		return "is_valid_uuid";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllLanguages("string");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		if (sources.length >= 1 && sources[0] instanceof String potentialUuid) {

			return Settings.isValidUuid(potentialUuid);
		}

		return false;
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_IS_VALID_UUID;
	}

	@Override
	public String getShortDescription() {
		return "Returns true if the given string is a valid UUID according to the configured UUID format. Returns false otherwise and if non-string arguments are given.";
	}
}
