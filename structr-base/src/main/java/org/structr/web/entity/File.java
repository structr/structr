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
package org.structr.web.entity;

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.api.AbstractMethod;
import org.structr.core.api.Arguments;
import org.structr.core.api.Methods;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Favoritable;
import org.structr.core.entity.PrincipalInterface;
import org.structr.core.function.Functions;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.graph.Tx;
import org.structr.core.graph.search.SearchCommand;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.property.StartNode;
import org.structr.core.scheduler.JobQueueManager;
import org.structr.core.script.Scripting;
import org.structr.rest.common.XMLStructureAnalyzer;
import org.structr.schema.SchemaService;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.EvaluationHints;
import org.structr.schema.action.Function;
import org.structr.schema.action.JavaScriptSource;
import org.structr.storage.StorageProvider;
import org.structr.storage.StorageProviderFactory;
import org.structr.web.common.ClosingOutputStream;
import org.structr.web.common.FileHelper;
import org.structr.web.common.RenderContext;
import org.structr.web.entity.relationship.FolderCONTAINSFile;
import org.structr.web.importer.CSVFileImportJob;
import org.structr.web.importer.MixedCSVFileImportJob;
import org.structr.web.importer.XMLFileImportJob;
import org.structr.web.property.FileDataProperty;
import org.structr.core.graph.NodeInterface;

import javax.activation.DataSource;
import java.io.OutputStream;
import java.util.Map;

public interface File extends AbstractFile, DataSource {

	String getXMLStructure(final SecurityContext securityContext) throws FrameworkException;
	Long doCSVImport(final SecurityContext securityContext, final Map<String, Object> parameters) throws FrameworkException;
	Long doXMLImport(final SecurityContext securityContext, final Map<String, Object> parameters) throws FrameworkException;
	Map<String, Object> getCSVHeaders(final SecurityContext securityContext, final Map<String, Object> parameters) throws FrameworkException;
	Map<String, Object> getFirstLines(final SecurityContext securityContext, final Map<String, Object> parameters);
	OutputStream getOutputStream(final boolean notifyIndexerAfterClosing, boolean append);
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

	Folder getCurrentWorkingDir();
	String getFormattedSize();
	String getExtractedContent();
	String getPath();

	int getNumberOrDefault(Map<String, Object> data, String key, int defaultValue);

	void checkMoveBinaryContents(final NodeInterface newProvider);
	void checkMoveBinaryContents(final Folder previousParent, final NodeInterface newParent);

	boolean doIndexing();

	// ----- interface JavaScriptSource -----
	String getJavascriptLibraryCode();
	String getContentType();
	boolean useAsJavascriptLibrary();

	boolean isImmutable();

}
