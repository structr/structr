/*
 * Copyright (C) 2010-2026 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.excel;

import org.structr.common.error.ArgumentTypeException;

import java.util.*;

public class ToExcelOptions {

	public static final String SHEET_NAME          = "sheetName";
	public static final String PROPERTIES          = "properties";
	public static final String VIEW                = "view";
	public static final String INCLUDE_HEADER      = "includeHeader";
	public static final String LOCALIZE_HEADER     = "localizeHeader";
	public static final String LOCALIZATION_DOMAIN = "localizationDomain";
	public static final String MAX_CELL_LENGTH     = "maxCellLength";
	public static final String OVERFLOW_MODE       = "overflowMode";

	private final Map<String, Object> options;

	private static final Set<String> ACCEPTED = Set.of(PROPERTIES, VIEW, INCLUDE_HEADER, LOCALIZE_HEADER, LOCALIZATION_DOMAIN, MAX_CELL_LENGTH, OVERFLOW_MODE, SHEET_NAME);

	private ToExcelOptions(final Map<String, Object> options) {

		this.options = options != null ? options : Collections.emptyMap();
	}

	public static ToExcelOptions from(final String functionName, final Object[] sources, final int index) {

		final Object source = sources[index];
		if (source instanceof Map map) {

			return new ToExcelOptions(map);
		}

		throw new ArgumentTypeException(functionName + "(): expected an options object as argument " + (index + 1) + ", got " + source.getClass().getSimpleName() + " '" + source + "'.");
	}

	public ToExcelOptions validate(final String functionName) {

		for (final String key : options.keySet()) {

			if (!ACCEPTED.contains(key)) {

				throw new ArgumentTypeException(functionName + "(): unknown option '" + key + "'. accepted options: " + String.join(", ", new TreeSet<>(ACCEPTED)) + ".");
			}
		}

		if (options.containsKey(VIEW) && options.containsKey(PROPERTIES)) {

			throw new ArgumentTypeException(functionName + "(): options '" + VIEW + " and '" + PROPERTIES + "' are mutually exclusive.");
		}

		if (!options.containsKey(VIEW) && !options.containsKey(PROPERTIES)) {

			throw new ArgumentTypeException(functionName + "(): one of '" + VIEW + " or '" + PROPERTIES + "' must be present.");
		}

		return this;
	}

	private String getString(final String key, final String defaultValue) {

		final Object value = options.get(key);

		return (value != null) ? value.toString() : defaultValue;
	}

	private Boolean getBoolean(final String key, final Boolean defaultValue) {

		final Object value = options.get(key);

		return value instanceof Boolean b ? b : defaultValue;
	}

	private Integer getInteger(final String key, final Integer defaultValue) {

		final Object value = options.get(key);

		return value instanceof Integer i ? i : defaultValue;
	}

	public String getSheetName() {

		return getString(SHEET_NAME, null);
	}

	public List<String> getProperties() {

		final Object value = options.get(PROPERTIES);

		return (value != null) ? (List<String>) value : null;
	}

	public String getPropertyView() {

		return getString(VIEW, null);
	}

	public Boolean getIncludeHeader() {

		return getBoolean(INCLUDE_HEADER, true);
	}

	public Boolean getLocalizeHeader() {

		return getBoolean(LOCALIZE_HEADER, false);
	}

	public String getLocalizationDomain() {

		return getString(LOCALIZATION_DOMAIN, null);
	}

	/**
	 * maxCellLength must not exceed 32767, which is also the default
	 */
	public int getMaxCellLength() {

		return Math.min(32767, getInteger(MAX_CELL_LENGTH, 32767));
	}

	public String getOverflowMode() {

		return getString(OVERFLOW_MODE, "o");
	}
}
