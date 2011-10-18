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
import java.util.LinkedList;
import java.util.List;
import org.structr.common.SecurityContext;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.node.FindNodeCommand;

/**
 *
 * @author Christian Morgner
 */
public class CdOperation implements PrimaryOperation {

	private List<Callback> callbacks = new LinkedList<Callback>();
	private SecurityContext securityContext = null;
	private AbstractNode currentNode = null;
	private String target = null;

	@Override
	public boolean executeOperation(StringBuilder stdOut) throws NodeCommandException {

		if(target != null) {

			AbstractNode newLocation = (AbstractNode)Services.command(securityContext, FindNodeCommand.class).execute(
			    securityContext.getUser(),
			    currentNode,
			    target
			   );

			if(newLocation != null) {

				for(Callback callback : callbacks) { callback.callback(newLocation.getId()); }

				return(true);

			}

		} else {

			throw new NodeCommandException("No parameter");
		}

		return(false);
	}

	@Override
	public String help() {

		StringBuilder ret = new StringBuilder(100);
		ret.append("usage: cd [path] - change current node");

		return(ret.toString());
	}

	@Override
	public void setCurrentNode(AbstractNode currentNode) {
		this.currentNode = currentNode;
	}

	@Override
	public void addCallback(Callback callback) {

		callbacks.add(callback);
	}

	@Override
	public int getParameterCount() {

		return(1);
	}

	@Override
	public String getKeyword() {

		return("cd");
	}

	@Override
	public boolean canExecute() {

		return(target != null);
	}

	@Override
	public void addSwitch(String switches) throws InvalidSwitchException {

		throw new InvalidSwitchException("CD does not support " + switches);
	}

	@Override
	public void addParameter(Object parameter) throws InvalidParameterException {

		if(parameter instanceof Collection) {

			throw new InvalidParameterException("CD does not take multiple parameters");

		} else {

			target = parameter.toString();

		}

	}

	@Override
	public void setSecurityContext(SecurityContext securityContext) {
		this.securityContext = securityContext;
	}
}
