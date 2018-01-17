/**
 * Copyright (C) 2010-2017 Structr GmbH
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
package org.structr.web.entity;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.activation.DataSource;
import javax.xml.stream.XMLStreamException;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import org.apache.commons.io.IOUtils;
import org.structr.api.config.Settings;
import org.structr.cmis.CMISInfo;
import org.structr.cmis.info.CMISDocumentInfo;
import org.structr.common.ConstantBooleanTrue;
import org.structr.common.Permission;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.UnlicensedException;
import org.structr.common.fulltext.FulltextIndexer;
import org.structr.common.fulltext.Indexable;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Favoritable;
import org.structr.core.entity.Principal;
import org.structr.core.entity.Relation.Cardinality;
import org.structr.core.function.Functions;
import org.structr.core.graph.ModificationEvent;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.script.Scripting;
import org.structr.files.cmis.config.StructrFileActions;
import org.structr.rest.common.XMLStructureAnalyzer;
import org.structr.schema.SchemaService;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;
import org.structr.schema.action.JavaScriptSource;
import org.structr.schema.json.JsonMethod;
import org.structr.schema.json.JsonObjectType;
import org.structr.schema.json.JsonReferenceType;
import org.structr.schema.json.JsonSchema;
import org.structr.web.common.ClosingFileOutputStream;
import org.structr.web.common.FileHelper;
import org.structr.web.common.RenderContext;
import org.structr.web.importer.CSVFileImportJob;
import org.structr.web.importer.XMLFileImportJob;
import org.structr.web.property.FileDataProperty;

/**
 *
 *
 */
public interface File extends AbstractFile, Indexable, Linkable, JavaScriptSource, CMISInfo, CMISDocumentInfo, Favoritable, DataSource {

	static class Impl { static {

		final JsonSchema schema   = SchemaService.getDynamicSchema();
		final JsonObjectType type = schema.addType("File");

		type.setImplements(URI.create("https://structr.org/v1.1/definitions/File"));
		type.setImplements(URI.create("#/definitions/Indexable"));
		type.setImplements(URI.create("#/definitions/Linkable"));
		type.setImplements(URI.create("#/definitions/JavaScriptSource"));
		type.setImplements(URI.create("#/definitions/Favoritable"));
		type.setExtends(URI.create("#/definitions/AbstractFile"));

		type.addStringProperty("relativeFilePath", PropertyView.Public);
		type.addStringProperty("url", PropertyView.Public);

		type.addBooleanProperty("isFile", PropertyView.Public).setReadOnly(true).addTransformer(ConstantBooleanTrue.class.getName());
		type.addBooleanProperty("isTemplate", PropertyView.Public);

		type.addIntegerProperty("cacheForSeconds", PropertyView.Public);
		type.addIntegerProperty("version", PropertyView.Public).setIndexed(true);
		type.addIntegerProperty("position", PropertyView.Public).setIndexed(true);

		type.addLongProperty("size", PropertyView.Public).setIndexed(true);
		type.addLongProperty("checksum", PropertyView.Public).setIndexed(true);
		type.addLongProperty("fileModificationDate");

		type.addStringProperty("md5");
		type.addStringProperty("sha1");
		type.addStringProperty("sha512");

		type.addPropertyGetter("relativeFilePath", String.class);
		type.addPropertyGetter("cacheForSeconds", Integer.class);
		type.addPropertyGetter("checksum", Long.class);
		type.addPropertyGetter("md5", String.class);

		type.addPropertyGetter("version", Integer.class);
		type.addPropertyGetter("contentType", String.class);
		type.addPropertyGetter("extractedContent", String.class);
		type.addPropertyGetter("basicAuthRealm", String.class);
		type.addPropertyGetter("size", Long.class);

		type.addCustomProperty("base64Data", FileDataProperty.class.getName());

		type.overrideMethod("onCreation",                  true,  File.class.getName() + ".onCreation(this, arg0, arg1);");
		type.overrideMethod("onModification",              true,  File.class.getName() + ".onModification(this, arg0, arg1, arg2);");
		type.overrideMethod("onNodeCreation",              true,  File.class.getName() + ".onNodeCreation(this);");
		type.overrideMethod("onNodeDeletion",              true,  File.class.getName() + ".onNodeDeletion(this);");
		type.overrideMethod("afterCreation",               true,  File.class.getName() + ".afterCreation(this, arg0);");

