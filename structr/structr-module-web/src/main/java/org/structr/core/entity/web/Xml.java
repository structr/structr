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
package org.structr.core.entity.web;

import org.structr.core.entity.PlainText;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.node.NodeFactoryCommand;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.neo4j.graphdb.Direction;
import org.structr.core.node.NodeRelationshipsCommand;
import org.structr.common.RelType;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.StructrRelationship;
import org.structr.core.entity.User;

/**
 * 
 * @author amorgner
 * 
 */
public class Xml extends PlainText {

    private final static String keyPrefix = "${";
    private final static String keySuffix = "}";
    private final static String ICON_SRC = "/images/page_white_code_red.png";

    @Override
    public String getIconSrc() {
        return ICON_SRC;
    }
    private final static String XML_KEY = "xml";

    public String getXml() {
        return (String) getProperty(XML_KEY);
    }

    public void setXml(String text) {
        setProperty(XML_KEY, text);
    }

    /**
     * Render XML content as HTML, replace keys by values
     */
    @Override
    public void renderView(StringBuilder out, final AbstractNode startNode,
            final String editUrl, final Long editNodeId, final User user) {

        if (isVisible(user)) {
            StringBuilder xml = new StringBuilder(getXml());

            // start with first occurrence of key prefix
            int start = xml.indexOf(keyPrefix);

            while (start > -1) {

                int end = xml.indexOf(keySuffix, start + keyPrefix.length());
                String key = xml.substring(start + keyPrefix.length(), end);

                //System.out.println("Key to replace: '" + key + "'");

                StringBuilder replacement = new StringBuilder();

                // first, look for a property with name=key
                if (dbNode.hasProperty(key)) {

                    replacement.append(dbNode.getProperty(key));

                } else {

                    Command nodeFactory = Services.command(NodeFactoryCommand.class);
                    Command relsCommand = Services.command(NodeRelationshipsCommand.class);

                    List<StructrRelationship> rels = (List<StructrRelationship>) relsCommand.execute(this, RelType.HAS_CHILD, Direction.OUTGOING);
                    for (StructrRelationship r : rels) {

                        AbstractNode s = (AbstractNode) nodeFactory.execute(r.getEndNode());

                        if (key.equals(s.getName())) {
                            s.renderView(replacement, startNode, editUrl, editNodeId, user);
                        }


                    }

                    rels = (List<StructrRelationship>) relsCommand.execute(this, RelType.LINK, Direction.OUTGOING);
                    for (StructrRelationship r : rels) {

                        AbstractNode s = (AbstractNode) nodeFactory.execute(r.getEndNode());

                        if (key.equals(s.getName())) {
                            s.renderView(replacement, startNode, editUrl, editNodeId, user);
                        }

                    }


                }

                xml.replace(start, end + keySuffix.length(), replacement.toString());

                start = xml.indexOf(keyPrefix, end + keySuffix.length() + 1);

            }


            out.append(xml);
        }
    }

    /**
     * Stream content directly to output.
     *
     * @param out
     */
    @Override
    public void renderDirect(OutputStream out, final AbstractNode startNode,
            final String editUrl, final Long editNodeId, final User user) {


        if (isVisible(user)) {
            try {

                StringReader in = new StringReader(getXml());

                // just copy to output stream
                IOUtils.copy(in, out);

            } catch (IOException e) {
                System.out.println("Error while rendering " + getXml() + ": " + e.getMessage());
            }

        }

    }
}
