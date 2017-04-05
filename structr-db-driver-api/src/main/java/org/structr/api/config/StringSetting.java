/**
 * Copyright (C) 2010-2017 Structr GmbH
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

import org.apache.commons.lang.StringUtils;
import org.structr.api.util.html.Attr;
import org.structr.api.util.html.Tag;

/**
 * A configuration setting with a key and a type.
 */
public class StringSetting extends Setting<String> {

	public StringSetting(final SettingsGroup group, final String key, final String value) {
		this(group, null, key, value);
	}

	public StringSetting(final SettingsGroup group, final String groupName, final String key, final String value) {
		super(group, groupName, key, value);
	}

	@Override
	public void render(final Tag parent) {

		final Tag group = parent.block("div").css("form-group");

		group.block("label").text(getKey());
		group.empty("input").attr(new Attr("type", "text"), new Attr("value", getValue()));

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
}
