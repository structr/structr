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

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.structr.common.SecurityContext;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.StructrRelationship;

/**
 *
 * @author Christian Morgner
 */
public class ShowOperation implements PrimaryOperation, NodeListOperation {

	private List<AbstractNode> nodeList = new LinkedList<AbstractNode>();
	private List<Callback> callbacks = new LinkedList<Callback>();
	private SecurityContext securityContext = null;
	private ShowMode mode = ShowMode.None;

	private enum ShowMode {

		None, Properties, Relationships
	}

	@Override
	public boolean executeOperation(StringBuilder stdOut) throws NodeCommandException {

		boolean ret = true;

		stdOut.append("<table class=\"file-list\">");
		for(AbstractNode node : nodeList) {

			switch(mode) {

				case Properties:
					showProperties(stdOut, node);
					break;

				case Relationships:
					showRelationships(stdOut, node);
					break;
			}
		}
		stdOut.append("</table>");

		return(ret);

	}

	@Override
	public void addCallback(Callback callback) {
		callbacks.add(callback);
	}

	@Override
	public String help() {
		StringBuilder ret = new StringBuilder(100);
		ret.append("usage: show [props|rels] on [node(s)] - show properties/relationships of node(s) (wildcards allowed)");

		return(ret.toString());
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
		return("show");
	}

	@Override
	public boolean canExecute() {
		return(mode != ShowMode.None);
	}

	@Override
	public void addSwitch(String switches) throws InvalidSwitchException {
		throw new InvalidSwitchException("SET does not support " + switches);
	}

	@Override
	public void addParameter(Object parameter) throws InvalidParameterException {

		String str = parameter.toString();

		if("props".equals(str)) {

			mode = ShowMode.Properties;

		} else if("rels".equals(str)) {

			mode = ShowMode.Relationships;

		} else {

			throw new InvalidParameterException("Unknown parameter " +  parameter);
		}
	}

	@Override
	public void addNodeToList(AbstractNode node) {
		nodeList.add(node);
	}

	// ----- private methods -----
	private void showProperties(StringBuilder stdOut, AbstractNode node) {

		Map<String, Object> properties = new LinkedHashMap<String, Object>();
		Node dbNode = node.getNode();

		if(dbNode != null) {

			// use property keys of db node, as AbstractNode's getPropertyKeys()
			// can and will be overwritten
			Iterable<String> propertyKeys = dbNode.getPropertyKeys();
			boolean firstRow = true;

			// fill map with properties from node
			for(String key : propertyKeys) {
				properties.put(key, node.getProperty(key));
			}

			for(Entry<String, Object> entry : properties.entrySet()) {

				stdOut.append("<tr>");

				if(firstRow) {

					stdOut.append("<td class=\"listed-file-name\" rowspan=\"");
					stdOut.append(properties.size());
					stdOut.append("\">");
					stdOut.append(node.getName());
					stdOut.append("</td>");

					firstRow = false;
				}

				stdOut.append("<td class=\"listed-file-name\">");
				stdOut.append(entry.getKey());
				stdOut.append("</td>");
				stdOut.append("<td class=\"listed-file-id\">");
				stdOut.append(entry.getValue().toString());
				stdOut.append("</td>");
				stdOut.append("</tr>");
			}
		}
	}

	private void showRelationships(StringBuilder stdOut, AbstractNode node) {

		List<StructrRelationship> inRels = node.getRelationships(Direction.INCOMING);
		List<StructrRelationship> outRels = node.getRelationships(Direction.OUTGOING);
		int size = inRels.size() + outRels.size();
		boolean firstRow = true;

		for(StructrRelationship rel : outRels) {

			stdOut.append("<tr>");

			if(firstRow) {

				stdOut.append("<td class=\"listed-file-name\" rowspan=\"");
				stdOut.append(size);
				stdOut.append("\">");
				stdOut.append(node.getName());
				stdOut.append("</td>");

				firstRow = false;
			}

			stdOut.append("<td class=\"listed-file-id\">-&gt;</td>");
			stdOut.append("<td class=\"listed-file-id\">");
			stdOut.append(rel.getRelType());
			stdOut.append("</td>");
			stdOut.append("<td class=\"listed-file-id\">-&gt;</td>");
			stdOut.append("<td class=\"listed-file-type\">");
			stdOut.append(rel.getProperties());
			stdOut.append("</td>");
			stdOut.append("<td class=\"listed-file-id\">-&gt;</td>");
			stdOut.append("<td class=\"listed-file-id\">");
			stdOut.append(rel.getEndNode().getName());
			stdOut.append("</td>");
			stdOut.append("</tr>");
		}

		for(StructrRelationship rel : inRels) {

			stdOut.append("<tr>");

			if(firstRow) {

				stdOut.append("<td class=\"listed-file-name\" rowspan=\"");
				stdOut.append(size);
				stdOut.append("\">");
				stdOut.append(node.getName());
				stdOut.append("</td>");

				firstRow = false;
			}

			stdOut.append("<td class=\"listed-file-id\">&lt;-</td>");
			stdOut.append("<td class=\"listed-file-id\">");
			stdOut.append(rel.getRelType());
			stdOut.append("</td>");
			stdOut.append("<td class=\"listed-file-id\">&lt;-</td>");
			stdOut.append("<td class=\"listed-file-type\">");
			stdOut.append(rel.getProperties());
			stdOut.append("</td>");
			stdOut.append("<td class=\"listed-file-id\">&lt;-</td>");
			stdOut.append("<td class=\"listed-file-id\">");
			stdOut.append(rel.getStartNode().getName());
			stdOut.append("</td>");
			stdOut.append("</tr>");
		}
	}

	@Override
	public void setSecurityContext(SecurityContext securityContext) {
		this.securityContext = securityContext;
	}
}
