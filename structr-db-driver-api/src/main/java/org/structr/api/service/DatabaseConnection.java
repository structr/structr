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
package org.structr.api.service;

import java.util.LinkedHashMap;
import java.util.Map;
import org.structr.api.util.html.Attr;
import org.structr.api.util.html.InputField;
import org.structr.api.util.html.Tag;

/**
 *
 */
public class DatabaseConnection extends LinkedHashMap<String, Object> {

	public static final String KEY_ACTIVE                  = "active";
	public static final String KEY_DISPLAYNAME             = "displayName";
	public static final String KEY_NAME                    = "name";
	public static final String KEY_DRIVER                  = "driver";
	public static final String KEY_URL                     = "url";
	public static final String KEY_USERNAME                = "username";
	public static final String KEY_PASSWORD                = "password";
	public static final String KEY_TENANT_IDENTIFIER       = "tenantIdentifier";
	public static final String KEY_RELATIONSHIP_CACHE_SIZE = "relationshipCacheSize";
	public static final String KEY_NODE_CACHE_SIZE         = "nodeCacheSize";
	public static final String KEY_UUID_CACHE_SIZE         = "uuidCacheSize";
	public static final String KEY_FORCE_STREAMING         = "forceStreaming";

	public DatabaseConnection() {
	}

	public DatabaseConnection(final Map<String, Object> data) {
		putAll(data);
	}

	public void setDisplayName(final String displayName) {
		put(KEY_DISPLAYNAME, displayName);
	}

	public void setName(final String name) {
		put(KEY_NAME, name);
	}

	public String getName() {
		return (String)get(KEY_NAME);
	}

	public String getDisplayName() {
		return (String)get(KEY_DISPLAYNAME);
	}

	public void setUrl(final String url) {
		put(KEY_URL, url);
	}

	public String getUrl() {
		return (String)get(KEY_URL);
	}

	public void setUsername(final String username) {
		put(KEY_USERNAME, username);
	}

	public String getUsername() {
		return (String)get(KEY_USERNAME);
	}

	public void setPassword(final String password) {
		put(KEY_PASSWORD, password);
	}

	public String getPassword() {
		return (String)get(KEY_PASSWORD);
	}

	public boolean isActive() {
		return Boolean.TRUE.equals(get(KEY_ACTIVE));
	}


	public void render(final Tag parent, final String configUrl) {

		final boolean active     = isActive();
		final String displayName = getDisplayName();
		final String name        = getName();
		final Tag div            = parent.block("div").css("connection app-tile" + (active ? " active" : ""));

		div.block("h4").text(displayName + (isActive() ? " (active)" : ""));

		final Tag url = div.block("p");
		url.block("label").text("Connection URL");
		final InputField nameInput = new InputField(url, "text", "url-" + name, getUrl());
		if (isActive()) {
			nameInput.attr(new Attr("readonly", "readonly"));
		}
		url.add(nameInput);

		final Tag user = div.block("p");
		user.block("label").text("Username");
		final InputField usernameInput = new InputField(user, "text", "username-" + name, getUsername());
		if (isActive()) {
			usernameInput.attr(new Attr("readonly", "readonly"));
		}
		user.add(usernameInput);

		final Tag pass = div.block("p");
		pass.block("label").text("Password");
		final InputField passwordInput = new InputField(pass, "password", "password-" + name, getPassword());
		if (isActive()) {
			passwordInput.attr(new Attr("readonly", "readonly"));
		}
		pass.add(passwordInput);

		final Tag buttons = div.block("p").css("buttons");

		if (isActive()) {
			buttons.block("a").css("align-left").attr(new Attr("href", "/structr")).text("Open Structr UI");
			buttons.block("button").attr(new Attr("type", "button")).text("Disconnect").attr(new Attr("onclick", "disconnect(this, '" + name + "');"));
		} else {
			buttons.block("button").attr(new Attr("type", "button")).text("Remove").attr(new Attr("onclick", "deleteConnection('" + name + "');"));
			buttons.block("button").css("default-action").attr(new Attr("type", "button")).text("Connect").attr(new Attr("onclick", "connect(this, '" + name + "');"));
		}

		//buttons.block("button").attr(new Attr("type", "button")).text("Save").attr(new Attr("onclick", "saveConnection('" + name + "')"));

		div.block("div").id("status-" + name).css("warning warning-message hidden");

	}
}
