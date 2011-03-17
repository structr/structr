/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
import org.structr.core.entity.NodeList;
import org.structr.core.entity.AbstractNode;

/**
 *
 * @author chrisi
 */
public class EditNodeList extends DefaultEdit {

    private static final Logger logger = Logger.getLogger(EditNodeList.class.getName());
    protected NodeList<AbstractNode> nodeList;

    public EditNodeList() {

        childNodesTable.setSortable(true);
        childNodesTable.setShowBanner(true);
        childNodesTable.setPageSize(DEFAULT_PAGESIZE);
        childNodesTable.setClass(TABLE_CLASS);
        childNodesTable.setSortedColumn(AbstractNode.NODE_ID_KEY);
        childNodesTable.setHoverRows(true);
        addControl(childNodesTable);

        editChildNodesPanel = new Panel("editChildNodesPanel", "/panel/edit-child-nodes-panel.htm");
        addControl(editChildNodesPanel);
    }

    @Override
    public void onInit() {

        super.onInit();

        if (node != null) {

            childNodesTable.getControlLink().setParameter(AbstractNode.NODE_ID_KEY, getNodeId());

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
                        childNodesTable.addColumn(col);
                        
                    } catch (IllegalAccessException ex) {
                        logger.log(Level.SEVERE, null, ex);
                    }
                }
                
            }

        }

    }

    @Override
    public void onRender() {

        childNodesTable.setDataProvider(new DataProvider() {

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
