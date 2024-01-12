/*
 * Copyright (C) 2010-2024 Structr GmbH
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
public class DisableCascadingDeleteFunction extends AdvancedScriptingFunction {

	public static final String ERROR_MESSAGE_DISABLE_CASCADING_DELETE    = "Usage: ${disable_cascading_delete()}";
	public static final String ERROR_MESSAGE_DISABLE_CASCADING_DELETE_JS = "Usage: ${Structr.disableCascadingDelete()}";

	@Override
	public String getName() {
		return "disable_cascading_delete";
	}

	@Override
	public String getSignature() {
		return null;
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		ctx.getSecurityContext().setDoCascadingDelete(false);

		return "";
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_DISABLE_CASCADING_DELETE_JS : ERROR_MESSAGE_DISABLE_CASCADING_DELETE);
	}

	@Override
	public String shortDescription() {
		return "Disables cascading delete in the Structr Backend for the current transaction";
	}

}
