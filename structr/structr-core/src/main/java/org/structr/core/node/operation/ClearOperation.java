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

import org.structr.common.CurrentSession;
import org.structr.core.entity.AbstractNode;
import org.structr.core.node.NodeConsoleCommand;

/**
 *
 * @author Christian Morgner
 */
public class ClearOperation implements PrimaryOperation {

	@Override
	public boolean executeOperation(StringBuilder stdOut) throws NodeCommandException {

		CurrentSession.getSession().removeAttribute(NodeConsoleCommand.CONSOLE_BUFFER_KEY);

		return(true);
	}

	@Override
	public void addCallback(Callback callback) {

	}

	@Override
	public String help() {

		StringBuilder ret = new StringBuilder(100);
		ret.append("usage: clear - clears the console output buffer");

		return(ret.toString());
	}

	@Override
	public void setCurrentNode(AbstractNode currentNode) {
	}

	@Override
	public int getParameterCount() {

		return(0);
	}

	@Override
	public String getKeyword() {

		return("clear");
	}

	@Override
	public boolean canExecute() {

		return(true);
	}

	@Override
	public void addSwitch(String switches) throws InvalidSwitchException {

		throw new InvalidSwitchException("CLEAR does not support " + switches);
	}

	@Override
	public void addParameter(Object parameter) throws InvalidParameterException {

		throw new InvalidParameterException("CLEAR does not take parameters");
	}
}
