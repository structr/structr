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
 * Work of a flow that moved to another thread without changing what it does, such as the rendering
 * of a page answered asynchronously.
 */
public class HandOffFrame extends ContinuationFrame {

	public HandOffFrame(final CallFrame continued, final String name) {
		super(continued, name);
	}

	@Override
	public String kind() {
		return "HAND_OFF";
	}
}
