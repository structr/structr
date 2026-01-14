/*
 * Copyright (C) 2010-2026 Structr GmbH
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
package org.structr.core.graph;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.Predicate;
import org.structr.api.config.Settings;
import org.structr.api.service.Command;
import org.structr.common.Filter;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.Query;
import org.structr.core.app.StructrApp;
import org.structr.core.traits.Traits;
import org.structr.util.Writable;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Abstract base class for all graph service commands.
 */
public abstract class NodeServiceCommand extends Command {

	private static final Logger logger                        = LoggerFactory.getLogger(NodeServiceCommand.class.getName());
	private static final ArrayBlockingQueue<String> uuidQueue = new ArrayBlockingQueue<>(100000);

	protected final Map<String, String> customHeaders = new LinkedHashMap();
	protected SecurityContext securityContext         = null;
	private Writable logWritable                      = null;

	protected NodeServiceCommand() { }

	public Map<String, String> getCustomHeaders () {
		return customHeaders;
	}

	@Override
	public Class getServiceClass()	{
		return NodeService.class;
	}

	@Override
	public void initialized() {
		this.securityContext = (SecurityContext)getArgument("securityContext");
	}

	public <T> long bulkGraphOperation(final SecurityContext securityContext, final Query query, final int commitCount, String description, final BulkGraphOperation<T> operation) {

		final Predicate<Long> condition = operation.getCondition();
		final App app                   = StructrApp.getInstance(securityContext);
		final boolean doValidation      = operation.doValidation();
		final boolean doCallbacks       = operation.doCallbacks();
		final boolean doNotifications   = operation.doNotifications();
		long objectCount                = 0L;
		boolean active                  = true;
		int page                        = 1;

		if (query == null) {

			info("{}: {} objects processed", description, 0);
			return 0;
		}

		// set page size to commit count
		query.getQueryContext().overrideFetchSize(commitCount);
		query.pageSize(commitCount);

		while (active) {

			active = false;

			try (final Tx tx = app.tx(doValidation, doCallbacks, doNotifications)) {

				query.page(page++);

				final Iterable<T> iterable = query.getResultStream();
				final Iterator<T> iterator = iterable.iterator();

				while (iterator.hasNext() && (condition == null || condition.accept(objectCount))) {

					T node = iterator.next();
					active = true;

					try {

						boolean success = operation.handleGraphObject(securityContext, node);
						if (success) {
							objectCount++;
						}

					} catch (Throwable t) {

						operation.handleThrowable(securityContext, t, node);
					}

					// commit transaction after commitCount
					if ((objectCount % commitCount) == 0) {
						break;
					}
				}

				tx.success();

			} catch (Throwable t) {

				// bulk transaction failed, what to do?
				operation.handleTransactionFailure(securityContext, t);
			}

			if (description != null) {
				info("{}: {} objects processed", description, objectCount);
			}
		}

		return objectCount;
	}

	public <T> long bulkOperation(final SecurityContext securityContext, final Iterable<T> iterable, final int commitCount, String description, final BulkGraphOperation<T> operation) {

		final Predicate<Long> condition = operation.getCondition();
		final App app                   = StructrApp.getInstance(securityContext);
		final boolean doValidation      = operation.doValidation();
		final boolean doCallbacks       = operation.doCallbacks();
		final boolean doNotifications   = operation.doNotifications();
		final Iterator<T> iterator      = iterable.iterator();
		long objectCount                = 0L;
		boolean active                  = true;
		int page                        = 0;


		while (active) {

			active = false;

			try (final Tx tx = app.tx(doValidation, doCallbacks, doNotifications)) {

				while (iterator.hasNext() && (condition == null || condition.accept(objectCount))) {

					T node = iterator.next();
					active = true;

					try {

						boolean success = operation.handleGraphObject(securityContext, node);
						if (success) {
							objectCount++;
						}

					} catch (Throwable t) {

						operation.handleThrowable(securityContext, t, node);
					}

					// commit transaction after commitCount
					if ((objectCount % commitCount) == 0) {
						break;
					}
				}

				tx.success();

			} catch (Throwable t) {

				// bulk transaction failed, what to do?
				operation.handleTransactionFailure(securityContext, t);
			}

			if (description != null) {
				info("{}: {} objects processed", description, objectCount);
			}
		}

		return objectCount;
	}

