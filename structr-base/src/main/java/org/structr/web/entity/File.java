/*
 * Copyright (C) 2010-2023 Structr GmbH
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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.api.graph.PropertyContainer;
import org.structr.api.schema.JsonMethod;
import org.structr.api.schema.JsonObjectType;
import org.structr.api.schema.JsonSchema;
import org.structr.common.*;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.UnlicensedScriptException;
import org.structr.common.event.RuntimeEventLog;
import org.structr.common.fulltext.FulltextIndexer;
import org.structr.common.fulltext.Indexable;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Favoritable;
import org.structr.core.entity.Principal;
import org.structr.core.function.Functions;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.scheduler.JobQueueManager;
import org.structr.core.script.Scripting;
import org.structr.core.storage.StorageProviderFactory;
import org.structr.rest.common.XMLStructureAnalyzer;
import org.structr.schema.SchemaService;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.EvaluationHints;
import org.structr.schema.action.Function;
import org.structr.schema.action.JavaScriptSource;
import org.structr.web.common.ClosingFileOutputStream;
import org.structr.web.common.FileHelper;
import org.structr.web.common.RenderContext;
import org.structr.web.importer.CSVFileImportJob;
import org.structr.web.importer.MixedCSVFileImportJob;
import org.structr.web.importer.XMLFileImportJob;
import org.structr.web.property.FileDataProperty;

import javax.activation.DataSource;
import javax.xml.stream.XMLStreamException;
import java.io.*;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 *
 *
 */
public interface File extends AbstractFile, Indexable, Linkable, JavaScriptSource, Favoritable, DataSource {

	static class Impl { static {

		final JsonSchema schema   = SchemaService.getDynamicSchema();
		final JsonObjectType type = schema.addType("File");

		type.setImplements(URI.create("https://structr.org/v1.1/definitions/File"));
		type.setImplements(URI.create("#/definitions/Indexable"));
		type.setImplements(URI.create("#/definitions/Linkable"));
		type.setImplements(URI.create("#/definitions/JavaScriptSource"));
		type.setImplements(URI.create("#/definitions/Favoritable"));
		type.setExtends(URI.create("#/definitions/AbstractFile"));
		type.setCategory("ui");

		type.addStringProperty("url", PropertyView.Public, PropertyView.Ui);

		type.addBooleanProperty("isFile",            PropertyView.Public, PropertyView.Ui).setReadOnly(true).addTransformer(ConstantBooleanTrue.class.getName());
		type.addBooleanProperty("isTemplate",        PropertyView.Public, PropertyView.Ui);
		type.addBooleanProperty("indexed",           PropertyView.Public, PropertyView.Ui);
		type.addLongProperty("size",                 PropertyView.Public, PropertyView.Ui).setIndexed(true);
		type.addLongProperty("fileModificationDate", PropertyView.Public);

		type.addIntegerProperty("cacheForSeconds", PropertyView.Ui);
		type.addBooleanProperty("dontCache",       PropertyView.Public, PropertyView.Ui).setDefaultValue("false");
		type.addIntegerProperty("version",         PropertyView.Ui).setIndexed(true);
		type.addLongProperty("checksum",           PropertyView.Ui).setIndexed(true);
		type.addStringProperty("md5",              PropertyView.Ui);
		type.addLongProperty("crc32").setIndexed(true);
		type.addStringProperty("sha1");
		type.addStringProperty("sha512");
		type.addIntegerProperty("position").setIndexed(true);

		type.addPropertyGetter("cacheForSeconds", Integer.class);
		type.addPropertyGetter("checksum", Long.class);
		type.addPropertyGetter("md5", String.class);

		type.addPropertyGetter("version", Integer.class);
		type.addPropertyGetter("contentType", String.class);
		type.addPropertyGetter("extractedContent", String.class);
		type.addPropertyGetter("basicAuthRealm", String.class);
		type.addPropertyGetter("size", Long.class);

		type.addCustomProperty("base64Data", FileDataProperty.class.getName()).setTypeHint("String");

