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
package org.structr.test.core.function;

import org.structr.common.error.FrameworkException;
import org.structr.core.function.CryptFunction;
import org.structr.core.graph.CryptFunctionMigrationHandler;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

/**
 * Ticket 1601: encrypt() and decrypt() turned a passphrase into a key with an unsalted MD5 digest, which
 * is what makes a stolen ciphertext worth guessing at offline - millions of candidate passphrases a
 * second, and two values encrypted with the same passphrase sharing a key.
 *
 * <p>The scheme is now the first argument and has to be named. {@code aes-gcm-pbkdf2} derives the key
 * with PBKDF2 and a salt of its own per value; {@code legacy} is what the function did before and is
 * there so that values written that way stay readable.
 */
public class CryptFunctionSchemeTest {

	private static final String PASSPHRASE = "correct-horse-battery-staple";
	private static final String CLEARTEXT  = "the-value-that-must-not-leak";

	@Test
	public void testARoundTripThroughTheDerivedSchemeReturnsTheValue() {

		try {

			final String ciphertext = CryptFunction.encrypt(CryptFunction.SCHEME_PBKDF2, CLEARTEXT, PASSPHRASE);

			assertNotNull("encrypting with the derived scheme produced nothing", ciphertext);
			assertFalse("the ciphertext contains the cleartext", ciphertext.contains(CLEARTEXT));

			assertEquals("the value did not survive the round trip", CLEARTEXT, CryptFunction.decrypt(CryptFunction.SCHEME_PBKDF2, ciphertext, PASSPHRASE));

		} catch (final FrameworkException fex) {

			fail("Unexpected exception: " + fex.getMessage());
		}
	}

	/**
	 * What the salt is for, and the difference to the scheme it replaces: the same value encrypted twice
	 * with the same passphrase must not produce the same ciphertext, or a dump tells an attacker which
	 * records share a value before anything is decrypted.
	 */
	@Test
	public void testEveryValueGetsASaltOfItsOwn() {

		try {

			final String first  = CryptFunction.encrypt(CryptFunction.SCHEME_PBKDF2, CLEARTEXT, PASSPHRASE);
			final String second = CryptFunction.encrypt(CryptFunction.SCHEME_PBKDF2, CLEARTEXT, PASSPHRASE);

			/* Comparing the two ciphertexts would prove nothing: both schemes use a random IV, so they
			   differ either way and the assertion would hold with no salt at all. The salt is the first
			   sixteen bytes, and that is what has to differ. */
			final byte[] firstSalt  = Arrays.copyOf(Base64.getDecoder().decode(first), 16);
			final byte[] secondSalt = Arrays.copyOf(Base64.getDecoder().decode(second), 16);

			assertFalse("two values encrypted with the same passphrase share a salt, so they share a key", Arrays.equals(firstSalt, secondSalt));

			assertFalse("the salt is all zeroes, so it is not being generated at all", Arrays.equals(firstSalt, new byte[16]));

			assertEquals("the first value no longer decrypts", CLEARTEXT, CryptFunction.decrypt(CryptFunction.SCHEME_PBKDF2, first, PASSPHRASE));
			assertEquals("the second value no longer decrypts", CLEARTEXT, CryptFunction.decrypt(CryptFunction.SCHEME_PBKDF2, second, PASSPHRASE));

		} catch (final FrameworkException fex) {

			fail("Unexpected exception: " + fex.getMessage());
		}
	}

	/**
	 * The reason the old scheme is still selectable at all: a value encrypted by an earlier version has
	 * to keep opening, and it does so through the same path that wrote it.
	 */
	@Test
	public void testTheLegacySchemeStillReadsWhatTheOldImplementationWrote() {

		try {

			final String writtenByTheOldImplementation = CryptFunction.encrypt(CLEARTEXT, PASSPHRASE);

			assertNotNull("the previous implementation produced nothing", writtenByTheOldImplementation);

			assertEquals("a value from the previous implementation cannot be read through the legacy scheme",
				CLEARTEXT, CryptFunction.decrypt(CryptFunction.SCHEME_LEGACY, writtenByTheOldImplementation, PASSPHRASE));

		} catch (final FrameworkException fex) {

			fail("Unexpected exception: " + fex.getMessage());
		}
	}

	/**
	 * The two schemes produce different ciphertexts from the same passphrase, so neither can read the
	 * other's. Worth pinning: it is the reason the migration step reports rather than rewrites.
	 */
	@Test
	public void testTheSchemesCannotReadEachOther() {

		try {

			final String derived = CryptFunction.encrypt(CryptFunction.SCHEME_PBKDF2, CLEARTEXT, PASSPHRASE);

			assertFalse("a value encrypted with the derived scheme was readable as a legacy value",
				CLEARTEXT.equals(CryptFunction.decrypt(CryptFunction.SCHEME_LEGACY, derived, PASSPHRASE)));

		} catch (final FrameworkException fex) {

			fail("Unexpected exception: " + fex.getMessage());
		}
	}

	@Test
	public void testAnUnknownSchemeIsRefused() {

		try {

			CryptFunction.encrypt("aes-256-whatever", CLEARTEXT, PASSPHRASE);

			fail("an unknown scheme name was accepted");

		} catch (final FrameworkException expected) {

			assertTrue("the error does not say which names exist: " + expected.getMessage(), String.valueOf(expected.getMessage()).contains(CryptFunction.SCHEME_PBKDF2));
		}
	}

	// ----- the migration report -----

	/**
	 * The step that finds the calls written before the scheme existed. It has to recognise both the
	 * updated and the not-updated shape, because reporting everything would be as useless as reporting
	 * nothing.
	 */
	@Test
	public void testTheMigrationStepRecognisesWhichCallsNeedAttention() {

		assertTrue("a call in the old shape was not reported", CryptFunctionMigrationHandler.usesOldSignature("encrypt(this.value, 'secret key')"));
		assertTrue("a call with only a value was not reported", CryptFunctionMigrationHandler.usesOldSignature("encrypt(this.value)"));
		assertTrue("a call whose first argument is not a known scheme was not reported", CryptFunctionMigrationHandler.usesOldSignature("decrypt('aes-256-whatever', this.value)"));

		assertFalse("an updated call was reported anyway", CryptFunctionMigrationHandler.usesOldSignature("encrypt('aes-gcm-pbkdf2', this.value, 'secret key')"));
		assertFalse("an updated call naming the legacy scheme was reported anyway", CryptFunctionMigrationHandler.usesOldSignature("decrypt('legacy', this.value, 'secret key')"));
		assertFalse("double quotes are not recognised", CryptFunctionMigrationHandler.usesOldSignature("encrypt(\"aes-gcm-pbkdf2\", this.value)"));
	}

	@Test
	public void testTheMigrationStepFindsCallsInBothScriptingLanguages() {

		final List<String> calls = CryptFunctionMigrationHandler.findCalls("${encrypt(this.a, 'k')} and ${{ $.decrypt($.this.b, 'k') }}");

		assertEquals("not both calls were found: " + calls, 2, calls.size());
		assertTrue("the StructrScript call was not found: " + calls, calls.get(0).startsWith("encrypt("));
		assertTrue("the JavaScript call was not found: " + calls, calls.get(1).startsWith("decrypt("));
	}
}
