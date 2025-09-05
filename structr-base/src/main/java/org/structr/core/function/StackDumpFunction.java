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
package org.structr.core.function;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.schema.action.ActionContext;

import java.util.LinkedList;
import java.util.List;

public class StackDumpFunction extends CoreFunction {

	private static final Logger logger = LoggerFactory.getLogger(StackDumpFunction.class.getName());

	public static final String ERROR_MESSAGE_STACK_DUMP    = "Usage: ${stack_dump()}. Example ${stack_dump()}";
	public static final String ERROR_MESSAGE_LOG_JS = "Usage: ${{ $.stackDump(); }}. Example ${{ $.stackDump(); }}";

	@Override
	public String getName() {
		return "stack_dump";
	}

	@Override
	public String getSignature() {
		return "";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		final List<String> frames = new LinkedList<>();

		StackWalker.getInstance().forEach(f -> frames.add("        " + f.toString()));

		logger.info("Stack dump requested:\n{}", StringUtils.join(frames, "\n"));

		return null;
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_LOG_JS : ERROR_MESSAGE_STACK_DUMP);
	}

	@Override
	public String shortDescription() {
		return "Logs the current execution stack.";
	}
}
