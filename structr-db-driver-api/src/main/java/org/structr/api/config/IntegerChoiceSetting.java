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

import org.structr.api.util.html.Attr;
import org.structr.api.util.html.Tag;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A configuration setting with a key and a type.
 */
public class IntegerChoiceSetting extends IntegerSetting {

	private final Map<Integer, String> choices = new LinkedHashMap<>();

	public IntegerChoiceSetting(final SettingsGroup group, final String groupName, final String key, final Integer value, final Map<Integer, String> choices) {
		this(group, groupName, key, value, choices, null);
	}

	public IntegerChoiceSetting(final SettingsGroup group, final String groupName, final String key, final Integer value, final Map<Integer, String> choices, final String comment) {
		super(group, groupName, key, value, comment);

		this.choices.putAll(choices);
	}

	@Override
	public void render(final Tag parent) {

		final Tag group = parent.block("div").css("form-group");

		renderLabel(group);

		final Tag select = group.block("select").attr(new Attr("name", getKey()));

		for (final Map.Entry<Integer, String> entry : choices.entrySet()) {

			final Tag option = select.block("option").text(entry.getValue());
			option.attr(new Attr("value", entry.getKey()));

			// selected?
			if (entry.getKey().equals(getValue())) {
				option.attr(new Attr("selected", "selected"));
			}
		}

		renderResetButton(group);
	}
}