		type.overrideMethod("isTemplate",                  false, "return getProperty(isTemplateProperty);");
		type.overrideMethod("isMounted",                   false, "return " + File.class.getName() + ".isMounted(this);");
		type.overrideMethod("setVersion",                  false, "setProperty(versionProperty, arg0);").addException(FrameworkException.class.getName());

		type.overrideMethod("increaseVersion",             false, File.class.getName() + ".increaseVersion(this);");
		type.overrideMethod("notifyUploadCompletion",      false, File.class.getName() + ".notifyUploadCompletion(this);");
		type.overrideMethod("triggerMinificationIfNeeded", false, File.class.getName() + ".triggerMinificationIfNeeded(this, arg0);");

		type.overrideMethod("getFileOnDisk",               false, "return " + File.class.getName() + ".getFileOnDisk(this);");
		type.overrideMethod("getInputStream",              false, "return " + File.class.getName() + ".getInputStream(this);");
		type.overrideMethod("getSearchContext",            false, "return " + File.class.getName() + ".getSearchContext(this, arg0, arg1);");
		type.overrideMethod("getJavascriptLibraryCode",    false, "return " + File.class.getName() + ".getJavascriptLibraryCode(this);");
		type.overrideMethod("getEnableBasicAuth",          false, "return getProperty(enableBasicAuthProperty);");

		// Favoritable
		type.overrideMethod("getContext",                  false, "return getPath();");
		type.overrideMethod("getFavoriteContent",          false, "return " + File.class.getName() + ".getFavoriteContent(this);");
		type.overrideMethod("setFavoriteContent",          false, File.class.getName() + ".setFavoriteContent(this, arg0);");
		type.overrideMethod("getFavoriteContentType",      false, "return getContentType();");

		// CMIS support
		type.overrideMethod("getCMISInfo",                 false, "return this;");
		type.overrideMethod("getBaseTypeId",               false, "return " + BaseTypeId.class.getName() + ".CMIS_DOCUMENT;");
		type.overrideMethod("getFolderInfo",               false, "return null;");
		type.overrideMethod("getDocumentInfo",             false, "return this;");
		type.overrideMethod("getItemInfo",                 false, "return null;");
		type.overrideMethod("getRelationshipInfo",         false, "return null;");
		type.overrideMethod("getPolicyInfo",               false, "return null;");
		type.overrideMethod("getSecondaryInfo",            false, "return null;");
		type.overrideMethod("getChangeToken",              false, "return null;");
		type.overrideMethod("getParentId",                 false, "return getProperty(parentIdProperty);");
		type.overrideMethod("getAllowableActions",         false, "return new " + StructrFileActions.class.getName() + "(isImmutable());");
		type.overrideMethod("isImmutable",                 false, "return " + File.class.getName() + ".isImmutable(this);");

		// overridden methods
		final JsonMethod getOutputStream1 = type.addMethod("getOutputStream");
		getOutputStream1.setSource("return " + File.class.getName() + ".getOutputStream(this, notifyIndexerAfterClosing, append);");
		getOutputStream1.addParameter("notifyIndexerAfterClosing", "boolean");
		getOutputStream1.addParameter("append", "boolean");
		getOutputStream1.setReturnType(FileOutputStream.class.getName());

		final JsonMethod getOutputStream2 = type.addMethod("getOutputStream");
		getOutputStream2.setSource("return " + File.class.getName() + ".getOutputStream(this, true, false);");
		getOutputStream2.setReturnType(FileOutputStream.class.getName());

		// relationships
		final JsonObjectType minifiedFile = (JsonObjectType)schema.getType("AbstractMinifiedFile");
		final JsonReferenceType rel       = minifiedFile.relate(type, "MINIFICATION", Cardinality.ManyToMany, "minificationTargets", "minificationSources");

		rel.addIntegerProperty("position", PropertyView.Public);

		type.addMethod("doCSVImport")
			.addParameter("parameters", "java.util.Map<java.lang.String, java.lang.Object>")
			.setSource(File.class.getName() + ".doCSVImport(this, parameters);")
			.addException(FrameworkException.class.getName())
			.setDoExport(true);

