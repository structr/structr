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

import java.text.MessageFormat;
import java.util.*;
import java.util.Map.Entry;
import org.apache.click.Context;
import org.apache.click.control.*;
import org.apache.click.dataprovider.*;
import org.apache.click.util.*;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.module.GetEntitiesCommand;

/**
 *
 * @author amorgner
 */
public class CreateNode extends Nodes {

    private static final long serialVersionUID = 1L;
    @Bindable
    protected ActionLink createNodeLink = new ActionLink("createNodeLink", "Create Node", this, "onCreateNodeClick");
    @Bindable
    protected Table nodeTable = new Table("nodeTable") {
    };

    public CreateNode() {


        Column iconCol = new Column("iconSrc") {

            @Override
            public void renderTableData(Object row, HtmlStringBuffer buffer,
                    Context context, int rowIndex) {

                if (getMessageFormat() == null && getFormat() != null) {
                    Locale locale = context.getLocale();
                    setMessageFormat(new MessageFormat(getFormat(), locale));
                }

                buffer.elementStart("td");
                if (getRenderId()) {
                    buffer.appendAttribute("id", getId() + "_" + rowIndex);
                }
                buffer.appendAttribute("class", getDataClass());

                if (getTitleProperty() != null) {
                    Object titleValue = getProperty(getTitleProperty(), row);
                    buffer.appendAttributeEscaped("title", titleValue);
                }
                if (hasAttributes()) {
                    buffer.appendAttributes(getAttributes());
                }
                if (hasDataStyles()) {
                    buffer.appendStyleAttributes(getDataStyles());
                }
                buffer.appendAttribute("width", getWidth());
                buffer.closeTag();

                buffer.elementStart("img");
                buffer.appendAttribute("src", getContext().getRequest().getContextPath() + getProperty(row));
                buffer.elementEnd("img");

                //renderTableDataContent(row, buffer, context, rowIndex);

                buffer.elementEnd("td");
            }
        };

        //ActionLink createNodeLink = new ActionLink("createNodeLink", "Create AbstractNode", this, "onCreateNodeClick");
        //createNodeLink.setImageSrc(iconCol.getName());
        //AbstractLink[] links = new AbstractLink[] { createNodeLink };
        //iconCol.setDecorator(new LinkDecorator(nodeTable, links, iconCol.getName()));

        iconCol.setHeaderTitle("Icon");
        nodeTable.addColumn(iconCol);

        Column typeCol = new Column("key") {

            @Override
            public void renderTableData(Object row, HtmlStringBuffer buffer,
                    Context context, int rowIndex) {

                if (getMessageFormat() == null && getFormat() != null) {
                    Locale locale = context.getLocale();
                    setMessageFormat(new MessageFormat(getFormat(), locale));
                }

                buffer.elementStart("td");
                if (getRenderId()) {
                    buffer.appendAttribute("id", getId() + "_" + rowIndex);
                }
                buffer.appendAttribute("class", getDataClass());

                if (getTitleProperty() != null) {
                    Object titleValue = getProperty(getTitleProperty(), row);
                    buffer.appendAttributeEscaped("title", titleValue);
                }
                if (hasAttributes()) {
                    buffer.appendAttributes(getAttributes());
                }
                if (hasDataStyles()) {
                    buffer.appendStyleAttributes(getDataStyles());
                }
                buffer.appendAttribute("width", getWidth());
                buffer.closeTag();

                buffer.elementStart("img");
                buffer.appendAttribute("src", getContext().getRequest().getContextPath() + getProperty(row));
                buffer.elementEnd("img");

                createNodeLink = new ActionLink("createNodeLink", (String) getProperty(row), this, "onCreateNodeClick");
                createNodeLink.render(buffer);

                //renderTableDataContent(row, buffer, context, rowIndex);

                buffer.elementEnd("td");
            }
        };


        typeCol.setHeaderTitle("Type");
        nodeTable.addColumn(typeCol);

        nodeTable.setDataProvider(new DataProvider() {

            @Override
            public List<NodeType> getData() {
                Map<String, Class> entities = (Map<String, Class>) Services.command(GetEntitiesCommand.class).execute();
                List<NodeType> list = new LinkedList<NodeType>();

                for (Entry<String, Class> entry : entities.entrySet()) {
                    String n = entry.getKey();
                    Class c = entry.getValue();

                    list.add(new NodeType(n, c));
                }

                return list;
            }
        });

    }

    public static class NodeType {

        public NodeType(String key, Class implClass) {

            this.key = key;
            this.implClass = implClass;
        }
        private String key;
        private Class<AbstractNode> implClass;

        public String getKey() {
            return key;
        }

        public String getIconSrc() {
            try {
		    // FIXME: does onNodeInstantiation() need to be called here?
                return implClass.newInstance().getIconSrc();
            } catch (Throwable ignore) {
                return "/images/error.png";
            }
        }

        public Class getImplClass() {
            return implClass;
        }
    }
}
