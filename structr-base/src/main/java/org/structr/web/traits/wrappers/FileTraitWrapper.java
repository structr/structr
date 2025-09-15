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
package org.structr.web.traits.wrappers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jakarta.servlet.ServletResponse;
import java.util.Optional;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.graph.PropertyContainer;
import org.structr.common.*;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.UnlicensedScriptException;
import org.structr.common.fulltext.FulltextIndexer;
import org.structr.core.GraphObject;
import org.structr.core.api.AbstractMethod;
import org.structr.core.api.Methods;
import org.structr.core.api.UnnamedArguments;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Principal;
import org.structr.core.function.Functions;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.scheduler.JobQueueManager;
import org.structr.core.script.Scripting;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.rest.common.XMLStructureAnalyzer;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.EvaluationHints;
import org.structr.schema.action.Function;
import org.structr.storage.StorageProvider;
import org.structr.storage.StorageProviderFactory;
import org.structr.storage.providers.local.LocalFSHelper;
import org.structr.web.common.ClosingOutputStream;
import org.structr.web.common.FileHelper;
import org.structr.web.common.RenderContext;
import org.structr.web.entity.AbstractFile;
import org.structr.web.entity.File;
import org.structr.web.entity.Folder;
import org.structr.web.entity.User;
import org.structr.web.importer.CSVFileImportJob;
import org.structr.web.importer.MixedCSVFileImportJob;
import org.structr.web.importer.XMLFileImportJob;
import org.structr.web.traits.definitions.AbstractFileTraitDefinition;
import org.structr.web.traits.definitions.FileTraitDefinition;
import org.structr.web.traits.operations.OnUploadCompletion;

import javax.xml.stream.XMLStreamException;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 *
 *
 */
public class FileTraitWrapper extends AbstractFileTraitWrapper implements File {

