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

public interface Signature {

	List<Language> getLanguages();
	String getSignature();

	/**
	 * Returns a Signature for the given language and signature string.
	 *
	 * @param signature the signature
	 * @param languages the languages for which this signature is valid
	 *
	 * @return a Signature for the given language and signature string
	 */
	static Signature of(final String signature, final Language... languages) {

		return new Signature() {

			@Override
			public List<Language> getLanguages() {
				return List.of(languages);
			}

			@Override
			public String getSignature() {
				return signature;
			}
		};
	}

	static Signature js(final String signature) {

		return new Signature() {

			@Override
			public List<Language> getLanguages() {
				return List.of(Language.Javascript);
			}

			@Override
			public String getSignature() {
				return signature;
			}
		};
	}

	static Signature ss(final String signature) {

		return new Signature() {

			@Override
			public List<Language> getLanguages() {
				return List.of(Language.StructrScript);
			}

			@Override
			public String getSignature() {
				return signature;
			}
		};
	}

	/**
	 * Returns a list of Signatures with the same signature string
	 * for all existing languages.
	 *
	 * @param signature the signature string
	 *
	 * @return a list of Signatures for all languages
	 */
	static List<Signature> forAllLanguages(final String signature) {

		return List.of(
			Signature.of(signature, Language.values())
		);
	}
}