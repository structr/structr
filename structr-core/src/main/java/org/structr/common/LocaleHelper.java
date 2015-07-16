/**
 * Copyright (C) 2010-2015 Morgner UG (haftungsbeschränkt)
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
package org.structr.common;

import java.util.Locale;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.LocaleUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * This class encapsulates the handling of the locale parameter
 * 
 * @author kaischwaiger
 */
public class LocaleHelper {
	public static final String LOCALE_KEY = "locale";

	/**
	 * Determine the effective locale for this request.
	 *
	 * Priority 1: URL parameter "locale" Priority 2: Browser locale
	 *
	 * @param request
	 * @return locale
	 */
	public static Locale getEffectiveLocale(final HttpServletRequest request) {

		if (request == null) {
			return Locale.getDefault();
		}

		// Overwrite locale if requested by URL parameter
		String requestedLocaleString = request.getParameter(LOCALE_KEY);
		Locale locale = request.getLocale();
		if (StringUtils.isNotBlank(requestedLocaleString)) {
			try {
				locale = LocaleUtils.toLocale(requestedLocaleString);
			} catch (IllegalArgumentException e) {
				locale = Locale.forLanguageTag(requestedLocaleString);
			}
		}

		return locale;

	}
}
