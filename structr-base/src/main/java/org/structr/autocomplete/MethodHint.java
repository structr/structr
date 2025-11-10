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
package org.structr.autocomplete;

import org.apache.commons.lang3.StringUtils;
import org.structr.docs.*;

import java.util.LinkedList;
import java.util.List;

public class MethodHint extends AbstractHint {

	protected String openAPISummary     = "No OpenAPI summary";
	protected String openAPIDescription = "No OpenAPI description";

	public MethodHint(final String name, final String summary, final String description) {

		this.name = name;

		if (!StringUtils.isEmpty(summary)) {
			this.openAPISummary = summary;
		}

		if (!StringUtils.isEmpty(description)) {
			this.openAPIDescription = description;
		}
	}

	@Override
	public String getDocumentation() {

		// this is the place where we assemble the hint text
		final List<String> buf = new LinkedList<>();

		buf.add("**Summary**");
		buf.add(openAPISummary);
		buf.add("");
		buf.add("**Description**");
		buf.add(openAPIDescription);

		return StringUtils.join(buf, "\n");
	}

	@Override
	public String getDisplayName() {
		return getName() + "()";
	}

	@Override
	public String getReplacement() {
		return getName() + "()";
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getShortDescription() {
		return "";
	}

	@Override
	public List<Parameter> getParameters() {
		return null;
	}

	@Override
	public List<Example> getExamples() {
		return null;
	}

	@Override
	public List<String> getNotes() {
		return null;
	}

	@Override
	public List<Signature> getSignatures() {
		return null;
	}

	@Override
	public List<Language> getLanguages() {
		return null;
	}

	@Override
	public DocumentableType getType() {
		return DocumentableType.Method;
	}
}
