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
package org.structr.web.importer;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.xml.stream.XMLStreamException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.AccessMode;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Principal;
import org.structr.core.graph.Tx;
import org.structr.module.StructrModule;
import org.structr.module.xml.XMLModule;
import org.structr.rest.common.XMLHandler;
import org.structr.web.entity.FileBase;

public class ImprovedXMLFileImportJob extends ImportJob {

	private static final Logger logger = LoggerFactory.getLogger(ImprovedXMLFileImportJob.class.getName());

	private int index = 0;
	private String contentType;

	public ImprovedXMLFileImportJob(FileBase file, Principal user, Map<String, Object> configuration) throws FrameworkException {
		super(file, user, configuration);

		contentType = file.getContentType();
	}

	@Override
	boolean runInitialChecks() throws FrameworkException {

		if ( !("text/xml".equals(contentType) || "application/xml".equals(contentType)) ) {

			throw new FrameworkException(400, "Cannot import XML from file with content type " + contentType);

		} else {

			final StructrModule module = StructrApp.getConfiguration().getModules().get("xml");

			if (module == null || !(module instanceof XMLModule) ) {

				throw new FrameworkException(400, "Cannot import XML, XML module is not available.");

			}

		}

		return true;
	}

	@Override
	public Runnable getRunnable() {

		return () -> {

			logger.info("Importing XML from {} ({})..", filePath, fileUuid);

			final SecurityContext threadContext = SecurityContext.getInstance(user, AccessMode.Backend);
			final App app                       = StructrApp.getInstance(threadContext);
			int overallCount                    = 0;

			// disable transaction notifications
			threadContext.disableModificationOfAccessTime();
			threadContext.ignoreResultCount(true);
			threadContext.setDoTransactionNotifications(false);
			threadContext.disableEnsureCardinality();

			try (final InputStream is = getFileInputStream(threadContext)) {

				try (final Reader reader = new InputStreamReader(is)) {

					reportBegin();

					final Iterator<Map<String, Object>> iterator = new XMLHandler(configuration, reader);
					final int batchSize                          = 100;
					int chunks                                   = 0;

					final long startTime = System.currentTimeMillis();

					while (iterator.hasNext()) {

						final long chunkStartTime = System.currentTimeMillis();

						int count = 0;

						try (final Tx tx = app.tx()) {

							while (iterator.hasNext() && ++count <= batchSize) {

								final Map<String, Object> params = new LinkedHashMap<>();
								final StringBuilder queryBuffer  = new StringBuilder();

								buildStatement(queryBuffer, params, iterator.next());

								final long t0 = System.currentTimeMillis();
								app.cypher(queryBuffer.toString(), params);
								final long t1 = System.currentTimeMillis();

								System.out.println((t1-t0) + "ms");

								overallCount++;
							}

							tx.success();

							chunks++;

							chunkFinished(chunkStartTime, chunks, batchSize, overallCount);

						}

						// do this outside of the transaction!
						shouldPause();
						if (shouldAbort()) {
							return;
						}

					}

					importFinished(startTime, overallCount);

				} catch (XMLStreamException | FrameworkException ex) {
					reportException(ex);
				}

			} catch (IOException ex) {
				reportException(ex);
			}

			jobFinished();
		};
	}

	@Override
	public String getImportType() {
		return "XML";
	}

	@Override
	public String getImportStatusType() {
		return "FILE_IMPORT_STATUS";
	}

	@Override
	public String getImportExceptionMessageType() {
		return "FILE_IMPORT_EXCEPTION";
	}

	// ----- private methods -----
	private void buildStatement(final StringBuilder buf, final Map<String, Object> parameters, final Map<String, Object> input) {
		handleMap(buf, parameters, input, null, 0);

		System.out.println("Statement:  " + buf.toString());
	}

	private void handleMap(final StringBuilder buf, final Map<String, Object> parameters, final Map<String, Object> input, final String parentKey, final int level) {

		final Map<String, Object> currentObjectValues = new LinkedHashMap<>();

		for (final Entry<String, Object> entry : input.entrySet()) {

			final String key   = entry.getKey();
			final Object value = entry.getValue();

			if (value instanceof Map) {

				// nested object (cardinality: single)
				handleMap(buf, parameters, (Map)value, key, level+1);
				continue;
			}

			if (value instanceof List) {

				// nested objects (cardinality: multiple)
				handleList(buf, parameters, (List)value, key, level+1);
				continue;
			}

			// store for use in CREATE statement
			currentObjectValues.put(key, value);
		}

		if (parentKey == null) {

			buf.append("MERGE (patent:Patent ");
			buf.append(formatMapForLiteralCypher(currentObjectValues));
			buf.append(")");

			for (final String key : parameters.keySet()) {

				final String relType = key;

				buf.append(" WITH patent UNWIND $");
				buf.append(key);
				buf.append(" AS data MERGE (patent)-[:");
				buf.append(relType);
				buf.append("]->(n { ");

				final Set<String> keys         = getKeys((List<Map<String, Object>>)parameters.get(key));
				for (final Iterator<String> it = keys.iterator(); it.hasNext();) {

					final String name = it.next();

					buf.append(name);
					buf.append(": COALESCE(data.");
					buf.append(name);
					buf.append(", '')");

					if (it.hasNext()) {
						buf.append(", ");
					}
				}

				buf.append(" })");
			}

		} else {

			List<Map<String, Object>> list = (List<Map<String, Object>>)parameters.get(parentKey);
			if (list == null) {

				list = new LinkedList<>();
				parameters.put(parentKey, list);
			}

			list.add(currentObjectValues);
		}
	}

	private void handleList(final StringBuilder buf, final Map<String, Object> parameters, final List input, final String parentKey, final int level) {

		for (final Object value : input) {

			if (value instanceof Map) {
				handleMap(buf, parameters, (Map)value, parentKey, level+1);
				continue;
			}

			if (value instanceof List) {
				handleList(buf, parameters, (List)value, parentKey, level+1);
				continue;
			}
		}
	}

	private String formatMapForLiteralCypher(final Map<String, Object> data) {

		final StringBuilder buf = new StringBuilder("{ ");

		for (final Iterator<Entry<String, Object>> iterator = data.entrySet().iterator(); iterator.hasNext();) {

			final Entry<String, Object> entry = iterator.next();
			final String key   = entry.getKey();
			final String value = getString(entry.getValue());

			buf.append(key);
			buf.append(" : ");
			buf.append(value);

			if (iterator.hasNext()) {
				buf.append(", ");
			}
		}

		buf.append(" }");

		return buf.toString();
	}

	private String getString(final Object value) {

		if (value instanceof String) {

			return "'" + value.toString().replaceAll("[']+", "") + "'";

		}

		return value.toString();
	}

	private Set<String> getKeys(final List<Map<String, Object>> list) {

		final Set<String> keys = new LinkedHashSet<>();

		for (final Map<String, Object> map : list) {
			keys.addAll(map.keySet());
		}

		return keys;
	}

}
