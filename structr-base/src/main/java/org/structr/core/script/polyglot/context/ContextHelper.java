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
package org.structr.core.script.polyglot.context;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

public abstract class ContextHelper {
    public static final String BINDING_REFERENCE_COUNT_KEY = "__structr.context.referenceCount";

    public static int getReferenceCount(final Context context) {

        if (context == null) {
            throw new IllegalArgumentException("Given context is null. Cannot retrieve reference count.");
        }

        synchronized (context) {
            final Value countValue = context.getPolyglotBindings().getMember(BINDING_REFERENCE_COUNT_KEY);

            if (countValue != null && countValue.fitsInInt()) {

                return countValue.asInt();
            } else if (countValue == null) {

                return 0;
            }

            throw new IllegalStateException("Reference count not found in context bindings. Was internal key \"%s\" overwritten in the scripting bindings?".formatted(BINDING_REFERENCE_COUNT_KEY));
        }
    }

    public static void setReferenceCount(final Context context, final int referenceCount) {

        if (context == null) {
            throw new IllegalArgumentException("Given context is null. Cannot set reference count.");
        }

        if (referenceCount < 0) {
            throw new IllegalArgumentException("Reference count must be greater or equal to zero.");
        }

        synchronized (context) {
            context.getPolyglotBindings().putMember(BINDING_REFERENCE_COUNT_KEY, referenceCount);
        }
    }

    public static void incrementReferenceCount(final Context context) {

        if (context == null) {
            throw new IllegalArgumentException("Given context is null. Cannot retrieve reference count.");
        }

        synchronized (context) {

            setReferenceCount(context, getReferenceCount(context) + 1);
        }
    }

    public static void decrementReferenceCount(final Context context) {

        if (context == null) {
            throw new IllegalArgumentException("Given context is null. Cannot retrieve reference count.");
        }

        synchronized (context) {
            final int currentReferenceCount = getReferenceCount(context);

            if (currentReferenceCount >= 1) {

                setReferenceCount(context, currentReferenceCount - 1);
            } else {

                throw new IllegalStateException("Reference count cannot be reduced below 0.");
            }
        }
    }
}
