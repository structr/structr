/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.ui.page.admin;

import java.text.MessageFormat;
import java.util.*;
import java.util.Map.Entry;
import org.apache.click.Context;
import org.apache.click.control.*;
import org.apache.click.dataprovider.*;
import org.apache.click.util.*;
//import org.structr.core.ClasspathEntityLocator;
import org.structr.core.Services;
import org.structr.core.entity.EmptyNode;
import org.structr.core.entity.StructrNode;
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

        //ActionLink createNodeLink = new ActionLink("createNodeLink", "Create StructrNode", this, "onCreateNodeClick");
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
                Map<String, Class> entities = (Map<String, Class>) Services.createCommand(GetEntitiesCommand.class).execute();
                List<NodeType> list = new ArrayList<NodeType>();

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
        private Class<StructrNode> implClass;

        public String getKey() {
            return key;
        }

        public String getIconSrc() {
            try {
                return implClass.newInstance().getIconSrc();
            } catch (Exception ignore) {
                return (new EmptyNode()).getIconSrc();
            }
        }

        public Class getImplClass() {
            return implClass;
        }
    }
}
