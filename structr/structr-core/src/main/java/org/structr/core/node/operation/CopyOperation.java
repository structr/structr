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
import org.structr.common.CurrentSession;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.node.CopyNodeCommand;
import org.structr.core.node.FindNodeCommand;
import org.structr.core.node.StructrTransaction;
import org.structr.core.node.TransactionCommand;

/**
 *
 * @author Christian Morgner
 */
public class CopyOperation implements PrimaryOperation {

	private ParameterState parameterState = ParameterState.NoNodeSet;

	private List<Callback> callbacks = new LinkedList<Callback>();
	private AbstractNode destinationNode = null;
	private AbstractNode currentNode = null;
	private AbstractNode sourceNode = null;

	private enum ParameterState {

		NoNodeSet,
		SourceNodeSet,
		DestinationNodeSet
	}

	@Override
	public boolean executeOperation(StringBuilder stdOut) throws NodeCommandException {

		if(parameterState.equals(ParameterState.DestinationNodeSet)) {

			Services.command(TransactionCommand.class).execute(new StructrTransaction() {

				@Override
				public Object execute() throws Throwable {

					Command moveCommand = Services.command(CopyNodeCommand.class);
					moveCommand.execute(sourceNode, destinationNode, CurrentSession.getUser());

					return(null);
				}

			});
		}

		return(true);
	}

	@Override
	public void addCallback(Callback callback) {
		callbacks.add(callback);
	}

	@Override
	public String help() {

		return("usage: cp [source] [destination] - copy source to destination");
	}

	@Override
	public void setCurrentNode(AbstractNode currentNode) {

		this.currentNode = currentNode;
	}

	@Override
	public int getParameterCount() {
		return(2);
	}

	@Override
	public String getKeyword() {
		return("cp");
	}

	@Override
	public boolean canExecute() {

		return(parameterState.equals(ParameterState.DestinationNodeSet));
	}

	@Override
	public void addSwitch(String switches) throws InvalidSwitchException {

		throw new InvalidSwitchException("CP does not support " + switches);
	}

	@Override
	public void addParameter(Object parameter) throws InvalidParameterException {

		switch(parameterState) {

			case NoNodeSet:
				sourceNode = getNode(parameter);
				parameterState = ParameterState.SourceNodeSet;
				break;

			case SourceNodeSet:
				destinationNode = getNode(parameter);
				parameterState = ParameterState.DestinationNodeSet;
				break;

			case DestinationNodeSet:
				throw new InvalidParameterException("MOVE only takes two parameters");

		}
	}

	// ----- private methods -----
	private AbstractNode getNode(Object parameter) throws InvalidParameterException {

		Command findNodeCommand = Services.command(FindNodeCommand.class);
		AbstractNode ret = null;

		if(parameter instanceof Collection) {

			throw new InvalidParameterException("MOVE does not take multiple arguments");

		} else {

			Object findNodeReturnValue = findNodeCommand.execute(CurrentSession.getUser(), currentNode, parameter);
			if(findNodeReturnValue instanceof Collection) {

				throw new InvalidParameterException("MOVE does not support wildcards");

			} else if(findNodeReturnValue instanceof AbstractNode) {

				ret = (AbstractNode)findNodeReturnValue;

			} else {

				throw new InvalidParameterException("Node " + parameter.toString() + " not found");
			}

		}

		return(ret);
	}

}
