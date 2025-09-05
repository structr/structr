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
package org.structr.common.error;

import java.util.Iterator;

public class PasswordPolicyViolationException extends FrameworkException {

    public PasswordPolicyViolationException(final int status, final String message, final ErrorBuffer errorBuffer) {
        super(status, message, errorBuffer);
    }

    @Override
    public String toString() {

        final ErrorBuffer errorBuffer = getErrorBuffer();
        final int status              = getStatus();
        final String message          = getMessage();

        StringBuilder buf = new StringBuilder();
        buf.append("PasswordPolicyViolationException(").append(status).append("): ").append(message);

        if (errorBuffer != null && !errorBuffer.getErrorTokens().isEmpty()) {

            buf.append(" ErrorTokens: ");
            for (final Iterator<ErrorToken> it = errorBuffer.getErrorTokens().iterator(); it.hasNext();) {

                final ErrorToken token = it.next();

                buf.append(token);

                if (it.hasNext()) {
                    buf.append(", ");
                }
            }

        } else {

            if (this.getCause() != null) {

                buf.append(" (").append(this.getCause().getMessage()).append(")");
            }
        }

        return buf.toString();
    }
}
