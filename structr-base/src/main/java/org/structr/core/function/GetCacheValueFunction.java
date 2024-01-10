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

import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.core.parser.CacheExpression;
import org.structr.schema.action.ActionContext;

public class GetCacheValueFunction extends CoreFunction {

	public static final String ERROR_MESSAGE_GET_CACHE_VALUE    = "Usage: ${get_cache_value(cacheKey)}. Example: ${get_cache_value('mykey')}";
	public static final String ERROR_MESSAGE_GET_CACHE_VALUE_JS = "Usage: ${{ Structr.get_cache_value(cacheKey); }}. Example: ${{ Structr.get_cache_value('mykey'); }}";

	@Override
	public String getName() {
		return "get_cache_value";
	}

	@Override
	public String getSignature() {
		return "key";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasLengthAndAllElementsNotNull(sources, 1);

			final String cacheKey = sources[0].toString();

			logger.warn("get_cache_value() is deprecated and will be removed in a future version.");
			return CacheExpression.getCachedValue(cacheKey);

		} catch (ArgumentNullException | ArgumentCountException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());

			return false;
		}
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return inJavaScriptContext ? ERROR_MESSAGE_GET_CACHE_VALUE_JS : ERROR_MESSAGE_GET_CACHE_VALUE;
	}

	@Override
	public String shortDescription() {
		return "Retrieves the cached value for the given key. Returns null if no cached value exists.";
	}
}
