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
package org.structr.logging;

import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import org.slf4j.MDC;
import org.structr.websocket.command.ScratchpadCommand;

public class PrefixingThrowableConverter extends ThrowableProxyConverter {

	@Override
	public String convert(ILoggingEvent event) {

		final String stack = super.convert(event);

		if (stack == null || stack.isEmpty()) {

			return "";
		}

		final String scratchPadLogString = MDC.get(ScratchpadCommand.MDC_SCRATCHPAD_TAG);

		if (scratchPadLogString == null) {

			return stack;

		} else {

			final String prefix = scratchPadLogString + " ";

			return stack.replaceAll("(?m)^", prefix);
		}
	}
}