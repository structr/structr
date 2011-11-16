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

package org.structr.core.node.operation;

import java.util.Collection;
import org.structr.common.SecurityContext;
import org.structr.core.entity.AbstractNode;
import org.structr.core.node.NodeConsoleCommand;

/**
 *
 * @author Christian Morgner
 */
public class HelpOperation implements PrimaryOperation {

	private SecurityContext securityContext = null;
	private String commandToHelp = null;

	@Override
	public boolean executeOperation(StringBuilder stdOut) throws NodeCommandException {

		Class clazz = NodeConsoleCommand.operationsMap.get(commandToHelp);
		if (clazz != null) {

			try {

				PrimaryOperation op = (PrimaryOperation)clazz.newInstance();
				stdOut.append(op.help());

			} catch (Throwable t) {}
		}

		return(true);
	}

	@Override
	public void addCallback(Callback callback) {
	}

	@Override
	public String help() {

		return("usage: help [command] - display help for a command");
	}

	@Override
	public void setCurrentNode(AbstractNode currentNode) {
	}

	@Override
	public int getParameterCount() {
		return(1);
	}

	@Override
	public String getKeyword() {
		return("help");
	}

	@Override
	public boolean canExecute() {
		return(commandToHelp != null);
	}

	@Override
	public void addSwitch(String sw) throws InvalidSwitchException {
		throw new InvalidSwitchException("Invalid switch " + sw);
	}

	@Override
	public void addParameter(Object parameter) throws InvalidParameterException {

		if(parameter instanceof Collection) {

			throw new InvalidParameterException("HELP does not take multiple arguments");

		} else {

			commandToHelp = parameter.toString();

		}
	}

	@Override
	public void setSecurityContext(SecurityContext securityContext) {
		this.securityContext = securityContext;
	}
}
