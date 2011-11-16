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

import java.util.List;
import java.util.logging.Logger;
import org.apache.click.control.Column;
import org.apache.click.control.Table;
import org.apache.click.dataprovider.DataProvider;
import org.apache.click.util.Bindable;
import org.structr.core.entity.AbstractNode;

/**
 * Sessions page
 * 
 * Displays sessions list
 * 
 * @author axel
 */
public class AllNodes extends Admin {

    private static final Logger logger = Logger.getLogger(AllNodes.class.getName());
    @Bindable
    protected Table allNodesTable = new Table("allNodesTable");
    @Override
    public String getTemplate() {
        return "/maintenance-template.htm";
    }

    public AllNodes() {

        super();
//        maintenancePanel = new Panel("maintenancePanel", "/panel/maintenance-panel.htm");

        allNodesTable.addColumn(new Column(AbstractNode.Key.nodeId.name()));
        allNodesTable.addColumn(new Column(AbstractNode.Key.name.name()));
        allNodesTable.addColumn(new Column(AbstractNode.Key.type.name()));
        allNodesTable.addColumn(new Column(AbstractNode.Key.position.name()));
        allNodesTable.addColumn(new Column(AbstractNode.Key.visibleToPublicUsers.name()));
        allNodesTable.addColumn(new Column(AbstractNode.Key.owner.name()));
        allNodesTable.addColumn(new Column(AbstractNode.Key.createdBy.name()));
        allNodesTable.addColumn(new Column(AbstractNode.Key.createdDate.name()));
        allNodesTable.addColumn(new Column("allProperties"));
        allNodesTable.setSortable(true);
        allNodesTable.setPageSize(15);
        allNodesTable.setHoverRows(true);
        allNodesTable.setShowBanner(true);
        allNodesTable.setClass(TABLE_CLASS);


    }

    @Override
    public void onInit() {
        super.onInit();
    }

    @Override
    public void onRender() {

        allNodesTable.setDataProvider(new DataProvider() {

            @Override
            public List<AbstractNode> getData() {

                return getAllNodes();

            }
        });

    }

}
