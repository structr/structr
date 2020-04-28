/**
 * Copyright (C) 2010-2020 Structr GmbH
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
public abstract class Setting<T> {

	private static final Logger logger = LoggerFactory.getLogger(Setting.class);

	protected SettingsGroup group = null;
	protected boolean isDynamic   = false;
	protected T defaultValue      = null;
	protected String category     = null;
	protected String key          = null;
	protected T value             = null;
	protected String comment      = null;

	public abstract void render(final Tag parent);
	public abstract void fromString(final String source);
	protected abstract Setting<T> copy(final String key);

	public Setting(final SettingsGroup group, final String categoryName, final String key, final T value) {

		this(group, categoryName, key, value, null);
	}

	public Setting(final SettingsGroup group, final String categoryName, final String key, final T value, final String comment) {

		this.key          = key.toLowerCase();
		this.value        = value;
		this.category     = categoryName;
		this.group        = group;
		this.defaultValue = value;
		this.comment      = comment;

		group.registerSetting(this);
		Settings.registerSetting(this);
	}

	public String getKey() {
		return key;
	}

	public void updateKey(final String key) {

		if (isDynamic()) {

			unregister();

			this.key = key;

			group.registerSetting(this);
			Settings.registerSetting(this);
		}
	}

	public String getCategory() {
		return category;
	}

	public String getComment() {
		return comment;
	}

	public T getValue() {
		return value;
	}

	public T getPrefixedValue(final String prefix) {

		if (StringUtils.isBlank(prefix)) {
			return getValue();
		}

		final Setting<T> prefixedSetting = getPrefixedSetting(prefix);

		return prefixedSetting.getValue();
	}

	public T getDefaultValue() {
		return defaultValue;
	}

	public T getValue(final T defaultValue) {

		if (value == null) {

			return defaultValue;
		}

		return value;
	}

	public Setting<T> getPrefixedSetting(final String prefix) {

		Setting<T> prefixedSetting = Settings.getSetting(prefix, key);
		if (prefixedSetting == null) {

			prefixedSetting = copy(prefix + "." + key);
		}

		return prefixedSetting;
	}

	public T getPrefixedValue(final String prefix, final T defaultValue) {

		if (StringUtils.isBlank(prefix)) {
			return getValue(defaultValue);
		}

		final Setting<T> prefixedSetting = getPrefixedSetting(prefix);

		return prefixedSetting.getValue(defaultValue);
	}

	public void setValue(final T value) {
		this.value = value;
	}

	public void setDefaultValue(final T value) {
		this.defaultValue = value;
	}

	public boolean isModified() {
		return (defaultValue != null && !defaultValue.equals(value)) || (defaultValue == null && value != null);
	}

	public boolean isDynamic() {
		return isDynamic;
	}

	public void setIsDynamic(boolean isDynamic) {
		this.isDynamic = isDynamic;
	}

	public void unregister() {

		group.unregisterSetting(this);
		Settings.unregisterSetting(this);
	}

	// ----- protected methods -----
	protected void renderLabel(final Tag group) {

		final Tag label = group.block("label").text(getKey());

		if (getComment() != null) {
			label.attr(new Attr("class", "has-comment"));
			label.attr(new Attr("data-comment", getComment()));
		}
	}

	protected void renderResetButton(final Tag group) {

		if (isModified()) {

			final Tag icon = group.block("i").css("sprite sprite-cancel").attr(new Attr("onclick", "javascript:resetToDefault('" + getKey() + "');"));

			if (isDynamic()) {

				icon.attr(new Attr("title", "Remove"));

			} else {

				icon.attr(new Attr("title", "Reset to default"));
			}
		}
	}
}
