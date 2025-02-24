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
package org.structr.web.importer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.AccessMode;
import org.structr.common.ContextStore;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Principal;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.StructrTraits;
import org.structr.module.StructrModule;
import org.structr.module.xml.XMLModule;
import org.structr.rest.common.XMLHandler;
import org.structr.web.entity.File;

import javax.xml.stream.XMLStreamException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Iterator;
import java.util.Map;

public class XMLFileImportJob extends FileImportJob {

	private static final Logger logger = LoggerFactory.getLogger(XMLFileImportJob.class.getName());

	private String contentType;

	public XMLFileImportJob(final File file, final Principal user, final Map<String, Object> configuration, final ContextStore ctxStore) throws FrameworkException {
		super(file, user, configuration, ctxStore);

		contentType = file.getContentType();
	}

	@Override
	public boolean runInitialChecks() throws FrameworkException {

		if ( !("text/xml".equals(contentType) || "application/xml".equals(contentType)) ) {

			throw new FrameworkException(400, "Cannot import XML from file with content type " + contentType);

		} else {

			final StructrModule module = StructrApp.getConfiguration().getModules().get("xml");

			if (!Services.isTesting() && (module == null || !(module instanceof XMLModule))) {

				throw new FrameworkException(400, "Cannot import XML, XML module is not available.");

			}

		}

		return true;
	}

	@Override
	public boolean canRunMultiThreaded() {
		// think about this, maybe we can add parallelism here?
		return false;
	}

	@Override
	public Runnable getRunnable() {

		return () -> {

			logger.info("Importing XML from {} ({})..", filePath, fileUuid);

			final SecurityContext threadContext = SecurityContext.getInstance(user, AccessMode.Backend);
			threadContext.setContextStore(ctxStore);
			final App app                       = StructrApp.getInstance(threadContext);
			int overallCount                    = 0;

			// disable transaction notifications
			threadContext.disableModificationOfAccessTime();
			threadContext.setDoTransactionNotifications(false);
			threadContext.disablePreventDuplicateRelationships();

			// experimental: instruct deserialization strategies to set properties on related nodes
			threadContext.setAttribute("setNestedProperties", true);
			threadContext.setAttribute("batchType", configuration.get("batchType"));

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

						// test: open transaction
						Tx tx = app.tx();

						// make transaction available in context
						threadContext.setAttribute("currentTransaction", tx);

						while (iterator.hasNext() && ++count <= batchSize) {

							app.create(StructrTraits.NODE_INTERFACE, PropertyMap.inputTypeToJavaType(threadContext, iterator.next()));
							overallCount++;
						}

						// tx might have changed, reload from context
						tx = (Tx)threadContext.getAttribute("currentTransaction");
						tx.success();
						tx.close();

						chunks++;

						chunkFinished(chunkStartTime, chunks, batchSize, overallCount, 0);

						// do this outside of the transaction!
						shouldPause();
						if (shouldAbort()) {
							return;
						}

					}

					importFinished(startTime, overallCount, 0);

				} catch (XMLStreamException | FrameworkException ex) {
					reportException(ex);
				}

			} catch (Exception ex) {

				reportException(ex);

			} finally {

				jobFinished();
			}
		};
	}

	@Override
	public String getJobType() {
		return "XML";
	}

	@Override
	public String getJobStatusType() {
		return "FILE_IMPORT_STATUS";
	}

	@Override
	public String getJobExceptionMessageType() {
		return "FILE_IMPORT_EXCEPTION";
	}
}
