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
package org.structr.core.graph;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.*;
import org.structr.api.graph.Node;
import org.structr.api.graph.PropertyContainer;
import org.structr.api.graph.Relationship;
import org.structr.common.SecurityContext;
import org.structr.common.error.DatabaseServiceNetworkException;
import org.structr.common.error.DatabaseServiceNotAvailableException;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.event.RuntimeEventLog;
import org.structr.core.GraphObject;
import org.structr.core.Services;
import org.structr.core.StructrTransactionListener;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Principal;
import org.structr.core.property.PropertyKey;
import org.structr.core.scheduler.TransactionPostProcessQueue;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Graph service command for database operations that need to be wrapped in
 * a transaction.
 */
public class TransactionCommand {

	private static final Logger logger                             = LoggerFactory.getLogger(TransactionCommand.class.getName());
	private static final Set<StructrTransactionListener> listeners = new LinkedHashSet<>();
	private static final ThreadLocal<TransactionCommand> commands  = new ThreadLocal<>();
	private static final MultiSemaphore                  semaphore = new MultiSemaphore();

	private TransactionReference transaction             = null;
	private ModificationQueue queue                      = null;
	private ErrorBuffer errorBuffer                      = null;
	private TransactionPostProcessQueue postProcessQueue = null;


	private static TransactionCommand getInstance() {

		TransactionCommand cmd = commands.get();
		if (cmd == null) {

			cmd = new TransactionCommand();
			cmd.postProcessQueue = new TransactionPostProcessQueue();
		}

		return cmd;
	}

	public static TransactionCommand beginTx(final SecurityContext securityContext) throws FrameworkException {

		final TransactionCommand cmd  = TransactionCommand.getInstance();
		final DatabaseService graphDb = Services.getInstance().getDatabaseService();

		if (graphDb != null) {

			if (cmd.transaction == null) {

				try {
					// start new transaction
					cmd.transaction = new TransactionReference(graphDb.beginTx());

				} catch (NetworkException nex) {
					throw new DatabaseServiceNetworkException(503, nex.getMessage());
				}

				cmd.queue = new ModificationQueue(cmd.transaction.getTransactionId(), Thread.currentThread().getName());
				cmd.errorBuffer = new ErrorBuffer();

				commands.set(cmd);
			}

			// increase depth
			cmd.transaction.begin();

		} else {

			throw new DatabaseServiceNotAvailableException(503, "Database service is not available, ensure the database is running and that there is a working network connection to it");
		}

		return cmd;
	}

	public static void commitTx(final SecurityContext securityContext, final boolean doValidation) throws FrameworkException {

		final TransactionCommand cmd  = TransactionCommand.getInstance();

		if (cmd.transaction != null && cmd.transaction.isToplevel()) {

			final ModificationQueue modificationQueue = cmd.queue;
			final ErrorBuffer errorBuffer             = cmd.errorBuffer;

			// 0.5: let transaction listeners examine (and prevent?) commit
			for (final StructrTransactionListener listener : listeners) {
				listener.beforeCommit(securityContext, modificationQueue.getModificationEvents());
			}

			// 1. do inner callbacks (may cause transaction to fail)
			if (securityContext == null || securityContext.doInnerCallbacks()) {

				if (!modificationQueue.doInnerCallbacks(securityContext, errorBuffer)) {

					if (modificationQueue != null && modificationQueue.getSize() > 0) {
						RuntimeEventLog.transaction("Failed", modificationQueue.getTransactionStats());
					}

					cmd.transaction.failure();
					throw new FrameworkException(422, "Unable to commit transaction, validation failed", errorBuffer);
				}
			}

			// 2. fetch all types of entities modified in this tx
			final Set<String> synchronizationKeys = modificationQueue.getSynchronizationKeys();

			if (securityContext != null && securityContext.uuidWasSetManually()) {

				if (synchronizationKeys != null) {

					synchronizationKeys.add("id");
				}
			}

			// we need to protect the validation and indexing part of every transaction
			// from being entered multiple times in the presence of validators
			// 3. acquire semaphores for each modified type
			try { semaphore.acquire(synchronizationKeys); } catch (InterruptedException iex) { return; }

			// do validation under the protection of the semaphores for each type
			if (!modificationQueue.doValidation(securityContext, errorBuffer, doValidation)) {

				cmd.transaction.failure();

				if (modificationQueue != null && modificationQueue.getSize() > 0) {
					RuntimeEventLog.transaction("Failed", modificationQueue.getTransactionStats());
				}

				// create error
				throw new FrameworkException(422, "Unable to commit transaction, validation failed", errorBuffer);
			}

			// finally: execute validatable post-transaction action
			if (!modificationQueue.doPostProcessing(securityContext, errorBuffer)) {

				cmd.transaction.failure();

				if (modificationQueue != null && modificationQueue.getSize() > 0) {
					RuntimeEventLog.transaction("Failed", modificationQueue.getTransactionStats());
				}

				throw new FrameworkException(422, "Unable to commit transaction, transaction post processing failed", errorBuffer);
			}

			try {
				cmd.transaction.success();

			} catch (Throwable t) {
				logger.error("Unable to commit transaction", t);
			}

			if (modificationQueue != null && modificationQueue.getSize() > 0) {
				RuntimeEventLog.transaction("Success", modificationQueue.getTransactionStats());
			}
		}
	}

