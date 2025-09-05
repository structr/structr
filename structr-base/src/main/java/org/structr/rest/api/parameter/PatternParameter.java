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
package org.structr.rest.api.parameter;

/**
 *
 */
public class PatternParameter implements RESTParameter {

	private final boolean includeInSignature;
	private String staticSignaturePart;
	private final String key;
	private final String pattern;

	public PatternParameter(final String key, final String pattern, final boolean includeInSignature, final String staticSignaturePart) {

		this.key                 = key;
		this.pattern             = pattern;
		this.includeInSignature  = includeInSignature;
		this.staticSignaturePart = staticSignaturePart;
	}

	@Override
	public String key() {
		return key;
	}

	@Override
	public String urlPattern() {
		return pattern;
	}

	@Override
	public boolean includeInSignature() {
		return includeInSignature;
	}

	@Override
	public String staticResourceSignaturePart() {
		return staticSignaturePart;
	}
}
