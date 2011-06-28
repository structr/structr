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

import java.text.DecimalFormat;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import org.structr.common.CurrentRequest;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.node.FindNodeCommand;

/**
 *
 * @author Christian Morgner
 */
public class ListOperation implements PrimaryOperation {

	private List<AbstractNode> nodeList = new LinkedList<AbstractNode>();
	private AbstractNode currentNode = null;
	private boolean recursive = false;

	@Override
	public boolean executeOperation(StringBuilder stdOut) throws NodeCommandException {

		// default: list contents of current node
		if(nodeList.isEmpty()) {
			
			nodeList.add(currentNode);
		}

		for(AbstractNode node : nodeList) {

			list(stdOut, node);

		}

		return(true);
	}

	@Override
	public void addCallback(Callback callback) {
	}

	@Override
	public String help() {

		return("usage: ls [node(s)] - print node(s)");
	}

	@Override
	public void setCurrentNode(AbstractNode currentNode) {

		this.currentNode = currentNode;
	}

	@Override
	public int getParameterCount() {

		return(1);
	}

	@Override
	public String getKeyword() {

		return("ls");
	}

	@Override
	public boolean canExecute() {

		return(true);
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

			for(Object o : (Collection)parameter) {

				add(o);
			}


		} else {

			add(parameter);
		}
	}

	private void add(Object obj) {

		Object findNodeResult = Services.command(FindNodeCommand.class).execute(CurrentRequest.getCurrentUser(), currentNode, obj);

		if(findNodeResult != null) {

			if(findNodeResult instanceof AbstractNode) {

				nodeList.add((AbstractNode)findNodeResult);

			} else if(findNodeResult instanceof Collection) {

				for(Object o : (Collection)findNodeResult) {

					if(o instanceof AbstractNode) {

						nodeList.add((AbstractNode)o);
					}
				}
			}
		}
	}

	private void list(StringBuilder stdOut, AbstractNode node) {

		List<AbstractNode> children = node.getSortedDirectChildNodes();
		DecimalFormat df = new DecimalFormat("0000000");

		if(!children.isEmpty()) {

			stdOut.append("<ul>");
			for(AbstractNode child : children) {

				stdOut.append("<li>");
				stdOut.append("<p>");
				stdOut.append(df.format(child.getId()));
				stdOut.append(" ");
				stdOut.append(child.getName());
				stdOut.append(" (");
				stdOut.append(child.getType());
				stdOut.append(")");
				stdOut.append("</p>");

				if(recursive) {

					list(stdOut, child);
				}

				stdOut.append("</li>");
			}
			stdOut.append("</ul>");
		}
	}
}
