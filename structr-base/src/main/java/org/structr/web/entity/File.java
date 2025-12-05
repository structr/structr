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
package org.structr.web.entity;

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.graph.NodeInterface;

import javax.activation.DataSource;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

public interface File extends AbstractFile, DataSource {

	String getXMLStructure(final SecurityContext securityContext) throws FrameworkException;
	Long doCSVImport(final SecurityContext securityContext, final Map<String, Object> parameters) throws FrameworkException;
	Long doXMLImport(final SecurityContext securityContext, final Map<String, Object> parameters) throws FrameworkException;
	Map<String, Object> getCSVHeaders(final SecurityContext securityContext, final Map<String, Object> parameters) throws FrameworkException;
	Map<String, Object> getFirstLines(final SecurityContext securityContext, final Map<String, Object> parameters);
	OutputStream getOutputStream(final boolean notifyIndexerAfterClosing, boolean append);
	InputStream getRawInputStream();
	boolean isTemplate();
	boolean dontCache();

	GraphObject getSearchContext(SecurityContext ctx, String searchTerm, int contextLength);

	void notifyUploadCompletion();
	void callOnUploadHandler(final SecurityContext ctx);
	void increaseVersion() throws FrameworkException;
	void setVersion(int version) throws FrameworkException;
	Integer getVersion();
	Integer getCacheForSeconds();
	Long getChecksum();
	Long getFileModificationDate();
	String getMd5();

	void setSize(Long size) throws FrameworkException;
	Long getSize();

	Folder getCurrentWorkingDir();
	String getFormattedSize();
	String getExtractedContent();
	String getPath();
	String getDiskFilePath(final SecurityContext securityContext);

	java.io.File getFileOnDisk();

	int getNumberOrDefault(Map<String, Object> data, String key, int defaultValue);

	void checkMoveBinaryContents(final NodeInterface newProvider);
	void checkMoveBinaryContents(final Folder previousParent, final NodeInterface newParent);

	boolean doIndexing();

	String getContentType();

	boolean isImmutable();
}
