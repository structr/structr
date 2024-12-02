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
package org.structr.core.converter;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Date;

/**
 *  Converts java temporal to a regular date object
 */
public abstract class TemporalDateConverter {

    public static Date convert(final Object inst) {

        if (inst == null) {
            return null;
        }

        if (inst instanceof Date date) {

            return date;
        } else if (inst instanceof ZonedDateTime zdt) {

            return Date.from(zdt.toInstant());
        } else if (inst instanceof Instant i) {

            return Date.from(i);
        }

        return null;
    }
}
