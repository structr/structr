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
package org.structr.extraction.api.processing;

/**
 * Whether a file is extracted automatically, and which content extraction configuration decided it.
 *
 * @param extract         whether the file is extracted automatically
 * @param source          "file" (a configuration attached to the file itself), "folder" (attached to one of
 *                        its folders) or "default" (no configuration)
 * @param configurationId the UUID of the deciding configuration, or null
 * @param targetPath      the path of the file or folder the deciding configuration is attached to, or null
 */
public record AutoExtractionDecision(boolean extract, String source, String configurationId, String targetPath) {
}
