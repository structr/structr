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
package org.structr.core.graph;

import org.structr.api.Prefetcher;
import org.structr.api.RetryException;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.StructrTransactionListener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 *
 */
public class Tx implements AutoCloseable, Prefetcher {

	private final AtomicBoolean guard       = new AtomicBoolean(false);
	private SecurityContext securityContext = null;
	private boolean doValidation            = true;
	private boolean doCallbacks             = true;
	private boolean doNotifications         = true;
	private String prefetchHint             = null;

	public Tx(final SecurityContext securityContext) {
		this(securityContext, true, true);
	}

	public Tx(final SecurityContext securityContext, final boolean doValidation, final boolean doCallbacks) {
		this(securityContext, doValidation, doCallbacks, (securityContext != null && securityContext.doTransactionNotifications()));
	}

	public Tx(final SecurityContext securityContext, final boolean doValidation, final boolean doCallbacks, final boolean doNotifications) {

		this.securityContext = securityContext;
		this.doNotifications = doNotifications;
		this.doValidation    = doValidation;
		this.doCallbacks     = doCallbacks;
	}

	public Tx begin() throws FrameworkException {

		TransactionCommand.beginTx(securityContext);

		return this;
	}

	public void success() throws FrameworkException {
		TransactionCommand.commitTx(securityContext, doValidation);
	}

	@Override
	public void prefetchHint(final String hint) {
		TransactionCommand.getCurrentTransaction().prefetchHint(hint);
		this.prefetchHint = hint;
	}

	@Override
	public void prefetch(final String type1, final String type2, final Set<String> keys) {
		TransactionCommand.getCurrentTransaction().prefetch(type1, type2, keys);
	}

	@Override
	public void prefetch(final String query, final Set<String> keys) {
		TransactionCommand.getCurrentTransaction().prefetch(query, keys);
	}

	@Override
	public void prefetch(final String query, final Set<String> outgoingKeys, final Set<String> incomingKeys) {
		TransactionCommand.getCurrentTransaction().prefetch(query, outgoingKeys, incomingKeys);
	}

	@Override
	public void prefetch2(final String query, final Set<String> outgoingKeys, final Set<String> incomingKeys, final String id) {
		TransactionCommand.getCurrentTransaction().prefetch2(query, outgoingKeys, incomingKeys, id);
	}

	public void setIsPing(final boolean isPing) {
		TransactionCommand.getCurrentTransaction().setIsPing(isPing);
	}

	@Override
	public void close() throws FrameworkException {

		final ModificationQueue modificationQueue = TransactionCommand.finishTx();

		if (guard.compareAndSet(false, true) && (modificationQueue == null || modificationQueue.transactionWasSuccessful())) {

			final List<Long> ids = new ArrayList<>();
			boolean hasChanges   = false;
			boolean retry        = true;

			while (retry) {

				retry = false;

				// experimental
				try (final Tx tx = begin()) {

					if (doCallbacks && modificationQueue != null) {

						modificationQueue.doOuterCallbacks(securityContext);

						// notify listeners if desired, and allow this setting to be overriden locally AND remotely
						if ( (securityContext == null) ? doNotifications : doNotifications && securityContext.doTransactionNotifications() ) {

							final Collection<ModificationEvent> modificationEvents = modificationQueue.getModificationEvents();
							for (final StructrTransactionListener listener : TransactionCommand.getTransactionListeners()) {

								listener.afterCommit(securityContext, modificationEvents);
							}
						}

						hasChanges = modificationQueue.hasChanges();
						if (hasChanges) {

							ids.addAll(modificationQueue.getIds());
						}

						modificationQueue.updateChangelog();
						modificationQueue.clear();
					}

					tx.success();

				} catch (RetryException rex) {
					retry = true;
				}
			}

			guard.set(false);

			// only for clustering
			if (hasChanges) {

				// send broadcast
				Services.getInstance().broadcastDataChange(ids);
			}

			// clear function property cache to avoid caching of objects across transaction boundaries
			if (securityContext != null) {
				
				securityContext.getContextStore().clearFunctionPropertyCache();
			}
		}
	}

	/**
	 * Allow setting the securityContext if it was null.
	 * Important for Login transactions.
	 * @param sc
	 */
	public void setSecurityContext(final SecurityContext sc) {

		if (securityContext == null) {

			if (sc.isSuperUserSecurityContext() == Boolean.FALSE) {

				securityContext = sc;
			}
		}
	}

	public void disableChangelog() {
		TransactionCommand.disableChangelog();
	}
}
