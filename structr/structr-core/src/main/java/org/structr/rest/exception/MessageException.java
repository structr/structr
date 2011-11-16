/*
 *  Copyright (C) 2011 Axel Morgner
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

package org.structr.rest.exception;

/**
 * An exception that can be used to pass a response message from
 * module-specific code to the response body.
 *
 * @author Christian Morgner
 */
public class MessageException extends PathException {

	private String msg = null;

	public MessageException(int status, String msg) {
		super(status);
		this.msg = msg;
	}

	@Override
	public String getMessage() {
		return msg;
	}
}
