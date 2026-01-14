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
package org.structr.core.script.polyglot.context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.logging.Handler;
import java.util.logging.LogRecord;

public class PolyglotLogHandler extends Handler {
    private static final Logger logger = LoggerFactory.getLogger(PolyglotLogHandler.class);

    @Override
    public void publish(LogRecord record) {
        logger.info(record.getMessage());
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() {
    }
}