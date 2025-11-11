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
package org.structr.docs;

import java.util.List;

public class Signature {

	private final List<Language> languages;
	private final String signature;

	public Signature(final String signature, final Language... languages) {
		this.languages = List.of(languages);
		this.signature = signature;
	}

	public List<Language> getLanguages() {
		return languages;
	}

	public String getSignature() {
		return signature;
	}

	/**
	 * Returns a Signature for the given language and signature string.
	 *
	 * @param signature the signature
	 * @param languages the languages for which this signature is valid
	 *
	 * @return a Signature for the given language and signature string
	 */
	public static Signature of(final String signature, final Language... languages) {
		return new Signature(signature, languages);
	}

	/**
	 * Returns a Signature for Javascript.
	 *
	 * @param signature
	 * @return
	 */
	public static Signature javaScript(final String signature) {
		return new Signature(signature, Language.Javascript);
	}

	/**
	 * Returns a Signature for StructrScript.
	 * @param signature
	 * @return
	 */
	public static Signature structrScript(final String signature) {
		return new Signature(signature, Language.StructrScript);
	}

	/**
	 * Returns a list of Signatures with the same signature string
	 * for all existing languages.
	 *
	 * @param signature the signature string
	 *
	 * @return a list of Signatures for all languages
	 */
	public static List<Signature> forAllLanguages(final String signature) {

		return List.of(
			Signature.of(signature, Language.values())
		);
	}
}