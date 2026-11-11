/*
 * Copyright (C) 2010-2026 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.process.engine;

import org.structr.common.trace.ScriptFrame;

/**
 * A script of a BPMN process: an automatic task, an ioMapping, a gateway condition or a listener
 * method. The non-fatal ones do not stop the process when they fail, so they report where they fail.
 */
public class BpmnScriptFrame extends ScriptFrame {

	public BpmnScriptFrame(final Spec spec) {
		this(spec, false);
	}

	private BpmnScriptFrame(final Spec spec, final boolean boundary) {
		super(spec, boundary);
	}

	/**
	 * A script whose failure does not stop the process.
	 */
	public static BpmnScriptFrame nonFatal(final Spec spec) {
		return new BpmnScriptFrame(spec, true);
	}

	@Override
	public String kind() {
		return "BPMN";
	}

	@Override
	public boolean isLogContext() {
		return true;
	}
}
