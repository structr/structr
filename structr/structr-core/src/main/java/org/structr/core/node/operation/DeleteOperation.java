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
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.node.DeleteNodeCommand;
import org.structr.core.node.FindNodeCommand;

/**
 *
 * @author Christian Morgner
 */
public class DeleteOperation implements PrimaryOperation {

	private List<Callback> callbacks = new LinkedList<Callback>();
	private List<String> parameters = new LinkedList<String>();
	private SecurityContext securityContext = null;
	private AbstractNode currentNode = null;
	private boolean recursive = false;

	@Override
	public boolean executeOperation(StringBuilder stdOut) throws NodeCommandException {

		Command deleteCommand = Services.command(securityContext, DeleteNodeCommand.class);
		List<AbstractNode> toDelete = new LinkedList<AbstractNode>();
		AbstractNode newParentNode = currentNode;

		for(String param : parameters) {

			if(".".equals(param)) {

				stdOut.append("Cannot delete current node");

			} else {

				Object findNodeResult = Services.command(securityContext, FindNodeCommand.class).execute(currentNode, param);
				if(findNodeResult != null) {

					if(findNodeResult instanceof Collection) {

						toDelete.addAll((Collection)findNodeResult);

					} else if(findNodeResult instanceof AbstractNode) {

						toDelete.add((AbstractNode)findNodeResult);
					}
				}

				for(AbstractNode node : toDelete) {

					try
					{
						// execute delete node command and call callbacks
						deleteCommand.execute(node, currentNode, recursive, securityContext.getUser());

						if(deleteCommand.getExitCode().equals(Command.exitCode.FAILURE)) {
							stdOut.append(deleteCommand.getErrorMessage());
						}

					} catch(Throwable t) {

						stdOut.append(t.getMessage());
					}
				}
			}

		}

		// call callbacks
		if(newParentNode != null) {

			for(Callback callback : callbacks) callback.callback(newParentNode.getId());
			return(true);
		}

		return(false);
	}

	@Override
	public String help() {

		StringBuilder ret = new StringBuilder(100);
		ret.append("usage: rm [-r] [node(s)] - remove nodes, -r toggles recursive delete, wildcards allowed");

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

		return("rm");
	}

	@Override
	public boolean canExecute() {

		return(!parameters.isEmpty());
	}

	@Override
	public void addSwitch(String switches) throws InvalidSwitchException {

		if(switches.startsWith("-") && switches.length() > 1) {

			String sw = switches.substring(1);
			int len = sw.length();

			for(int i=0; i<len; i++) {

				char ch = sw.charAt(i);
				switch(ch) {

					case 'r':
						recursive = true;
						break;

					default:
						throw new InvalidSwitchException("Invalid switch " + ch);
				}
			}

		} else {

			throw new InvalidSwitchException("Invalid switch " + switches);
		}
	}

	@Override
	public void addParameter(Object parameter) throws InvalidParameterException {

		if(parameter instanceof Collection) {

			parameters.addAll((Collection<String>)parameter);

		} else {

			parameters.add(parameter.toString());
		}
	}

	@Override
	public void setSecurityContext(SecurityContext securityContext) {
		this.securityContext = securityContext;
	}
}
