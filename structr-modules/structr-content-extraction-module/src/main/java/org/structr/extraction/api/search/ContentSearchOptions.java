/*
 * Copyright (C) 2010-2026 Structr GmbH
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
package org.structr.extraction.api.search;

import org.structr.web.entity.Folder;

import java.util.Set;

/**
 * Options for keyword search over extracted content.
 *
 * @param limit         maximum number of hits
 * @param minScore      hits with a lower score are left out
 * @param fileUuids     only chunks of these files; null for all files
 * @param folder        only chunks of files in this folder and its subfolders; null for all folders
 * @param contextChunks number of chunks before and after each hit to include as context; 0 for none
 * @param mode          how the query is read
 */
public record ContentSearchOptions(int limit, double minScore, Set<String> fileUuids, Folder folder, int contextChunks, ContentSearchMode mode) {

	public static ContentSearchOptions defaults() {

		return new ContentSearchOptions(10, 0.0, null, null, 0, ContentSearchMode.LUCENE);
	}
}
