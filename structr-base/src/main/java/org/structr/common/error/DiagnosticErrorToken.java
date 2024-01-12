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
package org.structr.common.error;

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.util.Locale;

/**
 *
 *
 */
public class DiagnosticErrorToken extends SemanticErrorToken {

	final String nodeType;
	final String nodeUuid;
	final String nodeName;

	public DiagnosticErrorToken(final String type, final Diagnostic<? extends JavaFileObject> diagnostic) {

		super(type, null, "compiler_error", diagnostic.getMessage(Locale.ENGLISH));

		this.nodeType = null;
		this.nodeUuid = null;
		this.nodeName = null;
	}

	public DiagnosticErrorToken(final String type, final Diagnostic<? extends JavaFileObject> diagnostic, final String nodeType, final String nodeUuid, final String nodeName) {

		super(type, null, "compiler_error", diagnostic.getMessage(Locale.ENGLISH));

		this.nodeType = nodeType;
		this.nodeUuid = nodeUuid;
		this.nodeName = nodeName;
	}

	public String getNodeType() {
		return this.nodeType;
	}

	public String getNodeUuid() {
		return this.nodeUuid;
	}

	public String getNodeName() {
		return this.nodeName;
	}
}
