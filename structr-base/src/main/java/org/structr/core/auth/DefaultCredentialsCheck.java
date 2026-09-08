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
package org.structr.core.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.api.service.InitializationCallback;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Principal;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.PrincipalTraitDefinition;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Reports admin accounts that still have the built-in default password.
 *
 * An instance provisioned without setting initialuser.password comes up on admin/admin and says nothing
 * about it. That is the normal, harmless case on a developer's machine and a live risk anywhere else, and
 * the difference is not something the instance can know - so it warns rather than refuses to start.
 *
 * On every startup, not once at creation: a warning printed while the user is created scrolls past in a
 * provisioning log, while the exposure lasts for as long as the password does.
 */
public class DefaultCredentialsCheck implements InitializationCallback {

	private static final Logger logger = LoggerFactory.getLogger(DefaultCredentialsCheck.class);

	private static volatile boolean defaultCredentialsInUse = false;
	private static final AtomicBoolean recheckQueued        = new AtomicBoolean(false);

	/** Whether any admin account still has the default password, as of the last check. */
	public static boolean isDefaultCredentialsInUse() {

		return defaultCredentialsInUse;
	}

	@Override
	public void initializationDone() {

		defaultCredentialsInUse = findAdminWithDefaultPassword();
	}

	/**
	 * Recomputes the flag once the password change that triggered this has committed.
	 *
	 * After the commit, not during it: the check reads every admin account, and a change that is rolled
	 * back would otherwise leave the warning describing a password nobody has. It also cannot be answered
	 * from the changed account alone - the flag is about the instance, so another admin may still be on
	 * the default - which is why this recomputes rather than simply clearing.
	 */
	public static void scheduleRecheck() {

		if (!TransactionCommand.inTransaction()) {

			// no transaction to wait for, so there is nothing to defer past
			defaultCredentialsInUse = new DefaultCredentialsCheck().findAdminWithDefaultPassword();

			return;
		}

		// one recheck per transaction however many passwords it changes: each one costs a password
		// verification per admin account, and a bulk import would otherwise pay that for every row
		if (recheckQueued.compareAndSet(false, true)) {

			TransactionCommand.queuePostCommitProcedure(() -> {

				recheckQueued.set(false);

				defaultCredentialsInUse = new DefaultCredentialsCheck().findAdminWithDefaultPassword();
			});
		}
	}

	/**
	 * The database this reports on has to be the one the instance will actually serve.
	 *
	 * On a first boot the callbacks run against the in-memory graph that is thrown away as soon as the
	 * real database is configured, so without this the check would report on a database that no longer
	 * exists, and stay silent about the one that does.
	 */
	@Override
	public boolean rerunAfterDatabaseChange() {

		return true;
	}

	// ----- private methods -----
	private boolean findAdminWithDefaultPassword() {

		// The DEFAULT of the setting, not its configured value: an operator who set initialuser.password
		// to something of their own has chosen that password, and warning about it would be noise. What
		// is worth reporting is the password anyone can look up in the documentation.
		final String defaultPassword = Settings.InitialAdminUserPassword.getDefaultValue();

		if (defaultPassword == null) {

			return false;
		}

		try (final Tx tx = StructrApp.getInstance().tx()) {

			final Traits traits                  = Traits.of(StructrTraits.PRINCIPAL);
			final PropertyKey<Boolean> isAdminKey = traits.key(PrincipalTraitDefinition.IS_ADMIN_PROPERTY);
			boolean found                         = false;

			for (final NodeInterface node : StructrApp.getInstance().nodeQuery(StructrTraits.PRINCIPAL).key(isAdminKey, true).getAsList()) {

				final Principal principal = node.as(Principal.class);
				final String storedHash   = principal.getEncryptedPassword();

				if (storedHash == null) {

					// an external account, nothing of ours to be weak
					continue;
				}

				// HashHelper directly rather than principal.isValidPassword(): that one re-hashes a legacy
				// hash on a match, and deliberately burns Argon2-equivalent time on a miss to keep login
				// timing uniform. Neither belongs in a startup check, and the miss is the common case.
				if (HashHelper.verifyPassword(defaultPassword, storedHash, principal.getSalt())) {

					logger.warn("SECURITY: admin user '{}' still has the default password. Anyone who can reach this "
						+ "instance can log in as an administrator. Change it, or set '{}' before provisioning.",
						principal.getName(), Settings.InitialAdminUserPassword.getKey());

					found = true;
				}
			}

			tx.success();

			return found;

		} catch (FrameworkException fex) {

			// a check that cannot run must not stop the instance, but silence here would be indistinguishable
			// from "no default credentials found", which is the answer that lets the risk pass unnoticed
			logger.warn("Unable to check for default admin credentials: {}", fex.getMessage());

			return false;
		}
	}
}
