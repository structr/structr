/**
 * Copyright (C) 2010-2020 Structr GmbH
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
package org.structr.core.script;

import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.PolyglotAccess;
import org.graalvm.polyglot.Value;
import org.structr.core.GraphObject;

public abstract class StructrPolyglotAccessProvider {

	public static HostAccess getHostAccessConfig() {

		return HostAccess.newBuilder()
				.allowPublicAccess(true)
				.allowArrayAccess(true)
				.allowListAccess(true)
				.targetTypeMapping(GraphObject.class, Value.class, null,
						v -> Value.asValue(new StructrPolyglotGraphObjectWrapper(v))
				)
				.build();
	}

	public static PolyglotAccess getPolyglotAccessConfig() {

		return PolyglotAccess.ALL;
	}
}