	public static ModificationQueue finishTx() {

		final TransactionCommand cmd        = TransactionCommand.getInstance();
		ModificationQueue modificationQueue = null;

		if (cmd.transaction != null) {

			if (cmd.transaction.isToplevel()) {

				modificationQueue = cmd.queue;

				final Set<String> synchronizationKeys = modificationQueue.getSynchronizationKeys();

				// cleanup
				commands.remove();

				try {
					cmd.transaction.close();

				} finally {

					// release semaphores as the transaction is now finished
					semaphore.release(synchronizationKeys);	// careful: this can be null
					cmd.postProcessQueue.applyProcessQueue();
				}

				// copy transaction success status to modification queue
				modificationQueue.setTransactionWasSuccessful(cmd.transaction.isSuccessful());

			} else {

				cmd.transaction.end();
			}
		}

		return modificationQueue;
	}

	public static void disableChangelog() {

		TransactionCommand command = commands.get();
		if (command != null) {

			ModificationQueue modificationQueue = command.getModificationQueue();
			if (modificationQueue != null) {

				modificationQueue.disableChangelog();

			} else {

				logger.error("Got empty changeSet from command!");
			}

		} else {

			RuntimeEventLog.transaction("Not in transaction");
			throw new NotInTransactionException("Not in transaction.");
		}
	}

	public static void postProcess(final String key, final TransactionPostProcess process) {

		TransactionCommand command = commands.get();
		if (command != null) {

			ModificationQueue modificationQueue = command.getModificationQueue();
			if (modificationQueue != null) {

				modificationQueue.postProcess(key, process);

			} else {

				logger.error("Got empty changeSet from command!");
			}

		} else {

			RuntimeEventLog.transaction("Not in transaction");
			throw new NotInTransactionException("Not in transaction.");
		}

	}

	public static void nodeCreated(final Principal user, final NodeInterface node) {

		TransactionCommand command = commands.get();
		if (command != null) {

			assertSameTransaction(node, command.getTransactionId());

			TransactionCommand.getCurrentTransaction().setNodeIsCreated(node.getNode().getId().getId());

			ModificationQueue modificationQueue = command.getModificationQueue();
			if (modificationQueue != null) {

				modificationQueue.create(user, node);

			} else {

				logger.error("Got empty changeSet from command!");
			}

		} else {

			RuntimeEventLog.transaction("Not in transaction");
			throw new NotInTransactionException("Not in transaction.");
		}
	}

	public static void nodeModified(final Principal user, final NodeInterface node, final PropertyKey key, final Object previousValue, final Object newValue) {

		TransactionCommand command = commands.get();
		if (command != null) {

			//assertSameTransaction(node, command.getTransactionId());

			ModificationQueue modificationQueue = command.getModificationQueue();
			if (modificationQueue != null) {

				modificationQueue.modify(user, node, key, previousValue, newValue);

			} else {

				logger.error("Got empty changeSet from command!");
			}

		} else {

			RuntimeEventLog.transaction("Not in transaction");
			throw new NotInTransactionException("Not in transaction.");
		}
	}

