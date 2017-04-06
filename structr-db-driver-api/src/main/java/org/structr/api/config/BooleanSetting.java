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

import org.structr.api.util.html.Attr;
import org.structr.api.util.html.Tag;

/**
 * A configuration setting with a key and a type.
 */
public class BooleanSetting extends Setting<Boolean> {

	/**
	 * Constructor to create an empty BooleanSetting with NO default value.
	 *
	 * @param group
	 * @param key
	 */
	public BooleanSetting(final SettingsGroup group, final String key) {
		this(group, key, null);
	}

	/**
	 * Constructor to create a BooleanSetting WITH default value.
	 *
	 * @param group
	 * @param key
	 * @param value
	 */
	public BooleanSetting(final SettingsGroup group, final String key, final Boolean value) {
		this(group, null, key, value);
	}

	/**
	 * Constructor to create a BooleanSetting with category name and default value.
	 * @param group
	 * @param categoryName
	 * @param key
	 * @param value
	 */
	public BooleanSetting(final SettingsGroup group, final String categoryName, final String key, final Boolean value) {
		super(group, categoryName, key, value);
	}

	@Override
	public void render(final Tag parent) {

		final Tag group = parent.block("div").css("form-group");

		group.block("label").text(getKey());

		final Tag trueInput  = group.empty("input").attr(new Attr("type", "radio"), new Attr("name", getKey()), new Attr("value",  "true"));
		group.block("span").text("Enabled");
		final Tag falseInput = group.empty("input").attr(new Attr("type", "radio"), new Attr("name", getKey()), new Attr("value", "false"));
		group.block("span").text("Disabled");

		if (getValue()) {

			trueInput.attr(new Attr("checked", "checked"));

		} else {

			falseInput.attr(new Attr("checked", "checked"));
		}

		renderResetButton(group);
	}

	@Override
	public void fromString(final String source) {
		setValue(Boolean.parseBoolean(source));
	}
}
