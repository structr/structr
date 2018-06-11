/**
 * Copyright (C) 2010-2018 Structr GmbH
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

import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;
import org.structr.api.RetryException;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.StructrTransactionListener;
import org.structr.core.TransactionSource;
import org.structr.core.app.StructrApp;

/**
 *
 *
 */
public class Tx implements AutoCloseable {

	private final AtomicBoolean guard       = new AtomicBoolean(false);
	private SecurityContext securityContext = null;
	private boolean success                 = false;
	private boolean doValidation            = true;
	private boolean doCallbacks             = true;
	private boolean doNotifications         = true;
	private TransactionCommand cmd          = null;
	private StructrApp app                  = null;

	public Tx(final SecurityContext securityContext, final StructrApp app) {
		this(securityContext, app, true, true);
	}

	public Tx(final SecurityContext securityContext, final StructrApp app, final boolean doValidation, final boolean doCallbacks) {
		this(securityContext, app, doValidation, doCallbacks, ((securityContext == null) ? false : securityContext.doTransactionNotifications()));
	}

	public Tx(final SecurityContext securityContext, final StructrApp app, final boolean doValidation, final boolean doCallbacks, final boolean doNotifications) {

		this.securityContext = securityContext;
		this.doNotifications = doNotifications;
		this.doValidation    = doValidation;
		this.doCallbacks     = doCallbacks;
		this.app             = app;
	}

	public Tx begin() throws FrameworkException {

		cmd = app.command(TransactionCommand.class).beginTx();

		return this;
	}

	public void success() throws FrameworkException {
		cmd.commitTx(doValidation);
		success = true;
	}

	@Override
	public void close() throws FrameworkException {

		final ModificationQueue modificationQueue = cmd.finishTx();

		if (success && guard.compareAndSet(false, true)) {

			boolean retry  = true;
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

								listener.afterCommit(securityContext, modificationEvents, cmd.getSource());
							}
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
		}
	}

	public void setSource(final TransactionSource source) {
		cmd.setSource(source);
	}
}
