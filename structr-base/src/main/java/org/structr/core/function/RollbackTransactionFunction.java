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
package org.structr.core.function;

import org.structr.common.error.FrameworkException;
import org.structr.core.graph.TransactionCommand;
import org.structr.schema.action.ActionContext;

public class RollbackTransactionFunction extends CoreFunction {

        @Override
        public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

                // force current transaction to fail
                TransactionCommand.getCurrentTransaction().failure();

                return null;
        }

        @Override
        public String usage(final boolean inJavaScriptContext) {

                if (inJavaScriptContext) {

                        return "Usage: ${{ $.rollbackTransaction(); }}";

                } else {

                        return "Usage: ${rollback_transaction()}";
                }
        }

        @Override
        public String shortDescription() {
                return "Marks the current transaction as failed and prevents all objects from being persisted in the database.";
        }

        @Override
        public String getSignature() {
                return "";
        }

        @Override
        public String getName() {
                return "rollback_transaction";
        }
}
