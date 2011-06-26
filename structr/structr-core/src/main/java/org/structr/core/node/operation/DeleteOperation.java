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
import org.structr.common.CurrentRequest;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.node.DeleteNodeCommand;
import org.structr.core.node.FindNodeCommand;

/**
 *
 * @author Christian Morgner
 */
public class DeleteOperation implements PrimaryOperation, BooleanParameterOperation {

	private Command deleteCommand = Services.command(DeleteNodeCommand.class);
	private List<Callback> callbacks = new LinkedList<Callback>();
	private List<String> parameters = new LinkedList<String>();
	private AbstractNode currentNode = null;
	private boolean recursive = false;

	@Override
	public boolean executeOperation(StringBuilder stdOut) throws NodeCommandException {

		AbstractNode newParentNode = null;

		for(String param : parameters) {

			if(".".equals(param)) {

				stdOut.append("Cannot delete current node");

			} else {
				long nodeId = -1;

				try {

					nodeId = Long.parseLong(param.toString());

				} catch(Throwable t) {

					AbstractNode findNode = (AbstractNode)Services.command(FindNodeCommand.class).execute(CurrentRequest.getCurrentUser(), currentNode, param);
					if(findNode != null) {

						nodeId = findNode.getId();

					} else {

						nodeId = -1;
					}
				}

				if(nodeId > 0) {

					if(nodeId == currentNode.getId()) {

						newParentNode = currentNode.getParentNode();
					}

					// execute delete node command and call callbacks
					AbstractNode returnedNode = (AbstractNode)deleteCommand.execute(nodeId, null, recursive, CurrentRequest.getCurrentUser());
					if(returnedNode != null && newParentNode == null)
					{
						newParentNode = returnedNode;
					}

					Command.exitCode exitCode = deleteCommand.getExitCode();
					switch(exitCode) {
						case FAILURE:
							stdOut.append(deleteCommand.getErrorMessage()).append("\n");
							break;

						case SUCCESS:
							stdOut.append("Node [").append(nodeId).append("] deleted.\n");
							break;
					}

				} else {

					stdOut.append("Node not found");
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
		ret.append("usage: rm [node(s)] [rec] - remove nodes");

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
	public void addParameter(Object parameter) throws InvalidParameterException {

		if(parameter instanceof Collection) {

			parameters.addAll((Collection<String>)parameter);

		} else {

			parameters.add(parameter.toString());
		}
	}

	// ----- interface BooleanParameterOperation -----
	@Override
	public void setBooleanParameter(String name, boolean value) {

		if("recursive".equals(name)) {

			this.recursive = value;
		}
	}
}