		// override setProperty methods, but don't call super first (we need the previous value)
		type.overrideMethod("setProperty",                 false,  "if (parentProperty.equals(arg0)) { " + File.class.getName() + ".checkMoveBinaryContents(this, arg0, arg1); }\n\t\treturn super.setProperty(arg0, arg1, false);");
		type.overrideMethod("setProperties",               false,  "if (arg1.containsKey(parentProperty)) { " + File.class.getName() + ".checkMoveBinaryContents(this, parentProperty, arg1.get(parentProperty)); }\n\t\tsuper.setProperties(arg0, arg1, arg2);")
				// the following lines make the overridden setProperties method more explicit in regards to its parameters
				.setReturnType("void")
				.addParameter("arg0", SecurityContext.class.getName())
				.addParameter("arg1", "java.util.Map<java.lang.String, java.lang.Object>")
				.addParameter("arg2", "boolean")
				.addException(FrameworkException.class.getName());

		type.overrideMethod("onCreation",                  true,  File.class.getName() + ".onCreation(this, arg0, arg1);");
		type.overrideMethod("onModification",              true,  File.class.getName() + ".onModification(this, arg0, arg1, arg2);");
		type.overrideMethod("onNodeDeletion",              true,  File.class.getName() + ".onNodeDeletion(this);");
		type.overrideMethod("afterCreation",               true,  File.class.getName() + ".afterCreation(this, arg0);");

		type.overrideMethod("isTemplate",                  false, "return getProperty(isTemplateProperty);");
		type.overrideMethod("setVersion",                  false, "setProperty(versionProperty, arg0);").addException(FrameworkException.class.getName());

		type.overrideMethod("increaseVersion",             false, File.class.getName() + ".increaseVersion(this);");
		type.overrideMethod("notifyUploadCompletion",      false, File.class.getName() + ".notifyUploadCompletion(this);");
		type.overrideMethod("callOnUploadHandler",         false, File.class.getName() + ".callOnUploadHandler(this, arg0);");

		type.overrideMethod("getInputStream",              false, "return " + File.class.getName() + ".getInputStream(this);");
		type.overrideMethod("getSearchContext",            false, "return " + File.class.getName() + ".getSearchContext(this, arg0, arg1, arg2);");
		type.overrideMethod("getJavascriptLibraryCode",    false, "return " + File.class.getName() + ".getJavascriptLibraryCode(this);");
		type.overrideMethod("getEnableBasicAuth",          false, "return getProperty(enableBasicAuthProperty);");

		// Favoritable
		type.overrideMethod("getContext",                  false, "return getPath();");
		type.overrideMethod("getFavoriteContentType",      false, "return getContentType();");
		type.overrideMethod("setFavoriteContent",          false, File.class.getName() + ".setFavoriteContent(this, arg0);");
		type.overrideMethod("getFavoriteContent",          false, "return " + File.class.getName() + ".getFavoriteContent(this);");
		type.overrideMethod("getCurrentWorkingDir",        false, "return " + File.class.getName() + ".getCurrentWorkingDir(this);");

		// overridden methods
		final JsonMethod getOutputStream1 = type.addMethod("getOutputStream");
		getOutputStream1.setSource("return " + File.class.getName() + ".getOutputStream(this, notifyIndexerAfterClosing, append);");
		getOutputStream1.addParameter("notifyIndexerAfterClosing", "boolean");
		getOutputStream1.addParameter("append", "boolean");
		getOutputStream1.setReturnType(FileOutputStream.class.getName());

		final JsonMethod getOutputStream2 = type.addMethod("getOutputStream");
		getOutputStream2.setSource("return " + File.class.getName() + ".getOutputStream(this, true, false);");
		getOutputStream2.setReturnType(FileOutputStream.class.getName());

		type.addMethod("doCSVImport")
			.setReturnType(java.lang.Long.class.getName())
			.addParameter("ctx", SecurityContext.class.getName())
			.addParameter("parameters", "java.util.Map<java.lang.String, java.lang.Object>")
			.setSource("return " + File.class.getName() + ".doCSVImport(this, parameters, ctx);")
			.addException(FrameworkException.class.getName())
			.setDoExport(true);


		type.addMethod("doXMLImport")
			.addParameter("ctx", SecurityContext.class.getName())
			.addParameter("parameters", "java.util.Map<java.lang.String, java.lang.Object>")
			.setReturnType(java.lang.Long.class.getName())
			.setSource("return " + File.class.getName() + ".doXMLImport(this, parameters, ctx);")
			.addException(FrameworkException.class.getName())
			.setDoExport(true);

