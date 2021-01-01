/*
 * Copyright (C) 2010-2021 Structr GmbH
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

import java.util.LinkedHashSet;
import java.util.Set;
import org.structr.api.util.html.Attr;
import org.structr.api.util.html.Tag;

/**
 * A configuration setting with a key and a type.
 */
public class ChoiceSetting extends StringSetting {

	private final Set<String> choices = new LinkedHashSet<>();

	public ChoiceSetting(final SettingsGroup group, final String groupName, final String key, final String value, final Set<String> choices) {
		this(group, groupName, key, value, choices, null);
	}

	public ChoiceSetting(final SettingsGroup group, final String groupName, final String key, final String value, final Set<String> choices, final String comment) {
		super(group, groupName, key, value, comment);

		this.choices.addAll(choices);
	}

	@Override
	public void render(final Tag parent) {

		final Tag group = parent.block("div").css("form-group");

		renderLabel(group);

		final Tag select = group.block("select").attr(new Attr("name", getKey()));

		for (final String choice : choices) {

			final Tag option = select.block("option").text(choice);

			// selected?
			if (choice.equals(getValue())) {
				option.attr(new Attr("selected", "selected"));
			}
		}

		renderResetButton(group);
	}
}
