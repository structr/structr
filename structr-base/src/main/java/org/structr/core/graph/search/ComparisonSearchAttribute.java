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
package org.structr.core.graph.search;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;
import org.structr.api.search.ComparisonQuery;
import org.structr.api.search.Operation;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.property.FunctionProperty;
import org.structr.core.property.PropertyKey;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ComparisonSearchAttribute<T> extends SearchAttribute<T> implements ComparisonQuery {

	private PropertyKey<T> searchKey = null;
	private T searchValue	         = null;
	private Comparison comparison    = null;
	private Pattern pattern          = null;

	public ComparisonSearchAttribute(final PropertyKey<T> searchKey, final Comparison comparison, final Object value, final Operation operation) {
		super(operation);

		this.searchKey  = searchKey;
		this.comparison = comparison;

		try {

			if (!(searchKey instanceof FunctionProperty)) {

				PropertyConverter converter = searchKey.inputConverter(SecurityContext.getSuperUserInstance());
				if (converter != null) {

					this.searchValue = (T) converter.convert(value);

				} else {

					try {

						this.searchValue = (T)value.toString();

					} catch (Throwable t) {

						LoggerFactory.getLogger(ComparisonSearchAttribute.class).warn("Could not convert given value. " + t.getMessage());
					}
				}

			} else {

				this.searchValue = this.searchKey.convertSearchValue(SecurityContext.getSuperUserInstance(), value.toString());
			}

		} catch (FrameworkException ex) {

			LoggerFactory.getLogger(ComparisonSearchAttribute.class).warn("Could not convert given value. " + ex.getMessage());
		}
	}

	@Override
	public T getSearchValue() {
		return this.searchValue;
	}

	@Override
	public String toString() {
		return "ComparisonSearchAttribute()";
	}

	@Override
	public PropertyKey getKey() {
		return searchKey;
	}

	@Override
	public Comparison getComparison() {
		return comparison;
	}

	@Override
	public boolean includeInResult(GraphObject entity) {

		final T value = entity.getProperty(searchKey);

		if (value != null && searchValue != null) {

			if (value instanceof Comparable && searchValue instanceof Comparable) {

				final Comparable a = (Comparable)value;
				final Comparable b = (Comparable)searchValue;

				final String propertyStringValue = stringOrNull(value);
				final String searchStringValue   = stringOrNull(searchValue);

				switch (this.comparison) {

					case equal -> {
						return a.compareTo(b) == 0;
					}

					case notEqual -> {
						return a.compareTo(b) != 0;
					}

					case greater -> {
						return a.compareTo(b) > 0;
					}

					case greaterOrEqual -> {
						return a.compareTo(b) >= 0;
					}

					case less -> {
						return a.compareTo(b) < 0;
					}

					case lessOrEqual -> {
						return a.compareTo(b) <= 0;
					}

					case startsWith -> {
						return propertyStringValue != null && searchStringValue != null && StringUtils.startsWith(propertyStringValue, searchStringValue);
					}

					case endsWith -> {
						return propertyStringValue != null && searchStringValue != null && StringUtils.endsWith(propertyStringValue, searchStringValue);
					}

					case contains -> {
						return propertyStringValue != null && searchStringValue != null && StringUtils.contains(propertyStringValue, searchStringValue);
					}

					case caseInsensitiveStartsWith -> {
						return propertyStringValue != null && searchStringValue != null && StringUtils.startsWithIgnoreCase(propertyStringValue, searchStringValue);
					}

					case caseInsensitiveEndsWith -> {
						return propertyStringValue != null && searchStringValue != null && StringUtils.endsWithIgnoreCase(propertyStringValue, searchStringValue);
					}

					case caseInsensitiveContains -> {
						return propertyStringValue != null && searchStringValue != null && StringUtils.containsIgnoreCase(propertyStringValue, searchStringValue);
					}

					case matches -> {
						return getMatcher(searchStringValue, propertyStringValue).matches();
					}
				}
			}

		} else if (value == null && comparison.equals(Comparison.isNull)) {

			return true;

		} else if (value != null && comparison.equals(Comparison.isNotNull)) {

			return true;
		}

		return false;
	}

	@Override
	public Class getQueryType() {
		return ComparisonQuery.class;
	}

	@Override
	public boolean isExactMatch() {
		return true;
	}

	// ----- private methods -----
	private String stringOrNull(final Object value) {

		if (value instanceof String) {
			return (String)value;
		}

		return null;
	}

	private Matcher getMatcher(final String regex, final String input) {

		final Pattern pattern = getPattern(regex);

		return pattern.matcher(input);
	}

	private Pattern getPattern(final String regex) {

		if (this.pattern == null) {

			this.pattern = pattern.compile(regex);
		}

		return this.pattern;
	}
}
