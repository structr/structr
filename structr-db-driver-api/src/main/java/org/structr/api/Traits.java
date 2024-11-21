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
package org.structr.api;

import org.apache.commons.lang.StringUtils;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

public class Traits {

        private Set<Class> traits = new LinkedHashSet<>();
        private String name       = null;

        public Traits(final String name) {
                this.name = name;
        }

        public String getName() {
                return name;
        }

        public void add(final Class c) {
                traits.add(c);
        }

        public static Traits of(final Class... list) {

                final Traits traits = new Traits(Traits.createName(list));

                for (final Class t : list) {
                        traits.add(t);
                }

                return traits;
        }

        public static String createName(final Class... list) {

                final Set<String> set = new TreeSet<>();

                for (final Class c : list) {

                        set.add(c.getSimpleName());
                }

                return StringUtils.join(set, "");
        }
}
