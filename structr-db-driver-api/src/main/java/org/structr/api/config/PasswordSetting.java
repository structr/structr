/*
 * Copyright (C) 2010-2026 Structr GmbH
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
 * A password configuration setting.
 */
public class PasswordSetting extends Setting<String> {

	public PasswordSetting(final SettingsGroup group, final String key) {
		this(group, key, null);
	}

	public PasswordSetting(final SettingsGroup group, final String key, final String value) {
		this(group, null, key, value);
	}

	public PasswordSetting(final SettingsGroup group, final String categoryName, final String key, final String value) {
		super(group, categoryName, key, value);
	}

	public PasswordSetting(final SettingsGroup group, final String categoryName, final String key, final String value, final String comment) {
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
		return new PasswordSetting(group, category, key, value);
	}
}
