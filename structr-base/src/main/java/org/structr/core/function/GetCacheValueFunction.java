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

import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.core.parser.CacheExpression;
import org.structr.docs.Example;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.docs.Parameter;
import org.structr.schema.action.ActionContext;

import java.util.List;

public class GetCacheValueFunction extends CoreFunction {

	@Override
	public String getName() {
		return "get_cache_value";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllLanguages("key");
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
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${get_cache_value(cacheKey)}. Example: ${get_cache_value('mykey')}"),
			Usage.javaScript("Usage: ${{ $.getCacheValue(cacheKey); }}. Example: ${{ $.getCacheValue('mykey'); }}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Retrieves the cached value for the given key.";
	}

	@Override
	public String getLongDescription() {
		return "Returns null if there is no stored value for the given key or if the stored value is expired.";
	}

	@Override
	public List<Example> getExamples() {
		return List.of(
				Example.structrScript("${get_cache_value('externalResult')}"),
				Example.javaScript("$.get_cache_value('externalResult')")
		);
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(
				Parameter.mandatory("key", "cache key")
		);
	}
}
