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

import org.structr.common.error.FrameworkException;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.schema.action.ActionContext;

import java.util.List;

public class GetErrorsFunction extends CoreFunction{
    @Override
    public Object apply(ActionContext ctx, Object caller, Object[] sources) throws FrameworkException {
        return ctx.getErrorBuffer().getErrorTokens();
    }

    @Override
    public String getName() {
        return "get_errors";
    }

    @Override
    public String getShortDescription() {
        return "Returns all error tokens present in the current context.";
    }

    @Override
    public String getLongDescription() {
        return "";
    }

    @Override
    public List<Signature> getSignatures() {
        return Signature.forAllScriptingLanguages("");
    }

    @Override
    public List<Usage> getUsages() {
        return List.of(
                Usage.javaScript("Usage: ${{$.getErrors()}}. Example: ${{$.getErrors()}}"),
                Usage.structrScript("Usage: ${get_errors()}. Example: ${get_errors()}")
        );
    }
}
