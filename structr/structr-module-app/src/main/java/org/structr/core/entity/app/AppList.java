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
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang.StringUtils;
import org.structr.common.AbstractNodeComparator;
import org.structr.common.CurrentRequest;
import org.structr.common.RelType;
import org.structr.common.StructrOutputStream;
import org.structr.core.entity.AbstractNode;

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
    private int pageNo = 1;
    private int pageSize = 10;
    private int lastPage = -1;

    @Override
    public String getIconSrc() {
        return ("/images/application_side_list.png");
    }

    private void init() {

        sortKey = getStringParameterValue(SORT_KEY_PARAMETER_NAME_KEY, sortKeyParameterName, sortKey);
        sortOrder = getStringParameterValue(SORT_ORDER_PARAMETER_NAME_KEY, sortOrderParameterName, sortOrder);
        pageNo = getIntParameterValue(PAGE_NO_PARAMETER_NAME_KEY, pageNoParameterName, pageNo);
        if (pageNo < 1) {
            pageNo = 1;
        }
        if (pageSize < 1) {
            pageSize = 1;
        }

        pageSize = getIntParameterValue(PAGE_SIZE_PARAMETER_NAME_KEY, pageSizeParameterName, pageSize);

        lastPage = Math.abs(getSize() / pageSize);
        if (getSize() % pageSize > 0) {
            lastPage++;
        }
    }

    private String getStringParameterValue(final String namePropertyKey, final String defaultParameterName, final String defaultValue) {
        String nameValue = defaultParameterName;
        String propertyValue = getStringProperty(namePropertyKey);
        if (StringUtils.isNotEmpty(propertyValue)) {
            nameValue = propertyValue;
        }
        String value = defaultValue;
        if (StringUtils.isNotEmpty(nameValue)) {
            String parameterValue = CurrentRequest.getRequest().getParameter(nameValue);
            if (StringUtils.isNotEmpty(parameterValue)) {
                value = parameterValue;
            }
        }
        return value;
    }

    private int getIntParameterValue(final String namePropertyKey, final String defaultParameterName, final int defaultValue) {
        String nameValue = defaultParameterName;
        String propertyValue = getStringProperty(namePropertyKey);
        if (StringUtils.isNotEmpty(propertyValue)) {
            nameValue = propertyValue;
        }
        int value = defaultValue;
        if (StringUtils.isNotEmpty(nameValue)) {
            String parameterValue = CurrentRequest.getRequest().getParameter(nameValue);
            if (StringUtils.isNotEmpty(parameterValue)) {
                value = Integer.parseInt(parameterValue);
            }
        }
        return value;
    }

    @Override
    public void renderNode(StructrOutputStream out, final AbstractNode startNode, final String editUrl, final Long editNodeId) {

        if (isVisible()) {

            if (hasTemplate()) {

                String html = template.getContent();

                if (StringUtils.isNotBlank(html)) {

                    init();

                    List<AbstractNode> nodesToRender = new LinkedList<AbstractNode>();

                    // iterate over children following the DATA relationship and collect all nodes
                    for (AbstractNode container : getSortedDirectChildren(RelType.DATA)) {
                        nodesToRender.addAll(container.getDirectChildNodes());
                    }

                    Collections.sort(nodesToRender, new AbstractNodeComparator(toGetter(sortKey), sortOrder));

                    int toIndex = Math.min(pageNo * pageSize, nodesToRender.size());
                    int fromIndex = Math.min(Math.max(pageNo - 1, 0) * pageSize, toIndex);

                    logger.log(Level.INFO, "Showing list elements from {0} to {1}", new Object[]{fromIndex, toIndex});

                    // iterate over direct children of the given node
                    for (AbstractNode node : nodesToRender.subList(fromIndex, toIndex)) {

                        doRendering(out, this, node, editUrl, editNodeId);
                    }



                } else {
                    logger.log(Level.WARNING, "No template!");
                }
            }

        } else {
            logger.log(Level.WARNING, "Node not visible");
        }
    }

    public int getLastPageNo() {
        if (lastPage == -1) {
            init();
        }
        return lastPage;
    }

    public int getSize() {

        int size = 0;

        // iterate over children following the DATA relationship
        for (AbstractNode container : getSortedDirectChildren(RelType.DATA)) {
            List<AbstractNode> nodes = container.getDirectChildNodes();
            size += nodes.size();
        }
        return size;
    }

    public String getPager() {

        StringBuilder out = new StringBuilder();

        init();

        out.append("<ul>");

        if (pageNo > 1) {
            out.append("<li><a href=\"?pageSize=").append(pageSize).append("&pageNo=").append(pageNo - 1).append("&sortKey=").append(sortKey).append("&sortOrder=").append(sortOrder).append("\">").append(" &lt; previous (").append(pageNo - 1).append(")").append("</a></li>");
        }

        for (int i = 1; i <= lastPage; i++) {

            // if we have more than 10 pages, skip some pages
            if (lastPage > 10
                    && (i < pageNo - 5 || i > pageNo + 5)
                    && (i < lastPage - 5 && i > 5)) {
                continue;
            }

            out.append("<li");
            if (i == pageNo) {
                out.append(" class=\"current\"");
            }
            out.append("><a href=\"?pageSize=").append(pageSize).append("&pageNo=").append(i).append("&sortKey=").append(sortKey).append("&sortOrder=").append(sortOrder).append("\">").append(i).append("</a></li>");

        }


        if (pageNo < lastPage) {
            out.append("<li><a href=\"?pageSize=").append(pageSize).append("&pageNo=").append(pageNo + 1).append("&sortKey=").append(sortKey).append("&sortOrder=").append(sortOrder).append("\">").append("next (").append(pageNo + 1).append(") &gt;").append("</a></li>");
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
