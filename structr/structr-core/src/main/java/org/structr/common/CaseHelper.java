/*
 *  Copyright (C) 2011 Axel Morgner, structr <structr@structr.org>
 * 
 *  This file is part of structr <http://structr.org>.
 * 
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.common;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;

/**
 *
 * @author axel
 */
public class CaseHelper {
	
	public static String toUpperCamelCase(final String input) {
		return WordUtils.capitalize(input, new char[]{'_'}).replaceAll("_", "");
	}

	public static String toLowerCamelCase(final String input) {
		return input.substring(0,1).toLowerCase().concat(WordUtils.capitalize(input, new char[]{'_'}).replaceAll("_", "").substring(1));
	}

	public static void main(String[] args) {

		String[] input = {
			"check_ins",
			"CheckIns"
		};

		for(int i=0; i<input.length; i++) {
			System.out.println(StringUtils.rightPad(input[i], 20) + StringUtils.leftPad(toUpperCamelCase(input[i]), 20));
		}
	}
}