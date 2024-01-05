/*
 * Copyright (C) 2010-2024 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.text;

import org.apache.commons.lang.StringUtils;

import java.util.regex.Pattern;

/**
 */

public class AnnotatedLine {

	private static final Pattern numberPattern = Pattern.compile("[0-9]+");

	private String line = null;
	private String type = null;

	public AnnotatedLine(final String line) {

		this.line = line.trim();
	}

	public String getContent() {
		return line;
	}

	public String getType() {
		return type;
	}

	public void setType(final String type) {
		this.type = type;
	}

	public boolean includeLine(final Context context) {

		if (StringUtils.isBlank(line)) {

			context.blankLines++;

			return false;

		} else {

			this.type = "paragraph";

			context.blankLines = 0;

			return !isNumeric() && line.length() > 3;
		}
	}

	public boolean isBlank() {
		return StringUtils.isBlank(line);
	}

	public boolean isNumeric() {
		return numberPattern.matcher(line.trim()).matches();
	}

	void transformAndAnalyze() {

		// replace hyphenated line breaks within words
		// does not always work for the German language..
		//this.line = line.replace("- ", "");
	}
}
