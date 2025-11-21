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
package org.structr.core.parser;

import org.apache.commons.lang3.StringUtils;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.UnlicensedScriptException;
import org.structr.core.GraphObject;
import org.structr.core.Services;
import org.structr.docs.*;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.EvaluationHints;

import java.util.List;

/**
 *
 *
 */

public class CacheExpression extends Expression {

	private Expression keyExpression     = null;
	private Expression timeoutExpression = null;
	private Expression valueExpression   = null;

	public CacheExpression(final int row, final int column) {
		super("cache", row, column);
	}

	@Override
	public void add(final Expression expression) throws FrameworkException {

		// first expression is the if condition
		if (this.keyExpression == null) {

			this.keyExpression = expression;

		} else if (this.timeoutExpression == null) {

			this.timeoutExpression = expression;

		} else if (this.valueExpression == null) {

			this.valueExpression = expression;

		} else {

			throw new FrameworkException(422, "Invalid cache() expression in builtin function: too many parameters.");
		}

		expression.parent = this;
		expression.level  = this.level + 1;
	}

	@Override
	public Object evaluate(final ActionContext ctx, final GraphObject entity, final EvaluationHints hints) throws FrameworkException, UnlicensedScriptException {

		if (keyExpression == null) {
			return "Error: cache(): key expression may not be empty.";
		}

		final Object keyObject = keyExpression.evaluate(ctx, entity, hints);
		if (keyObject == null) {

			return "Error: cache(): key may not be empty.";
		}

		final String key = keyObject.toString();
		if (StringUtils.isBlank(key)) {

			return "Error: cache(): key may not be empty.";
		}

		if (timeoutExpression == null) {
			return "Error: cache(): timeout expression may not be empty.";
		}

		final Object timeoutValue = timeoutExpression.evaluate(ctx, entity, hints);
		if (timeoutValue == null || !(timeoutValue instanceof Number)) {

			return "Error: cache(): timeout must be non-empty and a number.";
		}

		if (valueExpression == null) {
			return "Error: cache(): value expression may not be empty.";
		}

		final long timeout = ((Number)timeoutValue).longValue();

		// get or create new cached value
		final Services services = Services.getInstance();
		CachedValue cachedValue = (CachedValue)services.getCachedValue(key);
		if (cachedValue == null) {

			cachedValue = new CachedValue(timeout);
			services.cacheValue(key, cachedValue);

		} else {

			cachedValue.setTimeoutSeconds(timeout);
		}

		// refresh value from value expression (this is the only place the value expression is evaluated)
		if (cachedValue.isExpired()) {
			cachedValue.refresh(valueExpression.evaluate(ctx, entity, hints));
		}

		return cachedValue.getValue();
	}

	public static boolean hasCachedValue(final String key) {

		final CachedValue cachedValue = (CachedValue)Services.getInstance().getCachedValue(key);

		if (cachedValue == null) {

			return false;

		} else {

			return !cachedValue.isExpired();
		}
	}

	public static Object getCachedValue(final String key) {

		final CachedValue cachedValue = (CachedValue)Services.getInstance().getCachedValue(key);

		if (cachedValue == null || cachedValue.isExpired()) {

			return null;
		}

		return cachedValue.getValue();
	}

	public static void deleteCachedValue(final String key) {

		Services.getInstance().invalidateCachedValue(key);
	}

	private static final class CachedValue {

		private Object value        = null;
		private long timeoutSeconds = 0L;
		private long timeout        = 0L;

		public CachedValue(final long timeoutSeconds) {
			setTimeoutSeconds(timeoutSeconds);
		}

		public final void setTimeoutSeconds(final long timeoutSeconds) {
			this.timeoutSeconds = timeoutSeconds;
		}

		public final Object getValue() {
			return value;
		}

		public final boolean isExpired() {
			return System.currentTimeMillis() > timeout;
		}

		public final void refresh(final Object value) {

			this.timeout = System.currentTimeMillis() + (timeoutSeconds * 1000);
			this.value   = value;
		}
	}

	@Override
	public Object transform(final ActionContext ctx, final GraphObject entity, final Object source, final EvaluationHints hints) throws FrameworkException {
		return source;
	}

	@Override
	public String getName() {
		return "cache";
	}

	@Override
	public String getShortDescription() {
		return "Stores a value in the global cache.";
	}

	@Override
	public String getLongDescription() {
		return "This function can be used to store a value (which is costly to obtain or should not be updated frequently) under the given key in a global cache. The method will execute the valueExpression to obtain the value, and store it for the given time (in seconds). All subsequent calls to the `cache()` method will return the stored value (until the timeout expires) instead of evaluating the valueExpression.";
	}

	@Override
	public List<Parameter> getParameters() {
		return List.of(
			Parameter.mandatory("key", "cache key"),
			Parameter.mandatory("timeout", "timeout in seconds"),
			Parameter.mandatory("valueExpression", "expression that generates the stored value")
		);
	}

	@Override
	public List<Example> getExamples() {
		return List.of(
			Example.structrScript("${cache('externalResult', 3600, GET('http://api.myservice.com/get-external-result'))}", "Fetch a value from an external API endpoint and cache the result for an hour"),
			Example.javaScript("""
			${{
			    $.cache('myCacheKey', 3600, 'initialCacheValue');
			    $.cache('myCacheKey', 3600, 'test test test');
			    $.log($.cache('myCacheKey', 3600, 'test 2 test 2 test 2'));
			}}
			""", "Log `initialCacheValue` to the server log because the initialCacheValue holds for one hour"),
			Example.javaScript("""
			${{
			    $.cache('externalResult', 3600, () => {
			        return $.GET('http://api.myservice.com/get-external-result');
			    });
			}}
			""", "Fetch a value from an external API endpoint and cache the result for an hour"
			)
		);
	}

	@Override
	public List<String> getNotes() {
		return List.of(
			"The valueExpression will be used to create the initial value.",
			"Since subsequent calls to cache() will return the previous result it can be desirable to delete the previous value in order to be able to store a new value. This can be done via the `delete_cache_value()` function.,",
			"Usage in JavaScript is almost identical, but a complex `valueExpression` needs to be wrapped in an anonymous function so execution can be skipped if a valid cached value is present. If no anonymous function is used, the code is *always* executed and thus defeats the purpose of using `$.cache()`"
		);
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("key, timeout, valueExpression");
	}

	@Override
	public List<Language> getLanguages() {
		return Language.scriptingLanguages();
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${cache(key, timeout, valueExpression)}. Example: ${cache('data', 3600, GET('http://api.myservice.com/get-external-result'))}"),
			Usage.javaScript("Usage: ${{ $.cache(key, timeout, valueExpression) }}. Example: ${{ $.cache('data', 3600, () => $.GET('http://api.myservice.com/get-external-result')); }}")
		);
	}
}
