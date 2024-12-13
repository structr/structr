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
package org.structr.web.entity.path;

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.traits.NodeTrait;
import org.structr.web.entity.dom.Page;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public interface PagePath extends NodeTrait {

	Pattern PATH_COMPONENT_PATTERN = Pattern.compile("\\{([a-z][a-zA-Z0-9]+)\\}");

	Page getPage();
	Iterable<PagePathParameter> getParameters();
	Object updatePathAndParameters(final SecurityContext securityContext, final Map<String, Object> arguments) throws FrameworkException;
	Map<String, PagePathParameter> getMappedParameters();
	Map<String, Object> tryResolvePath(final String[] requestParts);
	String[] getValues(final Matcher matcher);
	String getValueOrNull(final String[] array, final int index);
}
