/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with Structr. If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core;

import java.util.HashMap;
import java.util.Map;
import org.structr.common.SecurityContext;

/**
 * The base class for all types of commands.
 *
 * <p>
 * This class contains some convenience methods to deal with the <code>parameters</code> array of the execute method, as well as a Map and accessor methods to inject arguments into newly instantiated
 * command instances.
 * </p>
 *
 * <p>
 * The <code>getServiceClass()</code> method returns the runtime class of the service this command belongs to. It is a good practice to create an abstract base class for your service that returns the
 * appropriate service class, and derive all commands you create for that service from this base class.
 * </p>
 *
 * @author Christian Morgner
 */
public abstract class Command {

	protected SecurityContext securityContext = null;
	protected Map<String, Object> arguments = null;
	protected Command.Status status = null;
	protected Command.ExitCode exitCode = null;
	private final static String EXIT_CODE = "exitCode";
	private Map<String, Object> exitStatus;

	public enum Status {

		WAITING, RUNNING, FINISHED
	}

	public enum ExitCode {

		UNKNOWN, SUCCESS, FAILURE
	}

	public Command() {

		this.arguments = new HashMap<>();
		this.exitStatus = new HashMap<>();

		// Set default
		exitStatus.put(EXIT_CODE, exitCode.UNKNOWN);
	}

	/**
	 * Returns the service class this command belongs to. Implement this method in an abstrac base class and derive all you service commands from this class.
	 *
	 * @return the service this command belongs to
	 */
	public abstract Class getServiceClass();

	/**
	 * Sets an argument for this command. Call this method from within
     * {
	 *
	 * @see org.structr.core.Service#injectArguments}.
	 *
	 * @param key the key
	 * @param value the value
	 */
	public final void setArgument(String key, Object value) {
		if (key != null && value != null) {
			this.arguments.put(key, value);
		}
	}

	/**
	 * Returns a previously set argument for this command.
	 *
	 * @param key the key
	 * @return the argument or null if no such argument exists.
	 */
	public final Object getArgument(String key) {
		return (this.arguments.get(key));
	}

	public void setSecurityContext(SecurityContext securityContext) {
		this.securityContext = securityContext;
	}
}
