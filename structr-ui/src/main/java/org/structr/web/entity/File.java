/**
 * Copyright (C) 2010-2018 Structr GmbH
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

import java.io.FileOutputStream;
import java.net.URI;
import javax.activation.DataSource;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import org.structr.cmis.CMISInfo;
import org.structr.cmis.info.CMISDocumentInfo;
import org.structr.common.ConstantBooleanTrue;
import org.structr.common.PropertyView;
import org.structr.common.error.FrameworkException;
import org.structr.common.fulltext.Indexable;
import org.structr.core.entity.Favoritable;
import org.structr.core.entity.Relation.Cardinality;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.graph.NodeInterface;
import org.structr.files.cmis.config.StructrFileActions;
import org.structr.schema.SchemaService;
import org.structr.schema.action.JavaScriptSource;
import org.structr.schema.json.JsonMethod;
import org.structr.schema.json.JsonObjectType;
import org.structr.schema.json.JsonReferenceType;
import org.structr.schema.json.JsonSchema;
import org.structr.web.property.FileDataProperty;

/**
 *
 *
 */
public interface File extends NodeInterface, Indexable, Linkable, JavaScriptSource, CMISInfo, CMISDocumentInfo, Favoritable, DataSource {

	static class Impl { static {

		final JsonSchema schema   = SchemaService.getDynamicSchema();
		final JsonObjectType type = schema.addType("File");

		type.setImplements(URI.create("https://structr.org/v1.1/definitions/File"));
		type.setImplements(URI.create("#/definitions/Indexable"));
		type.setImplements(URI.create("#/definitions/Linkable"));
		type.setImplements(URI.create("#/definitions/JavaScriptSource"));
		type.setImplements(URI.create("#/definitions/Favoritable"));
		type.setExtends(URI.create("#/definitions/AbstractFile"));

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

		type.addMethod("getFileOnDisk")
			.setReturnType(java.io.File.class.getName())
			.setSource("return " + File.class.getName() + ".getFileOnDisk(this);");

		type.addMethod("getFileOnDisk")
			.setReturnType(java.io.File.class.getName())
			.addParameter("doCreate", "boolean")
			.setSource("return " + File.class.getName() + ".getFileOnDisk(this, doCreate);");

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

	FileOutputStream getOutputStream(final boolean notifyIndexerAfterClosing, final boolean append);

	java.io.File getFileOnDisk(final boolean doCreate);
	java.io.File getFileOnDisk();

	boolean isTemplate();

	void notifyUploadCompletion();
	void increaseVersion() throws FrameworkException;
	void triggerMinificationIfNeeded(final ModificationQueue modificationQueue) throws FrameworkException;

	void setVersion(final int version) throws FrameworkException;
	Integer getVersion();

	Integer getCacheForSeconds();

	Long getChecksum();
	String getMd5();


