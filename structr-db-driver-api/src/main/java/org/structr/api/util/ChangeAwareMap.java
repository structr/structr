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
package org.structr.api.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * An extended LinkedHashMap that records write operations that happened after
 * the map was initialized in order to be able to identify modified values.
 */
public class ChangeAwareMap {

        private final Map<String, Object> data = new HashMap<>();
        private final Set<String> modifiedKeys = new HashSet<>();

        public ChangeAwareMap() {
        }

        public ChangeAwareMap(final Map<String, Object> initialData) {
                data.putAll(initialData);
        }

        public ChangeAwareMap(final ChangeAwareMap initialData) {
                data.putAll(initialData.data);
        }

        public void putAll(final Map<String, Object> input) {

                for (final Entry<String, Object> entry : input.entrySet()) {

                        final String key   = entry.getKey();
                        final Object value = entry.getValue();

                        put(key, value);
                }
        }

        public boolean containsKey(final String key) {
                return data.containsKey(key);
        }

        public Object get(final String key) {
                return data.get(key);
        }

        public Object put(final String key, final Object value) {
                modifiedKeys.add(key);
                return data.put(key, value);
        }

        public Set<String> keySet() {
                return data.keySet();
        }

        public Set<String> getModifiedKeys() {
                return modifiedKeys;
        }

        public Object remove(final String key) {
                return data.remove(key);
        }

        public int size() {
                return data.size();
        }

        public Set<Entry<String, Object>> entrySet() {
                return data.entrySet();
        }
}
