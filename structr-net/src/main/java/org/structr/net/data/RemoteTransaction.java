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
package org.structr.net.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.net.data.time.PseudoTemporalEnvironment;
import org.structr.net.peer.Peer;
import org.structr.net.protocol.*;
import org.structr.net.repository.RepositoryObject;


public class RemoteTransaction implements Callback, AutoCloseable {

	private static final Logger logger = LoggerFactory.getLogger(RemoteTransaction.class.getName());

	private PseudoTemporalEnvironment pte = null;
	private AbstractMessage message       = null;
	private String transactionOwner       = null;
	private final Object lock             = new Object();
	private String transactionId          = null;
	private boolean hasTimeout            = false;
	private long timeoutValue             = 5000L;
	private Peer peer                     = null;

	public RemoteTransaction(final Peer peer) {

		this.pte  = peer.getPseudoTemporalEnvironment().transaction();
		this.peer = peer;
	}

	public Object getProperty(final RepositoryObject sharedObject, final String key) throws TimeoutException {

		final Get get = new Get(peer.getUuid(), sharedObject, pte.current(), transactionId, key);

		peer.registerCallback(get.getId(), this);
		peer.broadcast(get);

		waitForCallback(get.getId(), timeoutValue);

		if (message != null && message instanceof Value) {

			final Value value = (Value)message;
			return value.getValue();
		}

		return null;
	}


	public void setProperty(final RepositoryObject sharedObject, final String key, final Object value) throws TimeoutException {

		peer.log("Set(", sharedObject.getUuid(), ", ", key, ", ", value, ")");

		final Set set = new Set(peer.getUuid(), sharedObject, pte.next(), transactionId, key, value);

		peer.registerCallback(set.getId(), this);
		peer.broadcast(set);

		waitForCallback(set.getId(), timeoutValue);
	}

	public void begin(final RepositoryObject sharedObject) throws TimeoutException {

		this.transactionOwner = sharedObject.getDeviceId();

		peer.log("BeginTx()");

		final BeginTx beginTx = new BeginTx(peer.getUuid(), transactionOwner, timeoutValue);

		peer.registerCallback(beginTx.getId(), this);
		peer.broadcast(beginTx);

		waitForCallback(beginTx.getId(), timeoutValue);

		if (message != null && message instanceof Ack) {

			final Ack ack = (Ack)message;
			this.transactionId = ack.getData();

		}
	}

	public void commit() throws TimeoutException {

		peer.log("Commit(", transactionId, ")");

		final Commit commit = new Commit(peer.getUuid(), transactionOwner, transactionId);

		peer.registerCallback(commit.getId(), this);
		peer.broadcast(commit);

		waitForCallback(commit.getId(), timeoutValue);
	}

	@Override
	public void close() throws Exception {
	}

	public String getTransactionId() {
		return transactionId;
	}

	// ----- interface Callback -----
	@Override
	public void callback(final AbstractMessage msg) {

		synchronized (lock) {

			hasTimeout = false;
			message = msg;

			lock.notify();
		}
	}

	// ----- private methods -----
	private void waitForCallback(final String uuid, final long timeout) throws TimeoutException {

		synchronized (lock) {

			this.message = null;
			this.hasTimeout = true;

			try {
				lock.wait(timeout);

			} catch (InterruptedException iex) {
				logger.warn("", iex);
			}

			// remove timed out callback
			if (this.hasTimeout) {

				peer.unregisterCallback(uuid);
				throw new TimeoutException();
			}
		}
	}
}
