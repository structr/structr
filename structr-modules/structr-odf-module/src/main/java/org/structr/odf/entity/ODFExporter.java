/*
 * Copyright (C) 2010-2025 Structr GmbH
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
package org.structr.odf.entity;

import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.transform.VirtualType;
import org.structr.web.entity.File;

/**
 * Base class for ODF exporter
 */
public interface ODFExporter extends NodeInterface {

	String ODF_IMAGE_PARENT_NAME                 = "draw:frame";
	String ODF_IMAGE_ATTRIBUTE_PARENT_IMAGE_NAME = "draw:name";
	String ODF_IMAGE_ATTRIBUTE_FILE_PATH         = "xlink:href";
	String ODF_IMAGE_DIRECTORY                   = "Pictures/";

	File getDocumentTemplate();
	File getResultDocument();
	void setResultDocument(final File resultDocument) throws FrameworkException;
	VirtualType getTransformationProvider();
}
