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
package org.structr.api.schema;

import java.net.URI;
import java.util.List;
import java.util.Set;

/**
 */
public interface JsonMethod extends Comparable<JsonMethod> {

	URI getId();
	JsonType getParent();

	String getName();
	JsonMethod setName(final String name);

	String getSource();
	JsonMethod setSource(final String source);

	String getSummary();
	JsonMethod setSummary(final String summary);

	String getDescription();
	JsonMethod setDescription(final String description);

	List<JsonParameter> getParameters();
	JsonMethod addParameter(final String name, final String type);

	String getReturnType();
	JsonMethod setReturnType(final String returnType);

	boolean overridesExisting();
	JsonMethod setOverridesExisting(final boolean overridesExisting);

	boolean doExport();
	JsonMethod setDoExport(final boolean doExport);

	boolean callSuper();
	JsonMethod setCallSuper(final boolean callsSuper);

	boolean isStatic();
	JsonMethod setIsStatic(final boolean isStatic);

	String getHttpVerb();
	JsonMethod setHttpVerb(final String httpVerb);

	boolean isPrivate();
	JsonMethod setIsPrivate(final boolean isPrivate);

	List<String> getExceptions();
	JsonMethod addException(final String exception);

	String getCodeType();
	JsonMethod setCodeType(final String codeType);

	Set<String> getTags();
	JsonMethod addTags(final String... tags);

	boolean includeInOpenAPI();
	JsonMethod setIncludeInOpenAPI(final boolean includeInOpenAPI);

	String getOpenAPIReturnType();
	JsonMethod setOpenAPIReturnType(final String openAPIReturnType);
}
