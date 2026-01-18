/*
 * Copyright (C) 2010-2026 Structr GmbH
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
package org.structr.util;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public abstract class UrlUtils {

    public static Map<String, String> parseQueryString(final String query) {

        if (query == null || query.isEmpty()) {

            return Collections.EMPTY_MAP;
        }

        Map<String, String> queryParams = new HashMap<>();

        String[] pairs = query.split("&");

        for (String pair : pairs) {

            String[] keyValuePair = pair.split("=");
            queryParams.put(URLDecoder.decode(keyValuePair[0], StandardCharsets.UTF_8), URLDecoder.decode(keyValuePair[1], StandardCharsets.UTF_8));
        }

        return queryParams;
    }
}