	public static void nodeDeleted(final Principal user, final NodeInterface node) {

		TransactionCommand command = commands.get();
		if (command != null) {

			assertSameTransaction(node, command.getTransactionId());

			ModificationQueue modificationQueue = command.getModificationQueue();
			if (modificationQueue != null) {

				modificationQueue.delete(user, node);

			} else {

				logger.error("Got empty changeSet from command!");
			}

		} else {

			RuntimeEventLog.transaction("Not in transaction");
			throw new NotInTransactionException("Not in transaction.");
		}
	}

	public static void relationshipCreated(final Principal user, final RelationshipInterface relationship) {

		TransactionCommand command = commands.get();
		if (command != null) {

			assertSameTransaction(relationship, command.getTransactionId());

			ModificationQueue modificationQueue = command.getModificationQueue();
			if (modificationQueue != null) {

				modificationQueue.create(user, relationship);

			} else {

				logger.error("Got empty changeSet from command!");
			}

		} else {

			RuntimeEventLog.transaction("Not in transaction");
			throw new NotInTransactionException("Not in transaction.");
		}
	}

	public static void relationshipModified(final Principal user, final RelationshipInterface relationship, final PropertyKey key, final Object previousValue, final Object newValue) {

		TransactionCommand command = commands.get();
		if (command != null) {

			assertSameTransaction(relationship, command.getTransactionId());

			ModificationQueue modificationQueue = command.getModificationQueue();
			if (modificationQueue != null) {

				modificationQueue.modify(user, relationship, key, previousValue, newValue);

			} else {

				logger.error("Got empty changeSet from command!");
			}

		} else {

			RuntimeEventLog.transaction("Not in transaction");
			throw new NotInTransactionException("Not in transaction.");
		}
	}

	public static void relationshipDeleted(final Principal user, final RelationshipInterface relationship, final boolean passive) {

		TransactionCommand command = commands.get();
		if (command != null) {

			assertSameTransaction(relationship, command.getTransactionId());

			ModificationQueue modificationQueue = command.getModificationQueue();
			if (modificationQueue != null) {

				modificationQueue.delete(user, relationship, passive);

			} else {

				logger.error("Got empty changeSet from command!");
			}

		} else {

			RuntimeEventLog.transaction("Not in transaction");
			throw new NotInTransactionException("Not in transaction.");
		}
	}

	public static void registerTransactionListener(final StructrTransactionListener listener) {
		listeners.add(listener);
	}

	public static void removeTransactionListener(final StructrTransactionListener listener) {
		listeners.remove(listener);
	}

	public static Set<StructrTransactionListener> getTransactionListeners() {
		return listeners;
	}

	public static void simpleBroadcastWarning(final String title, final String text, final Predicate<String> sessionIdPredicate) {

		final Map<String, Object> messageData = new HashMap();

		messageData.put(MaintenanceCommand.COMMAND_TYPE_KEY,    MaintenanceCommand.COMMAND_SUBTYPE_WARNING);
		messageData.put(MaintenanceCommand.COMMAND_TITLE_KEY,   title);
		messageData.put(MaintenanceCommand.COMMAND_MESSAGE_KEY, text);

		TransactionCommand.simpleBroadcastGenericMessage(messageData, sessionIdPredicate);
	}

	public static void simpleBroadcastGenericMessage (final Map<String, Object> data) {
		simpleBroadcastGenericMessage(data, null);
	}

	public static void simpleBroadcastGenericMessage (final Map<String, Object> data, final Predicate<String> sessionIdPredicate) {
		simpleBroadcast("GENERIC_MESSAGE", data, sessionIdPredicate);
	}

	public static void simpleBroadcastDeprecationWarning (final String deprecationSubType, final String title, final String text, final String uuid) {

		final Map<String, Object> messageData = Map.of(
				MaintenanceCommand.COMMAND_TYPE_KEY,    "DEPRECATION",
				MaintenanceCommand.COMMAND_SUBTYPE_KEY, deprecationSubType,
				MaintenanceCommand.COMMAND_TITLE_KEY,   title,
				MaintenanceCommand.COMMAND_MESSAGE_KEY, text,
				"nodeId", uuid
		);

		TransactionCommand.simpleBroadcastGenericMessage(messageData);
	}

