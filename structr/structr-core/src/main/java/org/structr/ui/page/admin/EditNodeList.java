/*
 *  Copyright (C) 2011 Axel Morgner, structr <structr@structr.org>
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
package org.structr.ui.page.admin;

import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.click.control.Column;
import org.apache.click.control.Panel;
import org.apache.click.dataprovider.DataProvider;
import org.apache.click.extras.control.FormTable;
import org.structr.core.entity.NodeList;
import org.structr.core.entity.AbstractNode;

/**
 *
 * @author chrisi
 */
public class EditNodeList extends DefaultEdit {

    private static final Logger logger = Logger.getLogger(EditNodeList.class.getName());
    protected NodeList<AbstractNode> nodeList;
    protected FormTable nodeListTable = new FormTable("nodeListTable");

    public EditNodeList() {
        
        super();

        nodeListTable.setSortable(true);
        nodeListTable.setShowBanner(true);
        nodeListTable.setPageSize(DEFAULT_PAGESIZE);
        nodeListTable.setClass(TABLE_CLASS);
        //nodeListTable.setSortedColumn(AbstractNode.Key.nodeId.name());
        nodeListTable.setHoverRows(true);
        addControl(nodeListTable);

        editChildNodesPanel = new Panel("editChildNodesPanel", "/panel/edit-child-nodes-panel.htm");
        addControl(editChildNodesPanel);
    }

    @Override
    public void onInit() {

        super.onInit();

        if (node != null) {

            nodeListTable.getControlLink().setParameter(AbstractNode.Key.nodeId.name(), getNodeId());

            nodeList = (NodeList<AbstractNode>) node;

            AbstractNode firstNode = nodeList.getFirstNode();

            if (firstNode != null) {

                Field[] fields = firstNode.getClass().getFields();
                for (Field f : fields) {
                    String fieldName;

                    try {
                        fieldName = (String) f.get(firstNode);
                        Column col;
                        col = new Column(fieldName);
                        nodeListTable.addColumn(col);
                        
                    } catch (IllegalAccessException ex) {
                        logger.log(Level.SEVERE, null, ex);
                    }
                }
                
            }

        }

    }

    @Override
    public void onRender() {
        
        super.onRender();

        nodeListTable.setDataProvider(new DataProvider() {

            @Override
            public List<AbstractNode> getData() {

                // Make a copy of the node list to make sort work
                List<AbstractNode> result = new LinkedList<AbstractNode>();
                for (AbstractNode n : nodeList) {
                    result.add(n);
                }
                return result;
//                return nodeList;
            }
        });

    }
}
