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
package org.structr.core.script.polyglot.config;

import org.structr.core.api.AbstractMethod;

public class ScriptConfigBuilder {
    private final ScriptConfig config = new ScriptConfig();

    public ScriptConfigBuilder() {
    }

    public ScriptConfigBuilder wrapJsInMain(final boolean wrapJsInMain) {
        config.setWrapJsInMain(wrapJsInMain);
        return this;
    }

    public ScriptConfigBuilder keepContextOpen(final boolean keepContextOpen) {
        config.setKeepContextOpen(keepContextOpen);
        return this;
    }

    public ScriptConfigBuilder currentMethod(final AbstractMethod currentMethod) {
        config.setCurrentMethod(currentMethod);
        return this;
    }

    public ScriptConfig build() {
        return config;
    }


}
