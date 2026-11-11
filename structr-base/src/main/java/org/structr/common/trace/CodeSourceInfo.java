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
package org.structr.common.trace;

/**
 * The node holding failing code, as the editor and the scripting error popup need it.
 *
 * @param id         the uuid of the node
 * @param type       its type, such as {@code SchemaMethod} or {@code Content}
 * @param staticType the type a static method belongs to, or null if the code is no static method
 */
public record CodeSourceInfo(String id, String type, String staticType) {

	public boolean isStaticMethod() {
		return staticType != null;
	}
}