		type.addMethod("doXMLImport")
			.addParameter("parameters", "java.util.Map<java.lang.String, java.lang.Object>")
			.setSource(File.class.getName() + ".doXMLImport(this, parameters);")
			.addException(FrameworkException.class.getName())
			.setDoExport(true);

		type.addMethod("getFirstLines")
			.addParameter("parameters", "java.util.Map<java.lang.String, java.lang.Object>")
			.setReturnType("java.util.Map<java.lang.String, java.lang.Object>")
			.setSource("return " + File.class.getName() + ".getFirstLines(this, parameters);")
			.setDoExport(true);

		type.addMethod("getCSVHeaders")
			.addParameter("parameters", "java.util.Map<java.lang.String, java.lang.Object>")
			.setReturnType("java.util.Map<java.lang.String, java.lang.Object>")
			.setSource("return " + File.class.getName() + ".getCSVHeaders(this, parameters);")
			.addException(FrameworkException.class.getName())
			.setDoExport(true);

		type.addMethod("getXMLStructure")
			.setReturnType("java.lang.String")
			.setSource("return " + File.class.getName() + ".getXMLStructure(this);")
			.addException(FrameworkException.class.getName())
			.setDoExport(true);

		/* TODO:
			public static final Property<List<User>> favoriteOfUsers                     = new StartNodes<>("favoriteOfUsers", UserFavoriteFile.class);
		*/

	}}

	FileOutputStream getOutputStream();
	FileOutputStream getOutputStream(final boolean notifyIndexerAfterClosing, final boolean append);

	java.io.File getFileOnDisk(final boolean doCreate);
	java.io.File getFileOnDisk();

	boolean isTemplate();

	void notifyUploadCompletion();
	void increaseVersion() throws FrameworkException;
	void triggerMinificationIfNeeded(final ModificationQueue modificationQueue) throws FrameworkException;

	String getRelativeFilePath();

	void setVersion(final int version) throws FrameworkException;
	Integer getVersion();

	Integer getCacheForSeconds();

	Long getChecksum();
	String getMd5();


	// ----- static methods -----
	public static String getDirectoryPath(final String uuid) {

		return (uuid != null)
			? uuid.substring(0, 1) + "/" + uuid.substring(1, 2) + "/" + uuid.substring(2, 3) + "/" + uuid.substring(3, 4)
			: null;

	}

	public static void increaseVersion(final File thisFile) throws FrameworkException {

		final Integer _version = thisFile.getVersion();

		thisFile.unlockSystemPropertiesOnce();
		if (_version == null) {

			thisFile.setVersion(1);

		} else {

			thisFile.setVersion(_version + 1);
		}
	}

	public static void notifyUploadCompletion(final File thisFile) {

		try {

			try (final Tx tx = StructrApp.getInstance().tx()) {

				synchronized (tx) {

					FileHelper.updateMetadata(thisFile, new PropertyMap());

					tx.success();
				}
			}

			final FulltextIndexer indexer = StructrApp.getInstance(thisFile.getSecurityContext()).getFulltextIndexer();
			indexer.addToFulltextIndex(thisFile);

		} catch (FrameworkException fex) {

			logger.warn("Unable to index " + thisFile, fex);
		}
	}

	public static InputStream getInputStream(final File thisFile) {

		final java.io.File fileOnDisk         = thisFile.getFileOnDisk();
		final SecurityContext securityContext = thisFile.getSecurityContext();

		try {

			final FileInputStream fis = new FileInputStream(fileOnDisk);

			if (thisFile.isTemplate()) {

				boolean editModeActive = false;
				if (securityContext.getRequest() != null) {

					final String editParameter = securityContext.getRequest().getParameter("edit");
					if (editParameter != null) {

						editModeActive = !RenderContext.EditMode.NONE.equals(RenderContext.editMode(editParameter));
					}
				}

				if (!editModeActive) {

					final String content = IOUtils.toString(fis, "UTF-8");

					try {

						final String result = Scripting.replaceVariables(new ActionContext(securityContext), thisFile, content);
						return IOUtils.toInputStream(result, "UTF-8");

					} catch (Throwable t) {

						logger.warn("Scripting error in {}:\n{}", getUuid(), content, t);
					}
				}
			}

			return fis;

		} catch (IOException ex) {
			logger.warn("File not found: {}", fileOnDisk);
		}

		return null;
	}

