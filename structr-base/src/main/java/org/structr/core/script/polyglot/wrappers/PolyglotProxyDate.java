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
package org.structr.core.script.polyglot.wrappers;

import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.*;

import java.time.Instant;
import java.util.Date;
import java.util.Set;

public class PolyglotProxyDate implements ProxyObject, ProxyDate, ProxyTime, ProxyInstant {
    private static final Set<String> PROTOTYPE_FUNCTIONS = Set.of(
            "getTime",
            "getDate",
            "getHours",
            "getMinutes",
            "getSeconds",
            "setDate",
            "setHours",
            "toString"
    );

    final private Date date;

    public PolyglotProxyDate(final Date date) {
        this.date = date;
    }

    @Override
    public Instant asInstant() {
        return date.toInstant();
    }

    public Date getDateDelegate() {
        return date;
    }

    @Override
    public Object getMember(String key) {
        return switch (key) {
            case "getTime" -> (ProxyExecutable) arguments -> date.getTime();
            case "getDate" -> (ProxyExecutable) arguments -> date.getDate();
            case "setHours" -> (ProxyExecutable) arguments -> {
                date.setHours(arguments[0].asInt());
                return date.getTime();
            };
            case "setDate" -> (ProxyExecutable) arguments -> {
                date.setDate(arguments[0].asInt());
                return date.getTime();
            };
            case "toString" -> (ProxyExecutable) arguments -> date.toString();
            default -> throw new UnsupportedOperationException("This date does not support: " + key);
        };
    }

    @Override
    public Object getMemberKeys() {
        return PROTOTYPE_FUNCTIONS.toArray();
    }

    @Override
    public boolean hasMember(String key) {
        return PROTOTYPE_FUNCTIONS.contains(key);
    }

    @Override
    public void putMember(String key, Value value) {
        throw new UnsupportedOperationException("This date does not support adding new properties/functions.");
    }
}