	public FileTraitWrapper(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	@Override
	public GraphObject getSearchContext(final SecurityContext ctx, final String searchTerm, final int contextLength) {

		final String text = getExtractedContent();
		if (text != null) {

			final FulltextIndexer indexer = StructrApp.getInstance(ctx).getFulltextIndexer();
			return indexer.getContextObject(searchTerm, text, contextLength);
		}

		return null;
	}

	@Override
	public void notifyUploadCompletion() {
		traits.getMethod(OnUploadCompletion.class).onUploadCompletion(this, getSecurityContext());
	}

	@Override
	public void callOnUploadHandler(final SecurityContext ctx) {

		try (final Tx tx = StructrApp.getInstance(ctx).tx()) {

			final AbstractMethod method = Methods.resolveMethod(traits, "onUpload");
			if (method != null) {

				method.execute(ctx, wrappedObject, new UnnamedArguments(), new EvaluationHints());
			}

			tx.success();

		} catch (FrameworkException fex) {

			final Logger logger = LoggerFactory.getLogger(File.class);
			logger.warn("Exception occurred in onUpload handler of {}: {}", this, fex.getMessage());
		}
	}


	@Override
	public String getFormattedSize() {
		return FileUtils.byteCountToDisplaySize(
				getSize()
		);
	}

	@Override
	public Long getSize() {
		return StorageProviderFactory.getStorageProvider(wrappedObject.as(AbstractFile.class)).size();
	}

	@Override
	public void increaseVersion() throws FrameworkException {

		final Integer _version = getVersion();

		wrappedObject.unlockSystemPropertiesOnce();
		if (_version == null) {

			setVersion(1);

		} else {

			setVersion(_version + 1);
		}
	}

	@Override
	public void setVersion(final int version) throws FrameworkException {
		wrappedObject.setProperty(traits.key(FileTraitDefinition.VERSION_PROPERTY), version);
	}

	@Override
	public Integer getVersion() {
		return wrappedObject.getProperty(traits.key(FileTraitDefinition.VERSION_PROPERTY));
	}

	@Override
	public Integer getCacheForSeconds() {
		return wrappedObject.getProperty(traits.key(FileTraitDefinition.CACHE_FOR_SECONDS_PROPERTY));
	}

	@Override
	public Long getChecksum() {
		return wrappedObject.getProperty(traits.key(FileTraitDefinition.CHECKSUM_PROPERTY));
	}

	@Override
	public Long getFileModificationDate() {
		return wrappedObject.getProperty(traits.key(FileTraitDefinition.FILE_MODIFICATION_DATE_PROPERTY));
	}

	@Override
	public String getMd5() {
		return wrappedObject.getProperty(traits.key(FileTraitDefinition.MD5_PROPERTY));
	}

	@Override
	public void setSize(final Long size) throws FrameworkException {
		wrappedObject.setProperty(traits.key(FileTraitDefinition.SIZE_PROPERTY), size);
	}

	@Override
	public boolean isTemplate() {
		return wrappedObject.getProperty(traits.key(FileTraitDefinition.IS_TEMPLATE_PROPERTY));
	}

	@Override
	public boolean dontCache() {
		return wrappedObject.getProperty(traits.key(FileTraitDefinition.DONT_CACHE_PROPERTY));
	}

	@Override
	public String getPath() {
		return wrappedObject.getProperty(traits.key(AbstractFileTraitDefinition.PATH_PROPERTY));
	}

	@Override
	public String getDiskFilePath(final SecurityContext securityContext) {

		final LocalFSHelper helper    = new LocalFSHelper(getStorageConfiguration());
		final java.io.File fileOnDisk = helper.getFileOnDisk(this);

		if (fileOnDisk != null) {

			return fileOnDisk.getAbsolutePath();
		}

		return null;
	}

	@Override
	public java.io.File getFileOnDisk() {

		final LocalFSHelper helper = new LocalFSHelper(getStorageConfiguration());

		return helper.getFileOnDisk(this);
	}

	@Override
	public InputStream getInputStream() {

		final Logger logger = LoggerFactory.getLogger(File.class);

		try {

			FileHelper.prefetchFileData(getUuid());

			final InputStream is = StorageProviderFactory.getStorageProvider(wrappedObject.as(AbstractFile.class)).getInputStream();
			final SecurityContext securityContext = wrappedObject.getSecurityContext();

			if (isTemplate()) {

				boolean editModeActive = false;
				if (securityContext.getRequest() != null) {

					final String editParameter = securityContext.getRequest().getParameter(RequestKeywords.EditMode.keyword());
					if (editParameter != null) {

						editModeActive = !RenderContext.EditMode.NONE.equals(RenderContext.getValidatedEditMode(securityContext.getUser(false), editParameter));
					}
				}

				if (!editModeActive) {

					final String content = IOUtils.toString(is, StandardCharsets.UTF_8);

					// close input stream here
					is.close();

					try {

						final String result = Scripting.replaceVariables(new ActionContext(securityContext), wrappedObject, content, "getInputStream");

						String encoding = "UTF-8";

						// if we have set a custom contentType response header in the script, use that - otherwise use the content-type of the file
						final String cType = Optional.ofNullable(getSecurityContext())
								.map(SecurityContext::getResponse)
								.map(ServletResponse::getContentType)
								.orElse(getContentType());

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

						logger.warn("Scripting error in {}:\n{}\n{}", getUuid(), content, t.getMessage());
					}
				}
			}

			return is;

		} catch (IOException ex) {
			logger.warn("Unable to open input stream for {}: {}", getPath(), ex.getMessage());
		}

		return null;
	}

	@Override
	public InputStream getRawInputStream() {

		FileHelper.prefetchFileData(getUuid());

		final InputStream is = StorageProviderFactory.getStorageProvider(wrappedObject.as(AbstractFile.class)).getInputStream();

		return is;
	}

	@Override
	public OutputStream getOutputStream() {
		return getOutputStream(true, false);
	}

	@Override
	public String getExtractedContent() {
		return wrappedObject.getProperty(traits.key(FileTraitDefinition.EXTRACTED_CONTENT_PROPERTY));
	}

	@Override
	public String getContentType() {
		return wrappedObject.getProperty(traits.key(FileTraitDefinition.CONTENT_TYPE_PROPERTY));
	}

	@Override
	public OutputStream getOutputStream(final boolean notifyIndexerAfterClosing, final boolean append) {

		if (isTemplate()) {

			final Logger logger = LoggerFactory.getLogger(File.class);
			logger.error("File is in template mode, no write access allowed: {}", getPath());

			return null;
		}

		try {

			// Return file output stream and save checksum and size after closing
			return new ClosingOutputStream(this, append, notifyIndexerAfterClosing);

		} catch (IOException e) {

			final Logger logger = LoggerFactory.getLogger(File.class);
			logger.error("Unable to open file output stream for {}: {}", getPath(), e.getMessage());
		}

		return null;

	}

	@Override
	public Map<String, Object> getFirstLines(final SecurityContext securityContext, final Map<String, Object> parameters) {

		final Map<String, Object> result = new LinkedHashMap<>();
		final int num                    = getNumberOrDefault(parameters, "num", 3);
		final LineAndSeparator ls        = getFirstLines(num);
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

	@Override
	public Map<String, Object> getCSVHeaders(final SecurityContext securityContext, final Map<String, Object> parameters) throws FrameworkException {

		if ("text/csv".equals(getContentType())) {

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

	@Override
	public Long doCSVImport(final SecurityContext securityContext, final Map<String, Object> parameters) throws FrameworkException {

		final Map<String, Object> mixedMappings = (Map<String, Object>)parameters.get("mixedMappings");
		final ContextStore contextStore         = securityContext.getContextStore();
		final Principal user                    = securityContext.getUser(false);
		final Object onFinishScript             = parameters.get("onFinish");

		if (mixedMappings != null) {

			final Map<String, String> mappings = (Map<String, String>)parameters.get("mappings");

			// split and duplicate configuration for each of the mapped types
			for (final Entry<String, Object> entry : mixedMappings.entrySet()) {

				final Map<String, Object> data       = (Map<String, Object>)entry.getValue();
				final Map<String, String> properties = (Map<String, String>)data.get("properties");

				mappings.putAll(properties);
			}

			MixedCSVFileImportJob job = new MixedCSVFileImportJob(this, user, parameters, contextStore);
			job.setOnFinishScript(onFinishScript);
			JobQueueManager.getInstance().addJob(job);

			return job.jobId();

		} else {

			CSVFileImportJob job = new CSVFileImportJob(this, user, parameters, contextStore);
			job.setOnFinishScript(onFinishScript);
			JobQueueManager.getInstance().addJob(job);

			return job.jobId();
		}
	}

	@Override
	public String getXMLStructure(final SecurityContext securityContext) throws FrameworkException {

		final String contentType = getContentType();

		if ("text/xml".equals(contentType) || "application/xml".equals(contentType)) {

			try (final Reader input = new InputStreamReader(getInputStream())) {

				final XMLStructureAnalyzer analyzer = new XMLStructureAnalyzer(input);
				final Gson gson                     = new GsonBuilder().setPrettyPrinting().create();

				return gson.toJson(analyzer.getStructure(100));

			} catch (XMLStreamException | IOException ex) {
				LoggerFactory.getLogger(File.class).error("{}", ExceptionUtils.getStackTrace(ex));
			}
		}

		return null;
	}

	@Override
	public Long doXMLImport(final SecurityContext securityContext, final Map<String, Object> config) throws FrameworkException {

		XMLFileImportJob job = new XMLFileImportJob(this, securityContext.getUser(false), config, securityContext.getContextStore());
		job.setOnFinishScript(config.get("onFinish"));
		JobQueueManager.getInstance().addJob(job);

		return job.jobId();
	}

	/**
	 * Returns the Folder entity for the current working directory,
	 * or the user's home directory as a fallback.
	 * @return
	 */
	@Override
	public Folder getCurrentWorkingDir() {

		final Principal _owner  = as(AccessControllable.class).getOwnerNode();
		Folder workingOrHomeDir = null;

		if (_owner != null && _owner.is(StructrTraits.USER)) {

			final User user = _owner.as(User.class);

			workingOrHomeDir = user.getWorkingDirectory();
			if (workingOrHomeDir == null) {

				workingOrHomeDir = user.getHomeDirectory();
			}
		}

		return workingOrHomeDir;
	}

	@Override
	public int getNumberOrDefault(final Map<String, Object> data, final String key, final int defaultValue) {

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

	@Override
	public void checkMoveBinaryContents(final NodeInterface newProvider) {

		final AbstractFile abstractFile = wrappedObject.as(AbstractFile.class);

		if (!StorageProviderFactory.getStorageProvider(abstractFile).equals(StorageProviderFactory.getSpecificStorageProvider(abstractFile, newProvider))) {

			final StorageProvider previousSP = StorageProviderFactory.getStorageProvider(abstractFile);
			final StorageProvider newSP      = StorageProviderFactory.getSpecificStorageProvider(abstractFile, newProvider);

			previousSP.moveTo(newSP);
		}
	}

	@Override
	public void checkMoveBinaryContents(final Folder previousParent, final NodeInterface newParent) {

		final StorageProvider previousSP = StorageProviderFactory.getSpecificStorageProvider(this, previousParent != null ? previousParent.getStorageConfiguration(): null);
		final StorageProvider newSP      = StorageProviderFactory.getSpecificStorageProvider(this, newParent != null ? newParent.as(Folder.class).getStorageConfiguration(): null);
		previousSP.moveTo(newSP);
	}

	@Override
	public boolean doIndexing() {

		// we need to use the low-level API here because BooleanProperty returns false if no value is set
		final PropertyContainer container = wrappedObject.getPropertyContainer();
		if (container != null) {

			if (container.hasProperty(FileTraitDefinition.INDEXED_PROPERTY)) {

				return Boolean.TRUE.equals(container.getProperty(FileTraitDefinition.INDEXED_PROPERTY));

			}
		}

		// default to setting in security context
		return wrappedObject.getSecurityContext().doIndexing();
	}

	@Override
	public boolean isImmutable() {

		final Principal _owner = as(AccessControllable.class).getOwnerNode();
		if (_owner != null) {

			return !_owner.isGranted(Permission.write, wrappedObject.getSecurityContext());
		}

		return true;
	}

	@Override
	public String getName() {
		return wrappedObject.getName();
	}

	// ----- private methods -----
	private LineAndSeparator getFirstLines(final int num) {

		final StringBuilder lines = new StringBuilder();
		int[] separator = new int[10];
		int separatorLength       = 0;

		try (final BufferedReader reader = new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8))) {

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
}
