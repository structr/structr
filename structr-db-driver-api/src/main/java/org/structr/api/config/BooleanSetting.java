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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.util.html.Attr;
import org.structr.api.util.html.Tag;

/**
 * A configuration setting with a key and a type.
 */
public class BooleanSetting extends Setting<Boolean> {

	private static final Logger logger = LoggerFactory.getLogger(BooleanSetting.class);

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
		this(group, categoryName, key, value, null);
	}

	/**
	 * Constructor to create a BooleanSetting with category name, default
	 * value and comment.
	 *
	 * @param group
	 * @param categoryName
	 * @param key
	 * @param value
	 */
	public BooleanSetting(final SettingsGroup group, final String categoryName, final String key, final Boolean value, final String comment) {
		super(group, categoryName, key, value, comment);
	}

	@Override
	public void render(final Tag parent) {

		final Tag group = parent.block("div").css("form-group");

		renderLabel(group);

		final Tag trueLabel  = group.block("label").attr(new Attr("class", "flex items-center"));
		final Tag trueInput  = trueLabel.empty("input").attr(new Attr("type", "radio"), new Attr("name", getKey()), new Attr("value",  "true"));
		trueLabel.block("span").text("Enabled");

		final Tag falseLabel = group.block("label").attr(new Attr("class", "flex items-center"));
		final Tag falseInput = falseLabel.empty("input").attr(new Attr("type", "radio"), new Attr("name", getKey()), new Attr("value", "false"));
		falseLabel.block("span").text("Disabled");

		if (getValue(false)) {

			trueInput.attr(new Attr("checked", "checked"));

		} else {

			falseInput.attr(new Attr("checked", "checked"));
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

				setValue(Boolean.parseBoolean(source));

			} catch (NumberFormatException nex) {

				logger.warn("Invalid value for setting {0}: {1}, ignoring.", new Object[] { getKey(), source } );
			}

		} else {

			// this is the "empty" value
			setValue(false);
		}
	}

	@Override
	protected Setting<Boolean> copy(final String key) {
		return new BooleanSetting(group, category, key, value);
	}
}
