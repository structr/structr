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
package org.structr.autocomplete;

import java.util.List;
import org.structr.core.GraphObjectMap;
import org.structr.core.property.Property;
import org.structr.core.property.StringProperty;

/**
 *
 *
 */
public abstract class AbstractHint {

	public static final Property<String> text             = new StringProperty("text");
	public static final Property<String> documentationKey = new StringProperty("documentation");
	public static final Property<String> replacementKey   = new StringProperty("replacement");
	public static final Property<String> typeKey          = new StringProperty("type");

	private boolean dontModify     = false;
	private boolean isDynamic      = false;
	protected String name          = null;
	protected String documentation = null;
	protected String replacement   = null;

	public abstract String getName();
	public abstract String getType();

	public String getDocumentation() {
	    return documentation;
    };

	public String getReplacement() {

		if (replacement != null) {
			return replacement;
		}

		return getName();
	}

	public String getDisplayName() {
		return getName();
	}

	public void allowNameModification(final boolean allowModification) {
		this.dontModify = !allowModification;
	}

	public boolean mayModify() {
		return !dontModify;
	}

	public void setIsDynamic(final boolean isDynamic) {
		this.isDynamic = isDynamic;
	}

	public boolean isDynamic() {
		return isDynamic;
	}

	public boolean isHidden() {
		return false;
	}

	public List<AbstractHint> getContextHints(final String lastToken) {
		return null;
	}

	public GraphObjectMap toGraphObject() {

		final GraphObjectMap item = new GraphObjectMap();

		item.put(text,             getDisplayName());
		item.put(documentationKey, getDocumentation());
		item.put(replacementKey,   getReplacement());
		item.put(typeKey,          getType());

		return item;
	}
}
