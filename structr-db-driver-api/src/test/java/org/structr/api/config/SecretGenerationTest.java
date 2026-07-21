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
package org.structr.api.config;

import org.testng.annotations.Test;

import java.io.File;

import static org.testng.AssertJUnit.*;

/**
 * Tests auto-generation of the global encryption secret and the JWT secret when they are absent from the
 * configuration (see {@link Settings#getOrGenerateEncryptionSecret()} / {@link Settings#getOrGenerateJWTSecret()}).
 */
public class SecretGenerationTest {

	@Test
	public void testEncryptionSecretGeneratedWhenBlank() {

		final String original = Settings.GlobalSecret.getValue();

		try {

			Settings.GlobalSecret.setValue(null);

			final String generated = Settings.getOrGenerateEncryptionSecret();

			assertNotNull("A global encryption secret must be generated when none is configured", generated);
			assertTrue("Generated encryption secret must not be blank", generated.trim().length() > 0);
			assertTrue("Generated encryption secret should be strong (>= 32 chars)", generated.length() >= 32);
			assertTrue("Generating a secret must mark the setting modified so it gets persisted", Settings.GlobalSecret.isModified());

			// a second call must be stable (no re-generation)
			assertEquals("Repeated access must return the same generated secret", generated, Settings.getOrGenerateEncryptionSecret());

		} finally {

			Settings.GlobalSecret.setValue(original);
			cleanupGeneratedConfig();
		}
	}

	@Test
	public void testEncryptionSecretNotClobberedWhenPresent() {

		final String original = Settings.GlobalSecret.getValue();

		try {

			Settings.GlobalSecret.setValue("preconfigured-secret");

			assertEquals("An already-configured encryption secret must be returned unchanged", "preconfigured-secret", Settings.getOrGenerateEncryptionSecret());

		} finally {

			Settings.GlobalSecret.setValue(original);
			cleanupGeneratedConfig();
		}
	}

	@Test
	public void testJWTSecretGeneratedAndTypePinned() {

		final String originalSecret = Settings.JWTSecret.getValue();
		final String originalType   = Settings.JWTSecretType.getValue();

		try {

			Settings.JWTSecret.setValue("");
			Settings.JWTSecretType.setValue("keypair"); // ensure the "secret" pinning is observable

			final String generated = Settings.getOrGenerateJWTSecret();

			assertNotNull("A JWT secret must be generated when none is configured", generated);
			assertTrue("Generated JWT secret must satisfy the >= 32 character requirement", generated.length() >= 32);

			// JWTHelper rejects secrets with fewer than 10 distinct characters
			final long distinctChars = generated.chars().distinct().count();
			assertTrue("Generated JWT secret must have >= 10 distinct characters (was " + distinctChars + ")", distinctChars >= 10);

			assertTrue("Generating a JWT secret must mark it modified so it gets persisted", Settings.JWTSecret.isModified());

			// the secret type must be pinned to "secret" and forced modified so it is persisted
			assertEquals("JWTSecretType must be pinned to 'secret' when a JWT secret is generated", "secret", Settings.JWTSecretType.getValue());
			assertTrue("JWTSecretType must be marked modified so the 'secret' choice is persisted", Settings.JWTSecretType.isModified());

			assertEquals("Repeated access must return the same generated JWT secret", generated, Settings.getOrGenerateJWTSecret());

		} finally {

			Settings.JWTSecret.setValue(originalSecret);
			Settings.JWTSecretType.setValue(originalType);
			cleanupGeneratedConfig();
		}
	}

	// ----- private methods -----
	private void cleanupGeneratedConfig() {

		// the helpers persist best-effort to structr.conf in the working directory; remove any file the
		// test may have created so it does not pollute the build tree
		final File conf = new File(Settings.ConfigFileName);
		if (conf.isFile()) {
			conf.delete();
		}
	}
}
