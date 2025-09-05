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

import org.structr.api.util.html.Attr;
import org.structr.api.util.html.Tag;

/**
 * A configuration setting with a key and a type.
 */
public class TextSetting extends StringSetting {

	public TextSetting(final SettingsGroup group, final String key, final String value) {
		this(group, null, key, value);
	}

	public TextSetting(final SettingsGroup group, final String groupName, final String key, final String value) {
		super(group, groupName, key, value);
	}

	public TextSetting(final SettingsGroup group, final String groupName, final String key, final String value, final String comment) {
		super(group, groupName, key, value, comment);
	}

	@Override
	public void render(final Tag parent) {

		final Tag group = parent.block("div").css("form-group");

		renderLabel(group);

		group.block("textarea").attr(new Attr("name", getKey())).text(getValue(""));

		renderResetButton(group);
	}
}