	public static FileOutputStream getOutputStream(final File thisFile, final boolean notifyIndexerAfterClosing, final boolean append) {

		if (thisFile.isTemplate()) {

			logger.error("File is in template mode, no write access allowed: {}", thisFile.getPath());
			return null;
		}

		try {

			// Return file output stream and save checksum and size after closing
			return new ClosingFileOutputStream(thisFile, append, notifyIndexerAfterClosing);

		} catch (IOException e) {
			logger.error("Unable to open file output stream for {}: {}", thisFile.getPath(), e.getMessage());
		}

		return null;

	}

	public static java.io.File getFileOnDisk(final File thisFile) {
		return File.getFileOnDisk(thisFile, true);
	}

	public static java.io.File getFileOnDisk(final File thisFile, final boolean create) {

		final Folder parentFolder = thisFile.getParent();
		if (parentFolder != null) {

			return Folder.getFileOnDisk(parentFolder, thisFile, "", create);

		} else {

			return AbstractFile.defaultGetFileOnDisk(thisFile, create);
		}
	}

	public static boolean isMounted(final File thisFile) {

		final Folder parent = thisFile.getParent();
		if (parent != null) {

			return parent.isMounted();
		}

		return false;
	}

	public static GraphObject getSearchContext(final File thisFile, final String searchTerm, final int contextLength) {

		final String text = thisFile.getExtractedContent();
		if (text != null) {

			final FulltextIndexer indexer = StructrApp.getInstance().getFulltextIndexer();
			return indexer.getContextObject(searchTerm, text, contextLength);
		}

		return null;
	}

	// ----- interface JavaScriptSource -----
	public static String getJavascriptLibraryCode(final File thisFile) {

		try (final InputStream is = thisFile.getInputStream()) {

			return IOUtils.toString(new InputStreamReader(is));

		} catch (IOException ioex) {
			logger.warn("", ioex);
		}

		return null;
	}

	// ----- interface Favoritable -----
	public static String getFavoriteContent(final File thisFile) {

		try (final InputStream is = thisFile.getInputStream()) {

			return IOUtils.toString(is, "utf-8");

		} catch (IOException ioex) {
			logger.warn("", ioex);
		}

		return null;
	}

	public static void setFavoriteContent(final File thisFile, final String content) throws FrameworkException {

		try (final OutputStream os = thisFile.getOutputStream(true, false)) {

			IOUtils.write(content, os, "utf-8");
			os.flush();

		} catch (IOException ioex) {
			logger.warn("", ioex);
		}
	}

	// ----- CMIS support -----
	/*
	@Override
	public CMISInfo getCMISInfo() {
		return this;
	}

	@Override
	public BaseTypeId getBaseTypeId() {
		return BaseTypeId.CMIS_DOCUMENT;
	}

	@Override
	public CMISFolderInfo getFolderInfo() {
		return null;
	}

	@Override
	public CMISDocumentInfo getDocumentInfo() {
		return this;
	}

	@Override
	public CMISItemInfo geItemInfo() {
		return null;
	}

	@Override
	public CMISRelationshipInfo getRelationshipInfo() {
		return null;
	}

	@Override
	public CMISPolicyInfo getPolicyInfo() {
		return null;
	}

	@Override
	public CMISSecondaryInfo getSecondaryInfo() {
		return null;
	}

	@Override
	public String getParentId() {
		return getProperty(FileBase.parentId);
	}

	@Override
	public AllowableActions getAllowableActions() {
		return new StructrFileActions(isImmutable());
	}

	@Override
	public String getChangeToken() {

		// versioning not supported yet.
		return null;
	}
	*/

	public static boolean isImmutable(final File thisFile) {

		final Principal _owner = thisFile.getOwnerNode();
		if (_owner != null) {

			return !_owner.isGranted(Permission.write, thisFile.getSecurityContext());
		}

		return true;
	}

