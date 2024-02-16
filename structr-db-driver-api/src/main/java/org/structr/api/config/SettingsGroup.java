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

import org.structr.api.util.html.Attr;
import org.structr.api.util.html.Tag;

import java.util.*;
import java.util.Map.Entry;

/**
 * A named group of configuration settings.
 */
public class SettingsGroup {

	private final List<Setting> settings = new ArrayList<>();
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

	public void unregisterSetting(final Setting setting) {
		settings.remove(setting);
	}

	public List<Setting> getSettings() {
		return settings;
	}

	public void render(final Tag menu, final Tag tabContainer) {

		menu.block("li").block("a").id(key + "Menu").attr(new Attr("href", "#" + key)).block("span").text(name);

		final Tag container = tabContainer.block("div").css("tab-content").id(key);

		render(container);
	}

	public void render(final Tag parent) {

		final Map<String, List<Setting>> mapped = new LinkedHashMap<>();
		final List<Setting> otherSettings       = new ArrayList<>();
		final Tag div                           = parent.block("div");

		// sort / categorize settings
		for (final Setting setting : settings) {

			final String group = setting.getCategory();

			// ignore hidden settings
			if ("hidden".equals(group)) {
				continue;
			}

			if (group != null) {

				List<Setting> list = mapped.get(group);
				if (list == null) {

					list = new ArrayList<>();
					mapped.put(group, list);
				}

				list.add(setting);

			} else {

				otherSettings.add(setting);
			}
		}

		// display grouped settings
		for (final Entry<String, List<Setting>> entry : mapped.entrySet()) {

			final String name        = entry.getKey();
			final Tag groupContainer = div.block("div").css("config-group");

			groupContainer.block("h1").text(name);

			for (final Setting setting : entry.getValue()) {

				setting.render(groupContainer);
			}
		}

		// display settings w/o group
		if (!otherSettings.isEmpty()) {

			final Tag groupContainer = div.block("div").css("config-group");

			// display title only if other groups exist
			if (!mapped.isEmpty()) {
				groupContainer.block("h1").text("Custom");
			}

			otherSettings.stream().sorted(Comparator.comparing(Setting::getKey)).forEach((Setting setting) -> setting.render(groupContainer));
		}
	}
}