	/*
	private static final Logger logger = LoggerFactory.getLogger(FileBase.class.getName());

	public static final Property<Long> size                                      = new LongProperty("size").indexed().systemInternal();
	public static final Property<Long> fileModificationDate                      = new LongProperty("fileModificationDate").indexed().systemInternal();
	public static final Property<String> url                                     = new StringProperty("url");
	public static final Property<Integer> cacheForSeconds                        = new IntProperty("cacheForSeconds").cmis();
	public static final Property<Integer> version                                = new IntProperty("version").indexed().systemInternal();
	public static final Property<String> base64Data                              = new FileDataProperty<>("base64Data");
	public static final Property<Boolean> isFile                                 = new ConstantBooleanProperty("isFile", true);
	public static final Property<List<AbstractMinifiedFile>> minificationTargets = new StartNodes<>("minificationTarget", MinificationSource.class);
	public static final Property<List<User>> favoriteOfUsers                     = new StartNodes<>("favoriteOfUsers", UserFavoriteFile.class);
	public static final Property<Boolean> isTemplate                             = new BooleanProperty("isTemplate");

	public static final Property<Long> checksum                                  = new LongProperty("checksum").indexed().unvalidated().systemInternal();
	public static final Property<String> md5                                     = new StringProperty("md5").unvalidated().systemInternal().readOnly();
	public static final Property<String> sha1                                    = new StringProperty("sha1").unvalidated().systemInternal().readOnly();
	public static final Property<String> sha512                                  = new StringProperty("sha512").unvalidated().systemInternal().readOnly();

	public static final View publicView = new View(FileBase.class, PropertyView.Public,
		type, name, size, url, owner, path, isFile, visibleToPublicUsers, visibleToAuthenticatedUsers, includeInFrontendExport, isFavoritable, isTemplate, fileModificationDate
	);

	public static final View uiView = new View(FileBase.class, PropertyView.Ui,
		type, size, url, parent, checksum, md5, version, cacheForSeconds, owner, isFile, hasParent, includeInFrontendExport, isFavoritable, isTemplate
	);

	@Override
	public boolean onCreation(final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

		if (super.onCreation(securityContext, errorBuffer)) {

			final PropertyMap changedProperties = new PropertyMap();

			if (Settings.FilesystemEnabled.getValue() && !getProperty(AbstractFile.hasParent)) {

				final Folder workingOrHomeDir = getCurrentWorkingDir();
				if (workingOrHomeDir != null && getProperty(AbstractFile.parent) == null) {

					changedProperties.put(AbstractFile.parent, workingOrHomeDir);
				}
			}

			changedProperties.put(hasParent, getProperty(parentId) != null);

			setProperties(securityContext, changedProperties);

			return true;
		}

		return false;
	}

	@Override
	public boolean onModification(final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {

		if (super.onModification(securityContext, errorBuffer, modificationQueue)) {

			synchronized (this) {

				// save current security context
				final SecurityContext previousSecurityContext = securityContext;

				// replace with SU context
				this.securityContext = SecurityContext.getSuperUserInstance();

				// update metadata and parent as superuser
				FileHelper.updateMetadata(this, new PropertyMap(hasParent, getProperty(parentId) != null), false);

				// restore previous security context
				this.securityContext = previousSecurityContext;
			}

			triggerMinificationIfNeeded(modificationQueue);

			return true;
		}

		return false;
	}

	@Override
	public void onNodeDeletion() {

		// only delete mounted files
		if (!isExternal()) {

			java.io.File toDelete = getFileOnDisk(false);

			try {

				if (toDelete.exists() && toDelete.isFile()) {

					toDelete.delete();
				}

			} catch (Throwable t) {
				logger.debug("Exception while trying to delete file {}: {}", toDelete, t);
			}
		}
	}

	@Override
	public void afterCreation(SecurityContext securityContext) {

		try {

			FileHelper.updateMetadata(this, new PropertyMap(version, 0));

		} catch (FrameworkException ex) {

			logger.error("Could not update metadata of {}: {}", this, ex.getMessage());
		}

	}

	@Export
	@Override
	public GraphObject getSearchContext(final String searchTerm, final int contextLength) {

		final String text = getProperty(extractedContent);
		if (text != null) {

			final FulltextIndexer indexer = StructrApp.getInstance(securityContext).getFulltextIndexer();
			return indexer.getContextObject(searchTerm, text, contextLength);
		}

		return null;
	}

	public void notifyUploadCompletion() {

		try {

			try (final Tx tx = StructrApp.getInstance().tx()) {

				synchronized (tx) {

					FileHelper.updateMetadata(this, new PropertyMap(), true);

					tx.success();
				}
			}

			final FulltextIndexer indexer = StructrApp.getInstance(securityContext).getFulltextIndexer();
			indexer.addToFulltextIndex(this);

		} catch (FrameworkException fex) {

			logger.warn("Unable to index {}: {}", this, fex.getMessage());
		}
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

	public String getMD5() {
		return getProperty(md5);
	}

	public String getFormattedSize() {
		return FileUtils.byteCountToDisplaySize(getSize());
	}

	public static String getDirectoryPath(final String uuid) {

		return (uuid != null)
			? uuid.substring(0, 1) + "/" + uuid.substring(1, 2) + "/" + uuid.substring(2, 3) + "/" + uuid.substring(3, 4)
			: null;

	}

	public void increaseVersion() throws FrameworkException {

		final Integer _version = getProperty(FileBase.version);

		unlockSystemPropertiesOnce();
		if (_version == null) {

			setProperties(securityContext, new PropertyMap(FileBase.version, 1));

		} else {

			setProperties(securityContext, new PropertyMap(FileBase.version, _version + 1));
		}
	}

	public void triggerMinificationIfNeeded(ModificationQueue modificationQueue) throws FrameworkException {

		final List<AbstractMinifiedFile> targets = getProperty(minificationTargets);

		if (!targets.isEmpty()) {

			// only run minification if the file version changed
			boolean versionChanged = false;
			for (ModificationEvent modState : modificationQueue.getModificationEvents()) {

				if (getUuid().equals(modState.getUuid())) {

					versionChanged = versionChanged ||
							modState.getRemovedProperties().containsKey(FileBase.version) ||
							modState.getModifiedProperties().containsKey(FileBase.version) ||
							modState.getNewProperties().containsKey(FileBase.version);
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

	@Override
	public InputStream getInputStream() {

		final java.io.File fileOnDisk = getFileOnDisk();

		try {

			final FileInputStream fis = new FileInputStream(fileOnDisk);

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

		} catch (IOException ex) {
			logger.warn("File not found: {}", fileOnDisk);
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

		try {

			// Return file output stream and save checksum and size after closing
			return new ClosingFileOutputStream(getFileOnDisk(), append, notifyIndexerAfterClosing);

		} catch (IOException e) {
			logger.error("Unable to open file output stream for {}: {}", path, e.getMessage());
		}

		return null;

	}

	public java.io.File getFileOnDisk() {
		return getFileOnDisk(true);
	}

	public java.io.File getFileOnDisk(final boolean create) {

		final Folder parentFolder = getProperty(FileBase.parent);
		if (parentFolder != null) {

			return parentFolder.getFileOnDisk(this, "", create);

		} else {

			return defaultGetFileOnDisk(this, create);
		}
	}

	@Export
	public Map<String, Object> getFirstLines(final Map<String, Object> parameters) {

		final Map<String, Object> result = new LinkedHashMap<>();
		final LineAndSeparator ls        = getFirstLines(getNumberOrDefault(parameters, "num", 3));
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

	@Export
	public Map<String, Object> getCSVHeaders(final Map<String, Object> parameters) throws FrameworkException {

		if ("text/csv".equals(getProperty(FileBase.contentType))) {

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

					sources[0] = getFirstLines(1).getLine();
					sources[1] = delimiter;
					sources[2] = quoteChar;
					sources[3] = recordSeparator;

					map.put("headers", func.apply(new ActionContext(securityContext), null, sources));

				} catch (UnlicensedException ex) {

					logger.warn("CSV module is not available.");
				}
			}

			return map;

		} else {

			throw new FrameworkException(400, "File format is not CSV");
		}
	}

	@Export
	public void doCSVImport(final Map<String, Object> parameters) throws FrameworkException {

		CSVFileImportJob job = new CSVFileImportJob(this, securityContext.getUser(false), parameters);
		JobQueueManager.getInstance().addJob(job);

	}

	@Export
	public String getXMLStructure() throws FrameworkException {

		final String contentType = getProperty(FileBase.contentType);

		if ("text/xml".equals(contentType) || "application/xml".equals(contentType)) {

			try (final Reader input = new InputStreamReader(getInputStream())) {

				final XMLStructureAnalyzer analyzer = new XMLStructureAnalyzer(input);
				final Gson gson                     = new GsonBuilder().setPrettyPrinting().create();

				return gson.toJson(analyzer.getStructure(100));

			} catch (XMLStreamException | IOException ex) {
				ex.printStackTrace();
			}
		}

		return null;
	}

	@Export
	public void doXMLImport(final Map<String, Object> config) throws FrameworkException {

		XMLFileImportJob job = new XMLFileImportJob(this, securityContext.getUser(false), config);
		JobQueueManager.getInstance().addJob(job);

	}

	// ----- private methods -----
	/**
	 * Returns the Folder entity for the current working directory,
	 * or the user's home directory as a fallback.
	 * @return
	private Folder getCurrentWorkingDir() {

		final Principal _owner  = getProperty(owner);
		Folder workingOrHomeDir = null;

		if (_owner != null && _owner instanceof User) {

			workingOrHomeDir = _owner.getProperty(User.workingDirectory);
			if (workingOrHomeDir == null) {

				workingOrHomeDir = _owner.getProperty(User.homeDirectory);
			}
		}

		return workingOrHomeDir;
	}

	private int getNumberOrDefault(final Map<String, Object> data, final String key, final int defaultValue) {

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

	private LineAndSeparator getFirstLines(final int num) {

		final StringBuilder lines = new StringBuilder();
		int separator[]           = new int[10];
		int separatorLength       = 0;

		try (final BufferedReader reader = new BufferedReader(new InputStreamReader(getInputStream(), "utf-8"))) {

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

	@Override
	public List<GraphObject> getSyncData() throws FrameworkException {

		final List<GraphObject> data = super.getSyncData();

		// nodes
		data.add(getProperty(parent));
		data.add(getIncomingRelationship(Folders.class));

		return data;
	}

	// ----- interface JavaScriptSource -----
	@Override
	public String getJavascriptLibraryCode() {

		try (final InputStream is = getInputStream()) {

			return IOUtils.toString(new InputStreamReader(is));

		} catch (IOException ioex) {
			logger.warn("", ioex);
		}

		return null;
	}

	// ----- CMIS support -----
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

	@Override
	public boolean isImmutable() {

		final Principal _owner = getOwnerNode();
		if (_owner != null) {

			return !_owner.isGranted(Permission.write, securityContext);
		}

		return true;
	}

	// ----- interface Favoritable -----
	@Override
	public String getContext() {
		return getProperty(FileBase.path);
	}

	@Override
	public String getFavoriteContent() {

		try (final InputStream is = getInputStream()) {

			return IOUtils.toString(is, "utf-8");

		} catch (IOException ioex) {
			logger.warn("Unable to get favorite content from {}: {}", this, ioex.getMessage());
		}

		return null;
	}

	@Override
	public String getFavoriteContentType() {
		return getContentType();
	}

	@Override
	public void setFavoriteContent(final String content) throws FrameworkException {

		try (final OutputStream os = getOutputStream(true, false)) {

			IOUtils.write(content, os, Charset.defaultCharset());
			os.flush();

		} catch (IOException ioex) {
			logger.warn("Unable to set favorite content from {}: {}", this, ioex.getMessage());
		}
	}

	// ----- nested classes -----
	private class LineAndSeparator {

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

	private class ClosingFileOutputStream extends FileOutputStream {

		private boolean notifyIndexerAfterClosing = false;
		private boolean closed                    = false;
		private java.io.File file                 = null;

		public ClosingFileOutputStream(final java.io.File file, final boolean append, final boolean notifyIndexerAfterClosing) throws IOException {

			super(file, append);

			this.notifyIndexerAfterClosing = notifyIndexerAfterClosing;
			this.file                      = file;
		}

		@Override
		public void close() throws IOException {

			if (closed) {
				return;
			}

			try (Tx tx = StructrApp.getInstance().tx()) {

				super.close();

				final String _contentType = FileHelper.getContentMimeType(FileBase.this);

				final PropertyMap changedProperties = new PropertyMap();
				changedProperties.put(checksum, FileHelper.getChecksum(file));
				changedProperties.put(size, FileHelper.getSize(file));
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
	}
	*/
}