	public void bulkTransaction(final SecurityContext securityContext, final long commitCount, final StructrTransaction transaction, final Predicate<Long> stopCondition) throws FrameworkException {

		final App app                = StructrApp.getInstance(securityContext);
		final AtomicLong objectCount = new AtomicLong(0L);

		if (stopCondition instanceof Filter) {
			((Filter)stopCondition).setSecurityContext(securityContext);
		}

		while (!stopCondition.accept(objectCount.get())) {

			try (final Tx tx = app.tx()) {

				long loopCount = 0;

				while (loopCount++ < commitCount && !stopCondition.accept(objectCount.get())) {

					transaction.execute();
					objectCount.incrementAndGet();
				}

				tx.success();
			}
		}
	}

	public static String getNextUuid() {

		String uuid = null;

		do {

			uuid = uuidQueue.poll();

		} while (uuid == null);

		return uuid;
	}

	// ----- public methods -----
	public void setLogBuffer(final Writable writable) {
		this.logWritable = writable;
	}

	// ----- protected methods -----
	protected void info(final String msg, final Object... data) {

		logger.info(msg, data);

		duplicateLogToConsole(msg, data);

	}

	protected void warn(final String msg, final Object... data) {

		logger.warn(msg, data);

		duplicateLogToConsole(msg, data);

	}

	protected void duplicateLogToConsole (final String msg, final Object... data) {

		// allow duplication of logging output to the console
		if (logWritable != null) {

			String logMessage = msg;

			for (final Object obj : data) {

				if (obj != null) {
					logMessage = logMessage.replaceFirst("\\{\\}", obj.toString());
				}
			}

			// alternative logging to a writer
			try {

				logWritable.println(logMessage);
				logWritable.flush();

			} catch (IOException ignore) {}

		}
	}

	protected Query getNodeQuery(final String nodeType, final boolean returnAllNodesQueryIfTypeNotFound) {

		if (nodeType != null) {

			final Traits traits = Traits.of(nodeType);
			if (traits != null) {


				return StructrApp.getInstance().nodeQuery(nodeType);
			}
		}

		if (returnAllNodesQueryIfTypeNotFound) {
			return StructrApp.getInstance().nodeQuery();
		}

		return null;
	}

	protected Query getRelationshipQuery(final String relationshipType, final boolean returnAllRelationshipsQueryIfTypeNotFound) {

		if (relationshipType != null) {

			final Traits traits = Traits.of(relationshipType);
			if (traits != null) {

				return StructrApp.getInstance().relationshipQuery(relationshipType);
			}
		}

		if (returnAllRelationshipsQueryIfTypeNotFound) {
			return StructrApp.getInstance().relationshipQuery();
		}

		return null;
	}

	// create uuid producer that fills the queue
	static {

		boolean createCompactUUIDsSettings = Settings.UUIDv4CreateCompact.getValue();
		final String configuredUUIDFormat  = Settings.UUIDv4AllowedFormats.getValue();
		boolean replaceDashes              = Settings.POSSIBLE_UUID_V4_FORMATS.without_dashes.toString().equals(configuredUUIDFormat) || (Settings.POSSIBLE_UUID_V4_FORMATS.both.toString().equals(configuredUUIDFormat) && createCompactUUIDsSettings);

		Thread uuidProducer = new Thread(new Runnable() {

			@Override
			public void run() {

				// please do not stop :)
				while (true) {

					try {

						while (true) {

							if (replaceDashes) {

								uuidQueue.put(StringUtils.replace(UUID.randomUUID().toString(), "-", ""));

							} else {

								uuidQueue.put(UUID.randomUUID().toString());
							}
						}

					} catch (Throwable t) {	}
				}
			}

		}, "UuidProducerThread");

		uuidProducer.setDaemon(true);
		uuidProducer.start();
	}
}
