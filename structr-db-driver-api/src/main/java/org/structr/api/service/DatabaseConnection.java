/**
 * Copyright (C) 2010-2019 Structr GmbH
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
 * @author Christian Morgner
 */
public class DatabaseConnection extends LinkedHashMap<String, Object> {

	public static final String KEY_ACTIVE                  = "active";
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

	public void setName(final String name) {
		put(KEY_NAME, name);
	}

	public String getName() {
		return (String)get(KEY_NAME);
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

		final String name = getName();
		final Tag div     = parent.block("div").css("connection app-tile");

		div.block("button").attr(new Attr("type", "button")).text("x").css("delete-button").attr(new Attr("onclick", "deleteConnection('" + name + "');"));

		div.block("h4").text(name + (isActive() ? " &raquo;active&laquo;" : ""));

		final Tag url = div.block("p");
		url.block("label").text("Connection URL");
		url.add(new InputField(url, "text", "url-" + name, getUrl()));

		final Tag user = div.block("p");
		user.block("label").text("Username");
		user.add(new InputField(user, "text", "username-" + name, getUsername()));

		final Tag pass = div.block("p");
		pass.block("label").text("Password");
		pass.add(new InputField(pass, "password", "password-" + name, getPassword()));

		final Tag buttons = div.block("p").css("buttons");

		if (!isActive()) {
			buttons.block("button").attr(new Attr("type", "button")).text("Use this connection").attr(new Attr("onclick", "useConnection('" + name + "');"));
		}

		buttons.block("button").attr(new Attr("type", "button")).text("Save").attr(new Attr("onclick", "saveConnection('" + name + "')"));

	}
}