		type.addMethod("getFirstLines")
			.addParameter("ctx", SecurityContext.class.getName())
			.addParameter("parameters", "java.util.Map<java.lang.String, java.lang.Object>")
			.setReturnType("java.util.Map<java.lang.String, java.lang.Object>")
			.setSource("return " + File.class.getName() + ".getFirstLines(this, parameters, ctx);")
			.setDoExport(true);

		type.addMethod("getCSVHeaders")
			.addParameter("ctx", SecurityContext.class.getName())
			.addParameter("parameters", "java.util.Map<java.lang.String, java.lang.Object>")
			.setReturnType("java.util.Map<java.lang.String, java.lang.Object>")
			.setSource("return " + File.class.getName() + ".getCSVHeaders(this, parameters, ctx);")
			.addException(FrameworkException.class.getName())
			.setDoExport(true);

		type.addMethod("getXMLStructure")
			.addParameter("ctx", SecurityContext.class.getName())
			.setReturnType("java.lang.String")
			.setSource("return " + File.class.getName() + ".getXMLStructure(this);")
			.addException(FrameworkException.class.getName())
			.setDoExport(true);

		type.addMethod("extractStructure")
			.addParameter("ctx", SecurityContext.class.getName())
			.addParameter("parameters", "java.util.Map<java.lang.String, java.lang.Object>")
			.setReturnType("java.util.Map<java.lang.String, java.lang.Object>")
			.setSource("return " + File.class.getName() + ".extractStructure(this);")
			.addException(FrameworkException.class.getName())
			.setDoExport(true);

		// view configuration
		type.addViewProperty(PropertyView.Public, "includeInFrontendExport");
		type.addViewProperty(PropertyView.Public, "owner");

		type.addViewProperty(PropertyView.Ui, "hasParent");
		type.addViewProperty(PropertyView.Ui, "path");

	}}

	String getXMLStructure(final SecurityContext securityContext) throws FrameworkException;
	Long doCSVImport(final SecurityContext securityContext, final Map<String, Object> parameters) throws FrameworkException;
	Long doXMLImport(final SecurityContext securityContext, final Map<String, Object> parameters) throws FrameworkException;
	Map<String, Object> getCSVHeaders(final SecurityContext securityContext, final Map<String, Object> parameters) throws FrameworkException;
	Map<String, Object> getFirstLines(final SecurityContext securityContext, final Map<String, Object> parameters);

	FileOutputStream getOutputStream(final boolean notifyIndexerAfterClosing, final boolean append);

	boolean isTemplate();

	void notifyUploadCompletion();
	void callOnUploadHandler(final SecurityContext ctx);
	void increaseVersion() throws FrameworkException;

	void setVersion(final int version) throws FrameworkException;
	Integer getVersion();

	Integer getCacheForSeconds();

	Long getChecksum();
	String getMd5();

	Folder getCurrentWorkingDir();

	static void onCreation(final File thisFile, final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

		if (Settings.FilesystemEnabled.getValue() && !thisFile.getHasParent()) {

			final Folder workingOrHomeDir = thisFile.getCurrentWorkingDir();
			if (workingOrHomeDir != null && thisFile.getParent() == null) {

				thisFile.setParent(workingOrHomeDir);
			}
		}
	}

