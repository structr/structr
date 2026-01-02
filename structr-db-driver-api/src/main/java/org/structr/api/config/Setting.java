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
import org.structr.api.util.html.Attr;
import org.structr.api.util.html.Tag;

/**
 * A configuration setting with a key and a type.
 */
public abstract class Setting<T> {

	protected SettingsGroup group                = null;
	protected boolean isDynamic                  = false;
	protected boolean isModified                 = false;
	protected boolean isProtected                = false;
	protected T defaultValue                     = null;
	protected String category                    = null;
	protected String key                         = null;
	protected T value                            = null;
	protected String comment                     = null;
	protected SettingChangeHandler changeHandler = null;

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

		final T oldValue      = this.value;
		this.value            = value;
		final boolean changed = ((oldValue == null && value != null) || (oldValue != null && !oldValue.equals(value)));

		if (changed == true && changeHandler != null) {

			changeHandler.execute(this, oldValue, value);
		}

		isModified = changed;
	}

	public void setDefaultValue(final T value) {
		this.defaultValue = value;
	}

	public boolean isModified() {
		return isModified || (defaultValue != null && !defaultValue.equals(value)) || (defaultValue == null && value != null);
	}

	public void setIsModified(final boolean isModified) {
		this.isModified = isModified;
	}

	public boolean isDynamic() {
		return isDynamic;
	}

	public Setting setIsProtected() {
		this.isProtected = true;
		return this;
	}

	public boolean isProtected() {
		return this.isProtected;
	}

	public void setIsDynamic(boolean isDynamic) {
		this.isDynamic = isDynamic;
	}

	public Setting<T> setChangeHandler(final SettingChangeHandler changeHandler) {
		this.changeHandler = changeHandler;
		return this;
	}

	public SettingChangeHandler getChangeHandler() {
		return this.changeHandler;
	}

	public void unregister() {

		group.unregisterSetting(this);
		Settings.unregisterSetting(this);
	}

	// ----- private methods -----
	private String getCalculatedComment() {

		if (getComment() != null) {

			return getComment();

		} else if (getKey().endsWith(".cronExpression")) {

			return Settings.CRON_EXPRESSION_INFO_HTML;
		}

		return null;
	}

	// ----- protected methods -----
	protected void renderLabel(final Tag group) {

		final Tag label           = group.block("label");
		final String finalComment = getCalculatedComment();

		if (finalComment != null) {

			label.attr(new Attr("class", "font-bold basis-full sm:basis-auto sm:min-w-128 has-comment"));
			label.attr(new Attr("data-comment", finalComment));

		} else {

			label.attr(new Attr("class", "font-bold basis-full sm:basis-auto sm:min-w-128"));
		}

		label.text(getKey());
	}

	protected void renderResetButton(final Tag group) {

		if (isModified() || isDynamic()) {

			final Tag icon = group.block("svg").css("reset-key cursor-pointer hover:opacity-100 icon-red ml-4 opacity-60 flex-shrink-0").attr(new Attr("width", 16), new Attr("height", 16), new Attr("data-key", getKey())).block("use").attr(new Attr("href", "#interface_delete_circle"));

			if (isDynamic()) {

				icon.attr(new Attr("title", "Remove"));

			} else {

				icon.attr(new Attr("title", "Reset to default"));
			}
		}
	}
}
