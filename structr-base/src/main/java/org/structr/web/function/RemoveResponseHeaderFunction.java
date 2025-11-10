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


import jakarta.servlet.http.HttpServletResponse;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.docs.Signature;
import org.structr.schema.action.ActionContext;

import java.util.List;


public class RemoveResponseHeaderFunction extends UiAdvancedFunction {

    public static final String ERROR_MESSAGE_REMOVE_RESPONSE_HEADER    = "Usage: ${remove_response_header(field)}. Example: ${remove_response_header('X-Frame-Options'}";
    public static final String ERROR_MESSAGE_REMOVE_RESPONSE_HEADER_JS = "Usage: ${{Structr.removeResponseHeader(field)}}. Example: ${{Structr.removeResponseHeader('X-Frame-Options')}}";

    @Override
    public String getName() {
        return "remove_response_header";
    }

    @Override
    public List<Signature> getSignatures() {
        return Signature.forAllLanguages("field");
    }

    @Override
    public Object apply(ActionContext ctx, Object caller, Object[] sources) throws FrameworkException {

        try {

            assertArrayHasLengthAndAllElementsNotNull(sources, 1);

            final String name = sources[0].toString();

            final SecurityContext securityContext = ctx.getSecurityContext();
            if (securityContext != null) {

                final HttpServletResponse response = securityContext.getResponse();
                if (response != null) {

                    response.setHeader(name, null);
                }
            }

        } catch (IllegalArgumentException e) {

            logParameterError(caller, sources, e.getMessage(), ctx.isJavaScriptContext());
            return usage(ctx.isJavaScriptContext());
        }

        return "";
    }

    @Override
    public String usage(boolean inJavaScriptContext) {
        return (inJavaScriptContext ? ERROR_MESSAGE_REMOVE_RESPONSE_HEADER_JS : ERROR_MESSAGE_REMOVE_RESPONSE_HEADER);
    }

    @Override
    public String getShortDescription() {
        return "Removes the given header field from the server response";
    }
}
