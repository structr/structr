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
package org.structr.api.config;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.util.html.Attr;
import org.structr.api.util.html.Tag;

/**
 * A configuration setting with a key and a type.
 */
public class IntegerSetting extends Setting<Integer> {

	private static final Logger logger = LoggerFactory.getLogger(IntegerSetting.class);

	/**
	 * Constructor to create an empty IntegerSetting with NO default value.
	 *
	 * @param group
	 * @param key
	 */
	public IntegerSetting(final SettingsGroup group, final String key) {
		this(group, key, null);
	}

	/**
	 * Constructor to create an IntegerSetting WITH default value.
	 *
	 * @param group
	 * @param key
	 * @param value
	 */
	public IntegerSetting(final SettingsGroup group, final String key, final Integer value) {
		this(group, null, key, value);
	}

	/**
	 * Constructor to create an IntegerSetting with category name and default value.
	 * @param group
	 * @param categoryName
	 * @param key
	 * @param value
	 */
	public IntegerSetting(final SettingsGroup group, final String categoryName, final String key, final Integer value) {
		super(group, categoryName, key, value);
	}

	/**
	 * Constructor to create an IntegerSetting with category name and default value and comment.
	 * @param group
	 * @param categoryName
	 * @param key
	 * @param value
	 * @param comment
	 */
	public IntegerSetting(final SettingsGroup group, final String categoryName, final String key, final Integer value, final String comment) {
		super(group, categoryName, key, value, comment);
	}

	@Override
	public void render(final Tag parent) {

		final Tag group = parent.block("div").css("form-group");

		renderLabel(group);

		final Tag input     = group.empty("input").attr(new Attr("type", "text"), new Attr("name", getKey()));
		final Integer value = getValue();

		// display value if non-empty
		if (value != null) {
			input.attr(new Attr("value", value));
		}

		renderResetButton(group);
	}

	@Override
	public void fromString(final String source) {

		if (source == null) {
			return;
		}

		if (StringUtils.isNotBlank(source)) {

			try {
				setValue(Integer.parseInt(source));

			} catch (NumberFormatException nex) {

				logger.warn("Invalid value for setting {0}: {1}, ignoring.", new Object[] { getKey(), source } );
			}

		} else {

			// this is the "empty" value
			setValue(-1);
		}
	}

	@Override
	protected Setting<Integer> copy(final String key) {
		return new IntegerSetting(group, category, key, value);
	}
}
