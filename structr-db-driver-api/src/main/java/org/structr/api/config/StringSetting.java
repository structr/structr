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
package org.structr.api.config;

import org.apache.commons.lang3.StringUtils;
import org.structr.api.util.html.Attr;
import org.structr.api.util.html.Tag;

/**
 * A configuration setting with a key and a type.
 */
public class StringSetting extends Setting<String> {


	/**
	 * Constructor to create an empty StringSetting with NO default value.
	 *
	 * @param group
	 * @param key
	 */
	public StringSetting(final SettingsGroup group, final String key) {
		this(group, key, null);
	}

	/**
	 * Constructor to create a StringSetting WITH default value.
	 *
	 * @param group
	 * @param key
	 * @param value
	 */
	public StringSetting(final SettingsGroup group, final String key, final String value) {
		this(group, null, key, value);
	}


	/**
	 * Constructor to create a StringSetting with category name and default value.
	 * @param group
	 * @param categoryName
	 * @param key
	 * @param value
	 */
	public StringSetting(final SettingsGroup group, final String categoryName, final String key, final String value) {
		super(group, categoryName, key, value);
	}

	/**
	 * Constructor to create a StringSetting with category name, default value and a comment
	 * @param group
	 * @param categoryName
	 * @param key
	 * @param value
	 * @param comment
	 */
	public StringSetting(final SettingsGroup group, final String categoryName, final String key, final String value, final String comment) {
		super(group, categoryName, key, value, comment);
	}

	@Override
	public void render(final Tag parent) {

		final Tag group = parent.block("div").css("form-group");

		renderLabel(group);

		final Tag input    = group.empty("input").attr(new Attr("type", "text"), new Attr("name", getKey()));
		final String value = getValue();

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

		setValue(source);
	}

	@Override
	public String getValue(final String defaultValue) {

		if (StringUtils.isBlank(getValue())) {

			return defaultValue;
		}

		return getValue();
	}

	@Override
	protected Setting<String> copy(final String key) {
		return new StringSetting(group, category, key, value);
	}
}
