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
package org.structr.extraction;

import org.structr.extraction.entity.ExtractedContentNode;

/**
 * Type and relationship names of the content extraction module.
 */
public final class ContentExtractionTraits {

	public static final String EXTRACTED_CONTENT_NODE = "ExtractedContentNode";

	/** Configuration of automatic extraction for a folder subtree or a single file. */
	public static final String CONTENT_EXTRACTION_CONFIGURATION = "ContentExtractionConfiguration";

	/** ContentExtractionConfiguration -[:CONFIGURES]-> AbstractFile (File or Folder) */
	public static final String CONFIGURATION_CONFIGURES = "ContentExtractionConfigurationCONFIGURESAbstractFile";

	/** File -[:HAS_EXTRACTED_CONTENT]-> ExtractedContentNode */
	public static final String FILE_HAS_EXTRACTED_CONTENT = "FileHAS_EXTRACTED_CONTENTExtractedContentNode";

	/** ExtractedContentNode -[:NEXT_EXTRACTED_CONTENT]-> ExtractedContentNode, in reading order */
	public static final String EXTRACTED_CONTENT_NEXT = "ExtractedContentNodeNEXT_EXTRACTED_CONTENTExtractedContentNode";

	/** Name of the fulltext index Structr creates for ExtractedContentNode.text. */
	public static final String TEXT_FULLTEXT_INDEX = "ExtractedContentNode_text_fulltext";

	private ContentExtractionTraits() {
	}
}
