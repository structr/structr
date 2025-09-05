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
package org.structr.web.function;

import org.structr.rest.auth.AuthHelper;
import org.structr.schema.action.ActionContext;

public class ConfirmationKeyFunction extends UiCommunityFunction {

	public static final String ERROR_MESSAGE_CONFIRMATION_KEY    = "Usage: ${confirmation_key()}. Example: ${confirmation_key()}";
	public static final String ERROR_MESSAGE_CONFIRMATION_KEY_JS = "Usage: ${{Structr.confirmation_key()}}. Example: ${{Structr.confirmation_key()}}";

	@Override
	public String getName() {
		return "confirmation_key";
	}

	@Override
	public String getSignature() {
		return null;
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) {

		return AuthHelper.getConfirmationKey();
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_CONFIRMATION_KEY_JS : ERROR_MESSAGE_CONFIRMATION_KEY);
	}

	@Override
	public String shortDescription() {
		return "Creates a confirmation key to use as a one-time token. Used for user confirmation or password reset.";
	}
}
