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
import org.structr.schema.SchemaHelper;
import org.structr.schema.action.ActionContext;

import java.util.List;

public class TypeInfoFunction extends AdvancedScriptingFunction {

	public static final String ERROR_MESSAGE_TYPE_INFO    = "Usage: ${type_info(type[, view])}. Example ${type_info('User', 'public')}";
	public static final String ERROR_MESSAGE_TYPE_INFO_JS = "Usage: ${$.typeInfo(type[, view])}. Example ${$.typeInfo('User', 'public')}";

	@Override
	public String getName() {
		return "type_info";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllLanguages("type [, view]");
	}

	@Override
	public Object apply(ActionContext ctx, Object caller, Object[] sources) throws FrameworkException {

		try {

			assertArrayHasMinLengthAndMaxLengthAndAllElementsNotNull(sources, 1, 2);

			final String typeName = sources[0].toString();
			final String viewName = (sources.length == 2 ? sources[1].toString() : null);

			return SchemaHelper.getSchemaTypeInfo(ctx.getSecurityContext(), typeName, viewName);

		} catch (IllegalArgumentException e) {

			logParameterError(caller, sources, e.getMessage(), ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_TYPE_INFO_JS : ERROR_MESSAGE_TYPE_INFO);
	}

	@Override
	public String getShortDescription() {
		return "Returns the type information for the specified type";
	}
}