	static void onModification(final File thisFile, final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {

		synchronized (thisFile) {

			// save current security context
			final SecurityContext previousSecurityContext = securityContext;

			// replace with SU context
			thisFile.setSecurityContext(SecurityContext.getSuperUserInstance());

			// update metadata and parent as superuser
			FileHelper.updateMetadata(thisFile, false);

			// restore previous security context
			thisFile.setSecurityContext(previousSecurityContext);

			// acknowledge all events for this node when it is modified
			final String uuid = thisFile.getUuid();
			if (uuid != null) {
				RuntimeEventLog.getEvents(e -> uuid.equals(e.getData().get("id"))).stream().forEach(e -> e.acknowledge());
			}
		}
	}

	static void onNodeDeletion(final File thisFile) {

		// only delete mounted files
		if (!thisFile.isExternal()) {

			StorageProviderFactory.getStreamProvider(thisFile).delete();
		}
	}

	static void afterCreation(final File thisFile, final SecurityContext securityContext) throws FrameworkException {

		try {

			FileHelper.updateMetadata(thisFile);
			thisFile.setVersion(0);

		} catch (FrameworkException ex) {

			final Logger logger = LoggerFactory.getLogger(File.class);
			logger.error("Could not update metadata of {}: {}", thisFile.getPath(), ex.getMessage());
		}

	}

	static GraphObject getSearchContext(final File thisFile, final SecurityContext ctx, final String searchTerm, final int contextLength) {

		final String text = thisFile.getExtractedContent();
		if (text != null) {

			final FulltextIndexer indexer = StructrApp.getInstance(ctx).getFulltextIndexer();
			return indexer.getContextObject(searchTerm, text, contextLength);
		}

		return null;
	}

	static void notifyUploadCompletion(final File thisFile) {

		try {

			try (final Tx tx = StructrApp.getInstance().tx()) {

				synchronized (tx) {

					FileHelper.updateMetadata(thisFile, true);
					File.increaseVersion(thisFile);

					tx.success();
				}
			}

			// indexing can be controlled for each file separately
			if (File.doIndexing(thisFile)) {

				final FulltextIndexer indexer = StructrApp.getInstance().getFulltextIndexer();
				indexer.addToFulltextIndex(thisFile);
			}

		} catch (FrameworkException fex) {

			final Logger logger = LoggerFactory.getLogger(File.class);
			logger.warn("Unable to index {}: {}", thisFile, fex.getMessage());
		}
	}

	static void callOnUploadHandler(final File thisFile, final SecurityContext ctx) {

		try (final Tx tx = StructrApp.getInstance(ctx).tx()) {

			thisFile.invokeMethod(ctx, "onUpload", Collections.emptyMap(), false, new EvaluationHints());

			tx.success();

		} catch (FrameworkException fex) {

			final Logger logger = LoggerFactory.getLogger(File.class);
			logger.warn("Exception occurred in onUpload handler of {}: {}", thisFile, fex.getMessage());
		}
	}


	static String getFormattedSize(final File thisFile) {
		return FileUtils.byteCountToDisplaySize(
				StorageProviderFactory.getStreamProvider(thisFile).size()
		);
	}

	static void increaseVersion(final File thisFile) throws FrameworkException {

		final Integer _version = thisFile.getVersion();

		thisFile.unlockSystemPropertiesOnce();
		if (_version == null) {

			thisFile.setVersion(1);

		} else {

			thisFile.setVersion(_version + 1);
		}
	}

	static InputStream getInputStream(final File thisFile) {

		final Logger logger = LoggerFactory.getLogger(File.class);

		try {

			final InputStream is = StorageProviderFactory.getStreamProvider(thisFile).getInputStream();
			final SecurityContext securityContext = thisFile.getSecurityContext();

			if (thisFile.isTemplate()) {

				boolean editModeActive = false;
				if (securityContext.getRequest() != null) {

					final String editParameter = securityContext.getRequest().getParameter(RequestKeywords.EditMode.keyword());
					if (editParameter != null) {

						editModeActive = !RenderContext.EditMode.NONE.equals(RenderContext.getValidatedEditMode(securityContext.getUser(false), editParameter));
					}
				}

				if (!editModeActive) {

					final String content = IOUtils.toString(is, "UTF-8");

					// close input stream here
					is.close();

					try {

						final String result = Scripting.replaceVariables(new ActionContext(securityContext), thisFile, content, "getInputStream");

						String encoding = "UTF-8";

						final String cType = thisFile.getContentType();
						if (cType != null) {

							final String charset = StringUtils.substringAfterLast(cType, "charset=").trim().toUpperCase();
							try {
								if (!"".equals(charset) && Charset.isSupported(charset)) {
									encoding = charset;
								}
							} catch (IllegalCharsetNameException ice) {
								logger.warn("Charset is not supported '{}'. Using 'UTF-8'", charset);
							}
						}

						return IOUtils.toInputStream(result, encoding);

					} catch (Throwable t) {

						logger.warn("Scripting error in {}:\n{}\n{}", thisFile.getUuid(), content, t.getMessage());
					}
				}
			}

			return is;

		} catch (IOException ex) {
			logger.warn("Unable to open input stream for {}: {}", thisFile.getPath(), ex.getMessage());
		}

		return null;
	}

	static FileOutputStream getOutputStream(final File thisFile) {
		return thisFile.getOutputStream(true, false);
	}

	static FileOutputStream getOutputStream(final File thisFile, final boolean notifyIndexerAfterClosing, final boolean append) {

		if (thisFile.isTemplate()) {

			final Logger logger = LoggerFactory.getLogger(File.class);
			logger.error("File is in template mode, no write access allowed: {}", thisFile.getPath());

			return null;
		}

		try {

			// Return file output stream and save checksum and size after closing
			return new ClosingFileOutputStream(thisFile, append, notifyIndexerAfterClosing);

		} catch (IOException e) {

			final Logger logger = LoggerFactory.getLogger(File.class);
			logger.error("Unable to open file output stream for {}: {}", thisFile.getPath(), e.getMessage());
		}

		return null;

	}

	static Map<String, Object> getFirstLines(final File thisFile, final Map<String, Object> parameters, final SecurityContext securityContext) {

		final Map<String, Object> result = new LinkedHashMap<>();
		final int num                    = File.getNumberOrDefault(parameters, "num", 3);
		final LineAndSeparator ls        = File.getFirstLines(thisFile, num, securityContext);
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

	static Map<String, Object> getCSVHeaders(final File thisFile, final Map<String, Object> parameters, final SecurityContext securityContext) throws FrameworkException {

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

					sources[0] = File.getFirstLines(thisFile, 1, securityContext).getLine();
					sources[1] = delimiter;
					sources[2] = quoteChar;
					sources[3] = recordSeparator;

					map.put("headers", func.apply(new ActionContext(securityContext), null, sources));

				} catch (UnlicensedScriptException ex) {

					final Logger logger = LoggerFactory.getLogger(File.class);
					logger.warn("CSV module is not available.");
				}
			}

			return map;

		} else {

			throw new FrameworkException(400, "File format is not CSV");
		}
	}

	static Long doCSVImport(final File thisFile, final Map<String, Object> parameters, final SecurityContext securityContext) throws FrameworkException {

		final Map<String, Object> mixedMappings  = (Map<String, Object>)parameters.get("mixedMappings");
		final ContextStore contextStore          = securityContext.getContextStore();
		final Principal user                     = securityContext.getUser(false);
		final Object onFinishScript              = parameters.get("onFinish");

		if (mixedMappings != null) {

			final Map<String, String> mappings = (Map<String, String>)parameters.get("mappings");

			// split and duplicate configuration for each of the mapped types
			for (final Entry<String, Object> entry : mixedMappings.entrySet()) {

				final Map<String, Object> data       = (Map<String, Object>)entry.getValue();
				final Map<String, String> properties = (Map<String, String>)data.get("properties");

				mappings.putAll(properties);
			}

			MixedCSVFileImportJob job = new MixedCSVFileImportJob(thisFile, user, parameters, contextStore);
			job.setOnFinishScript(onFinishScript);
			JobQueueManager.getInstance().addJob(job);

			return job.jobId();

		} else {

			CSVFileImportJob job = new CSVFileImportJob(thisFile, user, parameters, contextStore);
			job.setOnFinishScript(onFinishScript);
			JobQueueManager.getInstance().addJob(job);

			return job.jobId();
		}
	}

	static String getXMLStructure(final File thisFile) throws FrameworkException {

		final String contentType = thisFile.getContentType();

		if ("text/xml".equals(contentType) || "application/xml".equals(contentType)) {

			try (final Reader input = new InputStreamReader(thisFile.getInputStream())) {

				final XMLStructureAnalyzer analyzer = new XMLStructureAnalyzer(input);
				final Gson gson                     = new GsonBuilder().setPrettyPrinting().create();

				return gson.toJson(analyzer.getStructure(100));

			} catch (XMLStreamException | IOException ex) {
				LoggerFactory.getLogger(File.class).error("{}", ExceptionUtils.getStackTrace(ex));
			}
		}

		return null;
	}

	static Long doXMLImport(final File thisFile, final Map<String, Object> config, final SecurityContext securityContext) throws FrameworkException {

		XMLFileImportJob job = new XMLFileImportJob(thisFile, securityContext.getUser(false), config, securityContext.getContextStore());
		job.setOnFinishScript(config.get("onFinish"));
		JobQueueManager.getInstance().addJob(job);

		return job.jobId();
	}

	/**
	 * Returns the Folder entity for the current working directory,
	 * or the user's home directory as a fallback.
	 * @return
	 */
	static Folder getCurrentWorkingDir(final File thisFile) {

		final Principal _owner  = thisFile.getProperty(owner);
		Folder workingOrHomeDir = null;

		if (_owner != null && _owner instanceof User) {

			final User user = (User)_owner;

			workingOrHomeDir = user.getWorkingDirectory();
			if (workingOrHomeDir == null) {

				workingOrHomeDir = user.getHomeDirectory();
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

	static LineAndSeparator getFirstLines(final File thisFile, final int num, final SecurityContext securityContext) {

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
			LoggerFactory.getLogger(File.class).error("{}", ExceptionUtils.getStackTrace(ex));
		}

		return new LineAndSeparator(lines.toString(), new String(separator, 0, separatorLength));
	}

	static void checkMoveBinaryContents(final File thisFile, final PropertyKey key, final Object value) {

		final Folder previousParent     = (Folder)thisFile.getProperty(key);
		final Folder newParent          = (Folder)value;
		final java.io.File previousFile = null;
		java.io.File newFile            = null;

		if (newParent != null && !newParent.equals(previousParent)) {

			//newFile = newParent.getFileOnDisk(thisFile, "", false);
		}

		if (previousFile != null && previousFile.exists() && newFile != null && !newFile.exists() && !previousFile.equals(newFile)) {

			final Logger logger = LoggerFactory.getLogger(File.class);

			//try {

				logger.info("Moving file {} from {} to {}..", previousFile, previousParent, newFile);

				//ToDo: Implement equivalent system for new storage providers.
				//Files.move(Path.of(previousFile.toURI()), Path.of(newFile.toURI()));

			/*
			} catch (IOException ioex) {
				logger.error(ExceptionUtils.getStackTrace(ioex));
			}
			 */
		}
	}

	static boolean doIndexing(final File thisFile) {

		// we need to use the low-level API here because BooleanProperty returns false if no value is set
		final PropertyContainer container = thisFile.getPropertyContainer();
		if (container != null) {

			if (container.hasProperty("indexed")) {

				return Boolean.TRUE.equals(container.getProperty("indexed"));

			} else {


			}
		}

		// default to setting in security context
		return thisFile.getSecurityContext().doIndexing();
	}

	static Map<String, Object> extractStructure(final File thisFile) throws FrameworkException {
		StructrApp.getInstance(thisFile.getSecurityContext()).getContentAnalyzer().analyzeContent(thisFile);
		return null;
	}

	// ----- interface JavaScriptSource -----
	static String getJavascriptLibraryCode(final File thisFile) {

		try (final InputStream is = thisFile.getInputStream()) {

			return IOUtils.toString(new InputStreamReader(is));

		} catch (IOException ioex) {

			final Logger logger = LoggerFactory.getLogger(File.class);
			logger.warn("", ioex);
		}

		return null;
	}
	static boolean isImmutable(final File thisFile) {

		final Principal _owner = thisFile.getOwnerNode();
		if (_owner != null) {

			return !_owner.isGranted(Permission.write, thisFile.getSecurityContext());
		}

		return true;
	}

	// ----- interface Favoritable -----
	static String getFavoriteContent(final File thisFile) {

		try (final InputStream is = thisFile.getInputStream()) {

			return IOUtils.toString(is, "utf-8");

		} catch (IOException ioex) {

			final Logger logger = LoggerFactory.getLogger(File.class);
			logger.warn("Unable to get favorite content from {}: {}", thisFile, ioex.getMessage());
		}

		return null;
	}

	static void setFavoriteContent(final File thisFile, final String content) throws FrameworkException {

		try (final OutputStream os = thisFile.getOutputStream(true, false)) {

			IOUtils.write(content, os, Charset.defaultCharset());
			os.flush();

		} catch (IOException ioex) {

			final Logger logger = LoggerFactory.getLogger(File.class);
			logger.warn("Unable to set favorite content from {}: {}", thisFile, ioex.getMessage());
		}
	}

	class LineAndSeparator {

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
