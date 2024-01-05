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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.structr.api.util.html.Attr;
import org.structr.api.util.html.Tag;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A configuration setting with a key and a type.
 */
public class ChoiceSetting extends StringSetting {

	private final Map<String, String> choices = new LinkedHashMap<>();

	public ChoiceSetting(final SettingsGroup group, final String categoryName, final String key, final String value, final Set<String> choices) {
		this(group, categoryName, key, value, choices, null);
	}

	public ChoiceSetting(final SettingsGroup group, final String categoryName, final String key, final String value, final Set<String> choices, final String comment) {
		super(group, categoryName, key, value, comment);

		this.choices.putAll(choices.stream().collect(Collectors.toMap(Function.identity(), Function.identity())));
	}

	public ChoiceSetting(final SettingsGroup group, final String categoryName, final String key, final String value, final Map<String, String> choicesMap, final String comment) {
		super(group, categoryName, key, value, comment);

		this.choices.putAll(choicesMap);
	}

	@Override
	public void render(final Tag parent) {

		final Tag group = parent.block("div").css("form-group");

		renderLabel(group);

		final Tag select = group.block("select").attr(new Attr("name", getKey()));

		for (final Map.Entry<String, String> entry : choices.entrySet()) {

			final Tag option = select.block("option").text(entry.getValue()).attr(new Attr("value", entry.getKey()));

			// selected?
			if (entry.getKey().equals(getValue())) {
				option.attr(new Attr("selected", "selected"));
			}
		}

		renderResetButton(group);
	}
}
