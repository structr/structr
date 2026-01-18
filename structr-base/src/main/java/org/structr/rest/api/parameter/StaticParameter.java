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
package org.structr.rest.api.parameter;

/**
 *
 * @author Christian Morgner
 */
public class StaticParameter implements RESTParameter {

	private final boolean includeInSignature;
	private final String staticSignaturePart;
	private final String part;

	public StaticParameter(final String part, final boolean includeInSignature, final String staticSignaturePart) {

		this.includeInSignature  = includeInSignature;
		this.staticSignaturePart = staticSignaturePart;
		this.part                = part;
	}

	@Override
	public String key() {
		return part;
	}

	@Override
	public String urlPattern() {
		return part;
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