	public static void simpleBroadcastException (final Exception ex, final Map<String, Object> data, final boolean printStackTrace) {

		data.put("message", ex.getMessage());
		data.put("stringvalue", ex.toString());

		if (printStackTrace) {
			logger.warn("", ex);
		}

		simpleBroadcast("GENERIC_MESSAGE", data, null);
	}

	public static void simpleBroadcast (final String messageName, final Map<String, Object> data) {
		simpleBroadcast(messageName, data, null);
	}

	public static void simpleBroadcast (final String messageName, final Map<String, Object> data, final Predicate<String> sessionIdPredicate) {

		try (final Tx tx = StructrApp.getInstance().tx()) {

			for (final StructrTransactionListener listener : TransactionCommand.getTransactionListeners()) {
				listener.simpleBroadcast(messageName, data, sessionIdPredicate);
			}

			tx.success();

		} catch (FrameworkException fex) {
			logger.warn("Exception during simple broadcast", fex);
		}
	}

	public static boolean inTransaction() {
		return commands.get() != null;
	}

	public static long getCurrentTransactionId() {

		final TransactionCommand cmd = commands.get();
		if (cmd != null) {

			return cmd.getTransactionId();
		}

		RuntimeEventLog.transaction("Not in transaction");
		throw new NotInTransactionException("Not in transaction.");
	}

	public static Transaction getCurrentTransaction() {

		final TransactionCommand cmd = commands.get();
		if (cmd != null) {

			return cmd.transaction;
		}

		RuntimeEventLog.transaction("Not in transaction");
		throw new NotInTransactionException("Not in transaction.");
	}

	public static boolean isDeleted(final PropertyContainer propertyContainer) {

		if (propertyContainer.isNode()) {
			return isDeleted((Node)propertyContainer);
		}

		return isDeleted((Relationship)propertyContainer);
	}

	public static boolean isDeleted(final Node node) {

		TransactionCommand cmd = commands.get();
		if (cmd != null) {

			return cmd.queue.isDeleted(node);
		}

		RuntimeEventLog.transaction("Not in transaction");
		throw new NotInTransactionException("Not in transaction.");
	}

	public static boolean isDeleted(final Relationship rel) {


		TransactionCommand cmd = commands.get();
		if (cmd != null) {

			return cmd.queue.isDeleted(rel);

		} else {

			RuntimeEventLog.transaction("Not in transaction");
			throw new NotInTransactionException("Not in transaction.");
		}
	}

	public static void registerNodeCallback(final NodeInterface node, final String callbackId) {

		TransactionCommand command = commands.get();
		if (command != null) {

			ModificationQueue modificationQueue = command.getModificationQueue();
			if (modificationQueue != null) {

				modificationQueue.registerNodeCallback(node, callbackId);

			} else {

				logger.error("Got empty changeSet from command!");
			}

		} else {

			logger.error("Unable to register node callback");
		}

	}

	public static void registerRelCallback(final RelationshipInterface rel, final String callbackId) {

		TransactionCommand command = commands.get();
		if (command != null) {

			ModificationQueue modificationQueue = command.getModificationQueue();
			if (modificationQueue != null) {

				modificationQueue.registerRelCallback(rel, callbackId);

			} else {

				logger.error("Got empty changeSet from command!");
			}

		} else {

			logger.error("Unable to register relationship callback");
		}

	}

	public static void queuePostProcessProcedure(final Runnable runnable) {

		final TransactionCommand transactionCommand = commands.get();
		if (transactionCommand != null) {

			transactionCommand.postProcessQueue.queueProcess(runnable);
		}
	}

	public static void flushCaches() {
		final DatabaseService graphDb = Services.getInstance().getDatabaseService();
		graphDb.flushCaches();
	}

	private static void assertSameTransaction(final GraphObject obj, final long currentTransactionId) {

		final long nodeTransactionId = obj.getSourceTransactionId();
		if (!Services.isTesting() && currentTransactionId != nodeTransactionId) {

			logger.warn("Possible leaking {} instance detected: created in transaction {}, modified in {}", obj.getClass().getSimpleName(), nodeTransactionId, currentTransactionId);
			Thread.dumpStack();
		}
	}

	// ----- private methods -----
	private ModificationQueue getModificationQueue() {
		return queue;
	}

	private long getTransactionId() {
		return transaction.getTransactionId();
	}
}
