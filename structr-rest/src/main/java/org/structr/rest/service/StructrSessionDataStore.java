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
package org.structr.rest.service;

import java.util.Set;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.xerces.impl.dv.util.Base64;
import org.eclipse.jetty.server.session.AbstractSessionDataStore;
import org.eclipse.jetty.server.session.SessionData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Principal;
import org.structr.core.graph.Tx;
import org.structr.rest.auth.AuthHelper;
import org.structr.rest.auth.SessionHelper;

/**
 *
 *
 */
public class StructrSessionDataStore extends AbstractSessionDataStore {

	private static final Logger logger = LoggerFactory.getLogger(StructrSessionDataStore.class.getName());

	public StructrSessionDataStore() {
	}

	@Override
	public void doStore(final String id, final SessionData data, final long lastSaveTime) throws Exception {

		assertInitialized();

		try (final Tx tx = StructrApp.getInstance().tx(false, false, false)) {

			final Principal user = AuthHelper.getPrincipalForSessionId(id);

			// store sessions only for authenticated users
			if (user != null) {

				user.setSessionData(Base64.encode(SerializationUtils.serialize(data)));
			}

			tx.success();

		} catch (FrameworkException ex) {

			logger.info("Unable to store session data for session id " + id + ".", ex);
		}
	}

	@Override
	public Set<String> doGetExpired(final Set<String> candidates) {
		return candidates;
	}

	@Override
	public boolean isPassivating() {
		return true;
	}

	@Override
	public boolean exists(final String id) throws Exception {

		assertInitialized();

		try (final Tx tx = StructrApp.getInstance().tx(false, false, false)) {

			final boolean exists = AuthHelper.getPrincipalForSessionId(id) != null;

			tx.success();

			return exists;

		} catch (FrameworkException ex) {

			logger.info("Unable to determine if session " + id + " exists.", ex);
		}

		return false;
	}

	@Override
	public SessionData load(final String id) throws Exception {

		assertInitialized();

		SessionData sessionData = null;

		try (final Tx tx = StructrApp.getInstance().tx(false, false, false)) {

			final Principal user = AuthHelper.getPrincipalForSessionId(id);


			// store sessions only for authenticated users
			if (user != null) {

				final String sessionDataString = user.getSessionData();
				if (sessionDataString != null) {

					sessionData = SerializationUtils.deserialize(Base64.decode(sessionDataString));
				}
			}

			tx.success();

		} catch (FrameworkException ex) {

			logger.info("Unable to load session data for session id " + id + ".", ex);
		}

		return sessionData;
	}

	@Override
	public boolean delete(final String id) throws Exception {

		assertInitialized();

		try (final Tx tx = StructrApp.getInstance().tx(false, false, false)) {

			SessionHelper.clearSession(id);

			tx.success();

			return true;

		} catch (FrameworkException ex) {

			logger.info("Unable to load session data for session id " + id + ".", ex);
		}

		return false;
	}


	// ----- private methods -----
	private void assertInitialized() {

		while (!Services.getInstance().isInitialized()) {
			try { Thread.sleep(1000); } catch (Throwable t) {}
		}
	}
}
