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
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.schema.action.ActionContext;

import java.util.List;

public class HasCacheValueFunction extends CoreFunction {

	@Override
	public String getName() {
		return "has_cache_value";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("key");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasLengthAndAllElementsNotNull(sources, 1);

			final String cacheKey = sources[0].toString();

			logger.warn("has_cache_value() is deprecated and will be removed in a future version.");
			return CacheExpression.hasCachedValue(cacheKey);

		} catch (ArgumentNullException | ArgumentCountException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());

			return false;
		}
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${has_cache_value(cacheKey)}."),
			Usage.javaScript("Usage: ${{ Structr.has_cache_value(cacheKey); }}.")
		);
	}

	@Override
	public String getShortDescription() {
		return "Checks if a cached value exists for the given key.";
	}

	@Override
	public String getLongDescription() {

		return """
		Checks if a cached value exists for the given key. Returns false if there is no stored value for the given key or if the stored value is expired.
		This function is especially useful if the result of a JavaScript function should be cached (see Example 2).
		""";
	}

	@Override
	public List<Example> getExamples() {
		return List.of(
				Example.structrScript("${has_cache_value('externalResult')}"),
				Example.javaScript("""
						${{
							let myComplexFunction = function() {
								// computation... for brevity just return a date string
								return new Date().toString();
							};
							let cacheKey = 'myKey';
							if ($.hasCacheValue(cacheKey)) {
								// retrieve cached value
								let cacheValue = $.getCacheValue(cacheKey);
								// ...
								// ...
							} else {
								// cache the result of a complex function
								let cacheResult = $.cache(cacheKey, 30, myComplexFunction());
								// ...
								// ...
							}
						}}
						""")
		);
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(
				Parameter.mandatory("key", "cache key")
		);
	}
}
