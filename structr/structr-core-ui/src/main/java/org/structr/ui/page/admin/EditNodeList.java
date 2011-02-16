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
import org.apache.click.control.Table;
import org.apache.click.dataprovider.DataProvider;
import org.structr.core.entity.NodeList;
import org.structr.core.entity.StructrNode;

/**
 *
 * @author chrisi
 */
public class EditNodeList extends DefaultEdit {

    private static final Logger logger = Logger.getLogger(EditNodeList.class.getName());
    //@Bindable
    //protected Table childNodesTable = new Table("nodeListTable");
    private NodeList<StructrNode> nodeList;

    public EditNodeList() {

        childNodesTable.setSortable(true);
        childNodesTable.setShowBanner(true);
        childNodesTable.setPageSize(DEFAULT_PAGESIZE);
        childNodesTable.setClass(Table.CLASS_SIMPLE);
        childNodesTable.setSortedColumn(StructrNode.NODE_ID_KEY);
        childNodesTable.setHoverRows(true);
        
        editChildNodesPanel = new Panel("editChildNodesPanel", "/panel/edit-child-nodes-panel.htm");
    }

    @Override
    public void onInit() {

        super.onInit();


        if (node != null) {

            childNodesTable.getControlLink().setParameter(StructrNode.NODE_ID_KEY, getNodeId());
            
            nodeList = (NodeList<StructrNode>) node;

            StructrNode firstNode = nodeList.getFirstNode();
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

    @Override
    public void onRender() {

        childNodesTable.setDataProvider(new DataProvider() {
            @Override
            public List<StructrNode> getData() {

                // Make a copy of the node list to make sort work
                List<StructrNode> result = new LinkedList<StructrNode>();
                for (StructrNode n : nodeList) {
                    result.add(n);
                }
                return result;
//                return nodeList;
            }
        });

    }
}
