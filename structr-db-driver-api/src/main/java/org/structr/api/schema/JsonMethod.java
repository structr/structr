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
package org.structr.api.schema;

import java.net.URI;
import java.util.List;
import java.util.Set;

/**
 */
public interface JsonMethod extends Comparable<JsonMethod> {

	public URI getId();
	public JsonType getParent();

	public String getName();
	public JsonMethod setName(final String name);

	public String getSource();
	public JsonMethod setSource(final String source);

	public String getComment();
	public JsonMethod setComment(final String comment);

	public String getSummary();
	public JsonMethod setSummary(final String summary);

	public String getDescription();
	public JsonMethod setDescription(final String description);

	public List<JsonParameter> getParameters();
	public JsonMethod addParameter(final String name, final String type);

	public String getReturnType();
	public JsonMethod setReturnType(final String returnType);

	public boolean overridesExisting();
	public JsonMethod setOverridesExisting(final boolean overridesExisting);

	public boolean doExport();
	public JsonMethod setDoExport(final boolean doExport);

	public boolean callSuper();
	public JsonMethod setCallSuper(final boolean callsSuper);

	public boolean isStatic();
	public JsonMethod setIsStatic(final boolean callsSuper);

	public List<String> getExceptions();
	public JsonMethod addException(final String exception);

	public String getCodeType();
	public JsonMethod setCodeType(final String codeType);

	Set<String> getTags();
	void addTags(final String... tags);
}