	/* TODO:
		public static final Property<String> base64Data                              = new FileDataProperty<>("base64Data");
		public static final Property<List<AbstractMinifiedFile>> minificationTargets = new StartNodes<>("minificationTarget", MinificationSource.class);
		public static final Property<List<User>> favoriteOfUsers                     = new StartNodes<>("favoriteOfUsers", UserFavoriteFile.class);
	*/

	//public static final Property<String> relativeFilePath                        = new StringProperty("relativeFilePath").systemInternal();
	//public static final Property<Long> size                                      = new LongProperty("size").indexed().systemInternal();
	//public static final Property<String> url                                     = new StringProperty("url");
	//public static final Property<Long> checksum                                  = new LongProperty("checksum").indexed().unvalidated().systemInternal();
	//public static final Property<Integer> cacheForSeconds                        = new IntProperty("cacheForSeconds").cmis();
	//public static final Property<Integer> version                                = new IntProperty("version").indexed().systemInternal();
	//public static final Property<Boolean> isTemplate                             = new BooleanProperty("isTemplate");
	//public static final Property<Boolean> isFile                                 = new ConstantBooleanProperty("isFile", true);

	/*
	public static final View publicView = new View(File.class, PropertyView.Public,
		type, name, size, url, owner, path, isFile, visibleToPublicUsers, visibleToAuthenticatedUsers, includeInFrontendExport, isFavoritable, isTemplate
	);

	public static final View uiView = new View(File.class, PropertyView.Ui,
		type, relativeFilePath, size, url, parent, checksum, version, cacheForSeconds, owner, isFile, hasParent, includeInFrontendExport, isFavoritable, isTemplate
	);
	*/

	public static void onCreation(final File thisFile, final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

		final Folder parent = thisFile.getParent();

		if (Settings.FilesystemEnabled.getValue() && parent == null) {

			final Folder workingOrHomeDir = File.getCurrentWorkingDir(thisFile);
			if (workingOrHomeDir != null) {

				thisFile.setParent(workingOrHomeDir);
			}
		}
	}

