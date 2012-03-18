/*
 *  Copyright (C) 2012 Axel Morgner
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

package org.structr.android.restclient;

/**
 * An exception that encapsulates the HTTP response code
 * and phrase of a REST operation.
 * 
 * @author Christian Morgner
 */
public class StructrException extends Throwable {
	
	private String responsePhrase = null;
	private int responseCode = 0;
	
	public StructrException(int responseCode, String responsePhrase) {
		this.responsePhrase = responsePhrase;
		this.responseCode = responseCode;
	}
	
	@Override
	public String getMessage() {
		return "Error " + responseCode + ": " + responsePhrase;
	}
}
