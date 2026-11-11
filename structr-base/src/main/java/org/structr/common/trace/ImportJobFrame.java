/*
 * Copyright (C) 2010-2026 Structr GmbH
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
package org.structr.common.trace;

import java.util.Map;

/**
 * A job importing a file, such as a CSV or XML import.
 */
public class ImportJobFrame extends JobFrame {

	private final String fileName;

	public ImportJobFrame(final CallFrame creator, final String jobName, final String fileName) {

		super(creator, jobName);

		this.fileName = fileName;
	}

	@Override
	public String describe() {
		return "import of " + fileName;
	}

	@Override
	public Map<String, Object> toMap() {

		final Map<String, Object> map = super.toMap();

		map.put("file", fileName);

		return map;
	}
}