	public static void onModification(final File thisFile, final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {

		synchronized (thisFile) {

			// save current security context
			final SecurityContext previousSecurityContext = thisFile.getSecurityContext();

			// replace with SU context
			thisFile.setSecurityContext(SecurityContext.getSuperUserInstance());

			// update metadata and parent as superuser
			FileHelper.updateMetadata(thisFile, new PropertyMap(StructrApp.key(File.class, "hasParent"), thisFile.getParent() != null));

			// restore previous security context
			thisFile.setSecurityContext(previousSecurityContext);
		}

		if (thisFile instanceof AbstractMinifiedFile) {

			((AbstractMinifiedFile)thisFile).triggerMinificationIfNeeded(modificationQueue);
		}
	}

	public static void onNodeCreation(final File thisFile) {

		final String uuid     = thisFile.getUuid();
		final String filePath = getDirectoryPath(uuid) + "/" + uuid;

		try {
			thisFile.unlockSystemPropertiesOnce();
			thisFile.setProperty(StructrApp.key(File.class, "relativeFilePath"), filePath);

		} catch (Throwable t) {

			logger.warn("Exception while trying to set relative file path {}: {}", new Object[]{filePath, t});

		}
	}

	public static void onNodeDeletion(final File thisFile) {

		String filePath = null;
		try {
			final String path = thisFile.getRelativeFilePath();
			if (path != null) {

				filePath = FileHelper.getFilePath(path);

				java.io.File toDelete = new java.io.File(filePath);

				if (toDelete.exists() && toDelete.isFile()) {

					toDelete.delete();
				}
			}

		} catch (Throwable t) {

			logger.debug("Exception while trying to delete file {}: {}", new Object[]{filePath, t});
		}
	}

	public static void afterCreation(final File thisFile, final SecurityContext securityContext) {

		try {

			final java.io.File fileOnDisk = thisFile.getFileOnDisk();
			if (fileOnDisk.exists()) {
				return;
			}

			fileOnDisk.getParentFile().mkdirs();

			try {

				fileOnDisk.createNewFile();

			} catch (IOException ex) {

				logger.error("Could not create file", ex);
				return;
			}

			FileHelper.updateMetadata(thisFile, new PropertyMap(StructrApp.key(File.class, "version"), 0));

		} catch (FrameworkException ex) {

			logger.error("Could not create file", ex);
		}

	}

	/*
	@Override
	public String getPath() {
		return FileHelper.getFolderPath(this);
	}

	public String getRelativeFilePath() {

		return getProperty(FileBase.relativeFilePath);

	}

	public String getUrl() {

		return getProperty(FileBase.url);

	}

	@Override
	public String getContentType() {

		return getProperty(FileBase.contentType);

	}

	@Override
	public Long getSize() {

		return getProperty(size);

	}

	public Long getChecksum() {

		return getProperty(checksum);

	}

	public String getFormattedSize() {

		return FileUtils.byteCountToDisplaySize(getSize());

	}
	*/

	public static void triggerMinificationIfNeeded(final File thisFile, final ModificationQueue modificationQueue) throws FrameworkException {

		final List<AbstractMinifiedFile> targets = thisFile.getProperty(StructrApp.key(File.class, "minificationTargets"));
		final PropertyKey<Integer> versionKey    = StructrApp.key(File.class, "version");

		if (!targets.isEmpty()) {

			// only run minification if the file version changed
			boolean versionChanged = false;

			for (ModificationEvent modState : modificationQueue.getModificationEvents()) {

				if (thisFile.getUuid().equals(modState.getUuid())) {

					versionChanged |= modState.getRemovedProperties().containsKey(versionKey);
					versionChanged |= modState.getModifiedProperties().containsKey(versionKey);
					versionChanged |= modState.getNewProperties().containsKey(versionKey);
				}
			}

			if (versionChanged) {

				for (AbstractMinifiedFile minifiedFile : targets) {

					try {

						minifiedFile.minify();

					} catch (IOException ex) {
						logger.warn("Could not automatically update minification target: ".concat(minifiedFile.getName()), ex);
					}

				}
			}
		}
	}

	/*

	@Override
	public InputStream getInputStream() {

		final String relativeFilePath = getRelativeFilePath();

		if (relativeFilePath != null) {

			final String filePath = FileHelper.getFilePath(relativeFilePath);

			FileInputStream fis = null;
			try {

				java.io.File fileOnDisk = new java.io.File(filePath);

				// Return file input stream
				fis = new FileInputStream(fileOnDisk);

				if (getProperty(isTemplate)) {

					boolean editModeActive = false;
					if (securityContext.getRequest() != null) {
						final String editParameter = securityContext.getRequest().getParameter("edit");
						if (editParameter != null) {
							editModeActive = !RenderContext.EditMode.NONE.equals(RenderContext.editMode(editParameter));
						}
					}

					if (!editModeActive) {

						final String content = IOUtils.toString(fis, "UTF-8");

						try {

							final String result = Scripting.replaceVariables(new ActionContext(securityContext), this, content);
							return IOUtils.toInputStream(result, "UTF-8");

						} catch (Throwable t) {

							logger.warn("Scripting error in {}:\n{}", getUuid(), content, t);
						}
					}
				}

				return fis;

			} catch (FileNotFoundException e) {
				logger.debug("File not found: {}", new Object[]{relativeFilePath});

				if (fis != null) {

					try {

						fis.close();

					} catch (IOException ignore) {}

				}
			} catch (IOException ex) {
				java.util.logging.Logger.getLogger(FileBase.class.getName()).log(Level.SEVERE, null, ex);
			}
		}

		return null;
	}

	public FileOutputStream getOutputStream() {
		return getOutputStream(true, false);
	}

	public FileOutputStream getOutputStream(final boolean notifyIndexerAfterClosing, final boolean append) {

		if (getProperty(isTemplate)) {

			logger.error("File is in template mode, no write access allowed: {}", path);
			return null;
		}

		final String path = getRelativeFilePath();
		if (path != null) {

			final String filePath = FileHelper.getFilePath(path);

			try {

				final java.io.File fileOnDisk = new java.io.File(filePath);
				fileOnDisk.getParentFile().mkdirs();

				// Return file output stream and save checksum and size after closing
				final FileOutputStream fos = new FileOutputStream(fileOnDisk, append) {

					private boolean closed = false;

					@Override
					public void close() throws IOException {

						if (closed) {
							return;
						}

						try (Tx tx = StructrApp.getInstance().tx()) {

							super.close();

							final String _contentType = FileHelper.getContentMimeType(FileBase.this);

							final PropertyMap changedProperties = new PropertyMap();
							changedProperties.put(checksum, FileHelper.getChecksum(FileBase.this));
							changedProperties.put(size, FileHelper.getSize(FileBase.this));
							changedProperties.put(contentType, _contentType);

							if (StringUtils.startsWith(_contentType, "image") || ImageHelper.isImageType(getProperty(name))) {
								changedProperties.put(NodeInterface.type, Image.class.getSimpleName());
							}

							unlockSystemPropertiesOnce();
							setProperties(securityContext, changedProperties);

							increaseVersion();

							if (notifyIndexerAfterClosing) {
								notifyUploadCompletion();
							}

							tx.success();

						} catch (Throwable ex) {

							logger.error("Could not determine or save checksum and size after closing file output stream", ex);

						}

						closed = true;
					}
				};

				return fos;

			} catch (FileNotFoundException e) {

				logger.error("File not found: {}", path);
			}

		}

		return null;

	}

	public java.io.File getFileOnDisk() {

		final String path = getRelativeFilePath();
		if (path != null) {

			return new java.io.File(FileHelper.getFilePath(path));
		}

		return null;
	}

	public Path getPathOnDisk() {

		final String path = getRelativeFilePath();
		if (path != null) {

			return Paths.get(FileHelper.getFilePath(path));
		}

		return null;
	}
	*/

	public static Map<String, Object> getFirstLines(final File thisFile, final Map<String, Object> parameters) {

		final LineAndSeparator ls        = File.getFirstLines(thisFile, File.getNumberOrDefault(parameters, "num", 3));
		final Map<String, Object> result = new LinkedHashMap<>();
		final String separator           = ls.getSeparator();

		switch (separator) {

			case "\n":
				result.put("separator", "LF");
				break;

			case "\r":
				result.put("separator", "CR");
				break;

			case "\r\n":
				result.put("separator", "CR+LF");
				break;
		}

		result.put("lines", ls.getLine());

		return result;
	}

	public static Map<String, Object> getCSVHeaders(final File thisFile, final Map<String, Object> parameters) throws FrameworkException {

		if ("text/csv".equals(thisFile.getContentType())) {

			final Map<String, Object> map       = new LinkedHashMap<>();
			final Function<Object, Object> func = Functions.get("get_csv_headers");

			if (func != null) {

				try {

					final Object[] sources = new Object[4];
					String delimiter       = ";";
					String quoteChar       = "\"";
					String recordSeparator = "\n";

					if (parameters != null) {

						if (parameters.containsKey("delimiter"))       { delimiter       = parameters.get("delimiter").toString(); }
						if (parameters.containsKey("quoteChar"))       { quoteChar       = parameters.get("quoteChar").toString(); }
						if (parameters.containsKey("recordSeparator")) { recordSeparator = parameters.get("recordSeparator").toString(); }
					}

					// allow web-friendly specification of line endings
					switch (recordSeparator) {

						case "CR+LF":
							recordSeparator = "\r\n";
							break;

						case "CR":
							recordSeparator = "\r";
							break;

						case "LF":
							recordSeparator = "\n";
							break;

						case "TAB":
							recordSeparator = "\t";
							break;
					}

					sources[0] = getFirstLines(thisFile, 1).getLine();
					sources[1] = delimiter;
					sources[2] = quoteChar;
					sources[3] = recordSeparator;

					map.put("headers", func.apply(new ActionContext(thisFile.getSecurityContext()), null, sources));

				} catch (UnlicensedException ex) {

					logger.warn("CSV module is not available.");
				}
			}

			return map;

		} else {

			throw new FrameworkException(400, "File format is not CSV");
		}
	}

	public static void doCSVImport(final File thisFile, final Map<String, Object> config) throws FrameworkException {

		final SecurityContext securityContext = thisFile.getSecurityContext();
		final CSVFileImportJob job            = new CSVFileImportJob(thisFile, securityContext.getUser(false), config);

		DataImportManager.getInstance().addJob(job);
	}

	public static String getXMLStructure(final File thisFile) throws FrameworkException {

		final String contentType = thisFile.getContentType();

		if ("text/xml".equals(contentType) || "application/xml".equals(contentType)) {

			try (final Reader input = new InputStreamReader(thisFile.getInputStream())) {

				final XMLStructureAnalyzer analyzer = new XMLStructureAnalyzer(input);
				final Gson gson                     = new GsonBuilder().setPrettyPrinting().create();

				return gson.toJson(analyzer.getStructure(100));

			} catch (XMLStreamException | IOException ex) {
				ex.printStackTrace();
			}
		}

		return null;
	}

	public static void doXMLImport(final File thisFile, final Map<String, Object> config) throws FrameworkException {

		final SecurityContext securityContext = thisFile.getSecurityContext();
		final XMLFileImportJob job = new XMLFileImportJob(thisFile, securityContext.getUser(false), config);

		DataImportManager.getInstance().addJob(job);
	}

	/**
	 * Returns the Folder entity for the current working directory,
	 * or the user's home directory as a fallback.
	 * @return
	*/
	static Folder getCurrentWorkingDir(final File thisFile) {

		final Principal _owner  = thisFile.getOwnerNode();
		Folder workingOrHomeDir = null;

		if (_owner != null && _owner instanceof User) {

			workingOrHomeDir = _owner.getProperty(User.workingDirectory);
			if (workingOrHomeDir == null) {

				workingOrHomeDir = _owner.getProperty(User.homeDirectory);
			}
		}

		return workingOrHomeDir;
	}

	static int getNumberOrDefault(final Map<String, Object> data, final String key, final int defaultValue) {

		final Object value = data.get(key);

		if (value != null) {

			// try number
			if (value instanceof Number) {
				return ((Number)value).intValue();
			}

			// try string
			if (value instanceof String) {
				try { return Integer.valueOf((String)value); } catch (NumberFormatException nex) {}
			}
		}

		return defaultValue;
	}

	static LineAndSeparator getFirstLines(final File thisFile, final int num) {

		final StringBuilder lines = new StringBuilder();
		int separator[]           = new int[10];
		int separatorLength       = 0;

		try (final BufferedReader reader = new BufferedReader(new InputStreamReader(thisFile.getInputStream(), "utf-8"))) {

			int[] buf = new int[10010];

			int ch          = reader.read();
			int i           = 0;
			int l           = 0;

			// break on file end or if max char or line count is reached
			while (ch != -1 && i < 10000 && l < num) {

				switch (ch) {

					// CR
					case 13:

						// take only first line ending separator into account
						if (separator.length < 1) {

							// collect first separator char
							separator[separatorLength++] = ch;
						}

						// check next char only in case of CR
						ch = reader.read();

						// next char is LF ?
						if (ch == 10) {

							// CR + LF as line ending, collect second separator char
							separator[separatorLength++] = ch;

						} else {

							// CR only - do nothing
						}

						// append LF as line ending for display purposes
						buf[i++] = '\n';

						// add line to output
						lines.append(new String(buf, 0, i));

						// reset buffer
						buf = new int[10010];
						i=0;
						l++;

						break;

					// LF
					case 10:

						// take only first line ending separator into account
						if (separator.length < 1) {

							// collect first separator char
							separator[separatorLength++] = ch;
						}

						// must be LF only because two LF have to be ignored as empty line
						buf[i++] = '\n';

						// add line to output
						lines.append(new String(buf, 0, i));

						// reset buffer
						buf = new int[10010];
						i=0;
						l++;

						break;

					default:

						// no CR, no LF: Just add char
						buf[i++] = ch;
						break;
				}

				ch = reader.read();
			}

			lines.append(new String(buf, 0, i));

		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return new LineAndSeparator(lines.toString(), new String(separator, 0, separatorLength));
	}

	/*
	@Override
	public List<GraphObject> getSyncData() throws FrameworkException {

		final List<GraphObject> data = super.getSyncData();

		// nodes
		data.add(getProperty(parent));
		data.add(getIncomingRelationship(Folders.class));

		return data;
	}
	*/

	// ----- nested classes -----
	static class LineAndSeparator {

		private String line      = null;
		private String separator = null;

		public LineAndSeparator(final String line, final String separator) {
			this.line      = line;
			this.separator = separator;
		}

		public String getLine() {
			return line;
		}

		public String getSeparator() {
			return separator;
		}
	}
}
