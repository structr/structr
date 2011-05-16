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
package org.structr.core.entity.app;

import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang.StringUtils;
import org.structr.common.AbstractNodeComparator;
import org.structr.common.CurrentRequest;
import org.structr.common.CurrentSession;
import org.structr.common.RelType;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.User;

/**
 *
 * @author Christian Morgner
 */
public class AppList extends AppNodeView {

    private static final Logger logger = Logger.getLogger(AppList.class.getName());
    private static final String PAGE_NO_PARAMETER_NAME_KEY = "pageNoParameterName";
    private static final String PAGE_SIZE_PARAMETER_NAME_KEY = "pageSizeParameterName";
    private static final String SORT_KEY_PARAMETER_NAME_KEY = "sortKeyParameterName";
    private static final String SORT_ORDER_PARAMETER_NAME_KEY = "sortOrderParameterName";
    // defaults
    private String sortKeyParameterName = "sortKey";
    private String sortOrderParameterName = "sortOrder";
    private String pageNoParameterName = "pageNo";
    private String pageSizeParameterName = "pageSize";
    private String sortKey = "name";
    private String sortOrder = "";
    private int pageNo = 0;
    private int pageSize = 10;

    @Override
    public String getIconSrc() {
        return ("/images/application_side_list.png");
    }

    private void init() {

                    String sortKeyProperty = getStringProperty(SORT_KEY_PARAMETER_NAME_KEY);
                    if (StringUtils.isNotEmpty(sortKeyProperty)) {
                        sortKeyParameterName = sortKeyProperty;
                    }
                    if (StringUtils.isNotEmpty(sortKeyParameterName)) {
                        String sortKeyValue = CurrentRequest.getRequest().getParameter(sortKeyParameterName);
                        if (sortKeyValue != null) {
                            sortKey = sortKeyValue;
                        }
                    }


                    String sortOrderProperty = getStringProperty(SORT_ORDER_PARAMETER_NAME_KEY);
                    if (StringUtils.isNotEmpty(sortOrderProperty)) {
                        sortOrderParameterName = sortOrderProperty;
                    }
                    if (StringUtils.isNotEmpty(sortOrderParameterName)) {
                        String sortOrderValue = CurrentRequest.getRequest().getParameter(sortOrderParameterName);
                        if (sortOrderValue != null) {
                            sortOrder = sortOrderValue;
                        }
                    }


                    String pageNoProperty = getStringProperty(PAGE_NO_PARAMETER_NAME_KEY);
                    if (StringUtils.isNotEmpty(pageNoProperty)) {
                        pageNoParameterName = pageNoProperty;
                    }
                    if (StringUtils.isNotEmpty(pageNoParameterName)) {
                        String pageNoValue = CurrentRequest.getRequest().getParameter(pageNoParameterName);
                        if (pageNoValue != null) {
                            pageNo = Integer.parseInt(pageNoValue);
                        }
                    }


                    String pageSizeProperty = getStringProperty(PAGE_SIZE_PARAMETER_NAME_KEY);
                    if (StringUtils.isNotEmpty(pageSizeProperty)) {
                        pageSizeParameterName = pageSizeProperty;
                    }
                    if (StringUtils.isNotEmpty(pageSizeParameterName)) {
                        String pageSizeValue = CurrentRequest.getRequest().getParameter(pageSizeParameterName);
                        if (pageSizeValue != null) {
                            pageSize = Integer.parseInt(pageSizeValue);
                        }
                    }

                    sortKey = toGetter(sortKey);
    }

    @Override
    public void renderView(StringBuilder out, AbstractNode startNode, String editUrl, Long editNodeId, User user) {

        if (isVisible(user)) {

            if (hasTemplate(user)) {

                String html = template.getContent();

                if (StringUtils.isNotBlank(html)) {

                    init();

                    // iterate over children following the DATA relationship
                    for (AbstractNode container : getSortedDirectChildren(RelType.DATA, user)) {

                        List<AbstractNode> nodes = container.getDirectChildNodes(user);
                        Collections.sort(nodes, new AbstractNodeComparator(sortKey, sortOrder));

                        int toIndex = Math.min(((pageNo + 1) * pageSize), nodes.size());
                        int fromIndex = Math.min(pageNo * pageSize, toIndex);

                        // iterate over direct children of the given node
                        for (AbstractNode node : nodes.subList(fromIndex, toIndex)) {

                            doRendering(out, this, node, editUrl, editNodeId, user);
                        }
                    }

                } else {
                    logger.log(Level.WARNING, "No template!");
                }
            }

        } else {
            logger.log(Level.WARNING, "Node not visible");
        }
    }

    public int getSize() {

        User user = CurrentSession.getUser();
        int size = 0;

        // iterate over children following the DATA relationship
        for (AbstractNode container : getSortedDirectChildren(RelType.DATA, user)) {
            List<AbstractNode> nodes = container.getDirectChildNodes(user);
            size += nodes.size();
        }
        return size;
    }

    public String getPager() {

        StringBuilder out = new StringBuilder();
        
        init();
        
        int noOfPages = Math.abs(getSize() / pageSize) + 1;

        out.append("<ul>");

        for (int i = 0; i < noOfPages; i++) {
            out.append("<li");
            if (i == pageNo) {
                out.append(" class=\"current\"");
            }
            out.append("><a href=\"?pageSize=").append(pageSize).append("&pageNo=").append(i).append("\">").append(i).append("</a></li>");

        }

        out.append("</ul>");

        return out.toString();

    }

    @Override
    public void onNodeCreation() {
    }

    @Override
    public void onNodeInstantiation() {
    }
}
