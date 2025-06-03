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
package org.structr.core.cluster;

import java.io.Serializable;
import java.util.List;

/**
 *
 * @author Christian Morgner
 */
public class StructrMessage implements Serializable {

 	private static final long serialVersionUID = 4782435385638563693L;

	private String type    = null;
	private Object payload = null;

	public StructrMessage(final String type) {
		this(type, null);
	}

	public StructrMessage(final String type, final Object payload) {

		this.type    = type;
		this.payload = payload;
	}

	public String getType() {
		return this.type;
	}

	public Object getRawPayload() {
		return this.payload;
	}

	public List getPayloadAsList() {
		return (List)this.payload;
	}
}
