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
package org.structr.schema.action;

import org.structr.common.error.FrameworkException;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 */
public class EvaluationHints {

	private final Map<String, CodeLocation> usedKeys = new TreeMap<>();
	private final Set<String> existingKeys           = new TreeSet<>();

	public void reportExistingKey(final String key) {
		existingKeys.add(key);
	}

	public void reportUsedKey(final String key, final int row, final int column) {

		usedKeys.put(key, new CodeLocation(row, column));
	}

	public void checkForErrorsAndThrowException(final ErrorReporter reporter) throws FrameworkException {

		usedKeys.keySet().removeAll(existingKeys);

		if (!usedKeys.isEmpty()) {

			for (final Entry<String, CodeLocation> entry : usedKeys.entrySet()) {

				final String key            = entry.getKey();
				final CodeLocation location = entry.getValue();

				reporter.reportError("No such function or keyword: " + key, location.row, location.column);
			}
		}
	}

	private class CodeLocation {

		public int row = 1;
		public int column = 1;

		public CodeLocation(final int row, final int column) {

			this.row    = row;
			this.column = column;
		}
	}
}
