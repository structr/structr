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

import java.util.LinkedList;
import java.util.List;
import org.structr.api.util.html.Tag;

/**
 * A named group of configuration settings.
 */
public class SettingsGroup {

	private final List<Setting> settings = new LinkedList<>();
	private String name                  = null;
	private String key                   = null;

	public SettingsGroup(final String key, final String name) {

		this.key  = key;
		this.name = name;

		Settings.registerGroup(this);
	}

	public String getName() {
		return name;
	}

	public String getKey() {
		return key;
	}

	public void registerSetting(final Setting setting) {
		settings.add(setting);
	}

	public void render(final Tag parent) {

		final Tag div = parent.block("div");

		for (final Setting setting : settings) {

			setting.render(div);
		}
	}
}
