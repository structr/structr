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
import org.slf4j.event.Level;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

public class PolyglotOutputStream extends OutputStream {
    private final Logger logger;
    private final StringBuilder buffer;
    private final Level level;

    public PolyglotOutputStream(Logger logger) {
        this.logger = logger;
        this.level = Level.INFO;
        this.buffer = new StringBuilder();
    }

    @Override
    public void write(int b) throws IOException {
        if (b == '\n') {
            flush();
        } else {
            buffer.append((char) b);
        }
    }

    @Override
    public void flush () {
        if (!buffer.isEmpty()) {
            logger.atLevel(level).log(buffer.toString());
            buffer.setLength(0);
        }
    }
}
