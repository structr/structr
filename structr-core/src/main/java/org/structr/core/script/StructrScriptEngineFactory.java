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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;

/**
 *
 *
 */
public class StructrScriptEngineFactory implements ScriptEngineFactory {

	private static final Map<String, Object> SCRIPT_ENGINE_PARAMETERS = new HashMap<>();
	private static final String              SCRIPT_ENGINE_NAME       = "structr";
	private static final String              SCRIPT_ENGINE_VERSION    = "1.1";
	private static final List<String>        SCRIPT_ENGINE_NAMES      = new LinkedList<>(Arrays.asList( new String[] { SCRIPT_ENGINE_NAME } ));
	private static final String              SCRIPT_LANGUAGE_NAME     = "StructrScript";

	static {

		// initialize parameter map
		SCRIPT_ENGINE_PARAMETERS.put(ScriptEngine.ENGINE,           SCRIPT_ENGINE_NAME);
		SCRIPT_ENGINE_PARAMETERS.put(ScriptEngine.ENGINE_VERSION,   SCRIPT_ENGINE_VERSION);
		SCRIPT_ENGINE_PARAMETERS.put(ScriptEngine.FILENAME,         Collections.EMPTY_LIST);
		SCRIPT_ENGINE_PARAMETERS.put(ScriptEngine.LANGUAGE,         SCRIPT_LANGUAGE_NAME);
		SCRIPT_ENGINE_PARAMETERS.put(ScriptEngine.LANGUAGE_VERSION, SCRIPT_ENGINE_VERSION);
		SCRIPT_ENGINE_PARAMETERS.put(ScriptEngine.NAME,             SCRIPT_ENGINE_NAME);
	}

	@Override
	public String getEngineName() {
		return SCRIPT_ENGINE_NAME;
	}

	@Override
	public String getEngineVersion() {
		return SCRIPT_ENGINE_VERSION;
	}

	@Override
	public List<String> getExtensions() {
		return Collections.EMPTY_LIST;
	}

	@Override
	public List<String> getMimeTypes() {
		return Collections.EMPTY_LIST;
	}

	@Override
	public List<String> getNames() {
		return SCRIPT_ENGINE_NAMES;
	}

	@Override
	public String getLanguageName() {
		return SCRIPT_LANGUAGE_NAME;
	}

	@Override
	public String getLanguageVersion() {
		return SCRIPT_ENGINE_VERSION;
	}

	@Override
	public Object getParameter(String key) {
		return SCRIPT_ENGINE_PARAMETERS.get(key);
	}

	@Override
	public String getMethodCallSyntax(String obj, String m, String... args) {

		final StringBuilder buf = new StringBuilder();

		buf.append(m);
		buf.append("(");
		buf.append(obj);

		for (final String arg : args) {
			buf.append(", ");
			buf.append(arg);
		}

		buf.append(")");

		return buf.toString();
	}

	@Override
	public String getOutputStatement(String toDisplay) {

		final StringBuilder buf = new StringBuilder("('");

		buf.append(toString());

		buf.append("')");

		return buf.toString();
	}

	@Override
	public String getProgram(String... statements) {

		final StringBuilder buf = new StringBuilder("${(");
		final int length        = statements.length;

		for (int i=0; i<length; i++) {

			buf.append("(");
			buf.append(statements[i]);
			buf.append(")");

			if (i < length) {
				buf.append(",");
			}
		}

		buf.append(")}");

		return buf.toString();
	}

	@Override
	public ScriptEngine getScriptEngine() {
		return new StructrScriptEngine(this);
	}
}
