/*
 * Copyright (C) 2010-2022 Structr GmbH
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
package org.structr.common.event;

import java.util.LinkedHashMap;
import java.util.Map;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;

public class Usage {

	private String uuid = null;
	private String type = null;
	private String ctx  = null;

	public Usage(final String uuid, final String type, final String ctx) {

		this.uuid = uuid;
		this.type = type;
		this.ctx  = ctx;
	}

	@Override
	public String toString() {
		return uuid + " (" + ctx + ")";
	}

	@Override
	public int hashCode() {
		return toString().hashCode();
	}

	@Override
	public boolean equals(final Object other) {
		return other.hashCode() == this.hashCode();
	}

	public String getUuid() {
		return uuid;
	}

	public Map<String, Object> convert(final SecurityContext securityContext) throws FrameworkException {

		final Map<String, Object> result = new LinkedHashMap<>();

		result.put("key",  ctx);
		result.put("type", type);

		resolve(securityContext, result);

		return result;
	}

	public void resolve(final SecurityContext securityContext, final Map<String, Object> data) throws FrameworkException {

		if (uuid != null) {

			final NodeInterface node = StructrApp.getInstance(securityContext).getNodeById(uuid);
			if (node != null) {

				node.visitForUsage(data);
			}
		}
	}
}
