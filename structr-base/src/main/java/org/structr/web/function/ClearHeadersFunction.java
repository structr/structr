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
package org.structr.web.function;

import org.structr.schema.action.ActionContext;

public class ClearHeadersFunction extends UiAdvancedFunction {

    public static final String ERROR_MESSAGE_CLEAR_HEADERS = "Usage: ${clear_headers()}. Example: ${clear_headers()}";
    public static final String ERROR_MESSAGE_CLEAR_HEADERS_JS = "Usage: ${{Structr.clear_headers()}}. Example: ${{Structr.clear_headers()}}";

    @Override
    public String getName() {
        return "clear_headers";
    }

    @Override
    public String getSignature() {
        return null;
    }

    @Override
    public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) {

        if (sources == null || sources.length == 0) {

            ctx.clearHeaders();
            return "";

        } else {

            logParameterError(caller, sources, ctx.isJavaScriptContext());
        }

        return usage(ctx.isJavaScriptContext());
    }

    @Override
    public String usage(boolean inJavaScriptContext) {
        return (inJavaScriptContext ? ERROR_MESSAGE_CLEAR_HEADERS_JS : ERROR_MESSAGE_CLEAR_HEADERS);
    }

    @Override
    public String shortDescription() {
        return "Clears headers for the next request";
    }
}
