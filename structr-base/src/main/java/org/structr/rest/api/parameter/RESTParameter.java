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
package org.structr.rest.api.parameter;

/**
 */
public interface RESTParameter {

	String key();
	String urlPattern();
	boolean includeInSignature();
	String staticResourceSignaturePart();

	static RESTParameter forPattern(final String key, final String pattern, final boolean includeInSignature) {
		return new PatternParameter(key, pattern, includeInSignature, null);
	}

	static RESTParameter forPattern(final String key, final String pattern, final boolean includeInSignature, final String staticResourceSignaturePart) {
		return new PatternParameter(key, pattern, includeInSignature, staticResourceSignaturePart);
	}

	static RESTParameter forStaticString(final String part, final boolean includeInSignature) {
		return new StaticParameter(part, includeInSignature, part);
	}

	static RESTParameter forStaticString(final String part, final boolean includeInSignature, final String staticSignaturePart) {
		return new StaticParameter(part, includeInSignature, staticSignaturePart);
	}
}
