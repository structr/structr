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

import java.util.LinkedList;
import java.util.List;
import org.apache.click.Context;
import org.apache.click.control.AbstractLink;
import org.apache.click.control.Checkbox;
import org.apache.click.control.Column;
import org.apache.click.control.FieldSet;
import org.apache.click.control.Form;
import org.apache.click.control.Option;
import org.apache.click.control.PageLink;
import org.apache.click.control.TextField;
import org.apache.click.control.Panel;
import org.apache.click.control.Select;
import org.apache.click.control.Submit;
import org.apache.click.control.Table;
import org.apache.click.dataprovider.DataProvider;
import org.apache.click.extras.control.AutoCompleteTextField;
import org.apache.click.extras.control.LinkDecorator;
import org.apache.click.util.Bindable;
import org.apache.click.util.HtmlStringBuffer;
import org.apache.commons.lang.StringUtils;
import org.structr.core.node.search.Search;
import org.structr.core.node.search.SearchOperator;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.node.search.SearchAttribute;
import org.structr.core.node.search.SearchNodeCommand;

/**
 *
 * @author amorgner
 */
public class SearchResults extends Nodes {

    private final static String SEARCH_IN_NAME_KEY = "inName";
    private final static String SEARCH_IN_TITLE_KEY = "inTitle";
    private final static String SEARCH_IN_CONTENT_KEY = "inContent";
    private static final String SEARCH_OPERATOR_KEY = "searchOp";
//    private static final String SEARCH_TOP_NODE_KEY = "topNode";
//    private static final String SEARCH_ONLY_PUBLIC_KEY = "onlyPublic";
//    private static final String SEARCH_INCLUDE_DELETED_KEY = "onlyPublic";
    // defaults
    private static boolean inNameChecked = true;
    private static boolean inTitleChecked = true;
    private static boolean inContentChecked = false;
    @Bindable
    protected Form advancedSearchForm = new Form("advancedSearchForm");
    @Bindable
    protected Panel advancedSearchPanel = new Panel("advancedSearchPanel", "/panel/advanced-search-panel.htm");
    @Bindable
    protected Panel searchResultsPanel = new Panel("searchResultsPanel", "/panel/search-results-panel.htm");
    @Bindable
    protected Table searchResultsTable = new Table();
    @Bindable
    protected TextField searchTextField = new TextField(SEARCH_TEXT_KEY, "Search for");
    
    // common fields
    @Bindable
    protected Select searchOpSelect = new Select(SEARCH_OPERATOR_KEY, "Boolean Search Operator");
    @Bindable
    protected Select typeSearch = new Select(AbstractNode.Key.type.name(), "Name");
    @Bindable
    protected Checkbox inNameCheckbox = new Checkbox(SEARCH_IN_NAME_KEY, "Name");
    @Bindable
    protected Checkbox inTitleCheckbox = new Checkbox(SEARCH_IN_TITLE_KEY, "Title");
    @Bindable
    protected Checkbox inContentCheckbox = new Checkbox(SEARCH_IN_CONTENT_KEY, "Content");
    @Bindable
    protected TextField creatorSearch = new TextField(AbstractNode.Key.createdBy.name(), "Created By");
    //@Bindable
    //protected TextField contentSearch = new TextField(PlainText.CONTENT_KEY, "Content");
    protected FieldSet searchFields = new FieldSet("Textual Search");

    public SearchResults() {

        super();
        searchFields.setColumns(5);
        //advancedSearchForm.add(typeSearch);
        searchFields.add(searchTextField);
        searchFields.add(inNameCheckbox);
        searchFields.add(inTitleCheckbox);
        searchFields.add(inContentCheckbox);
        //advancedSearchForm.add(titleSearch);
        //advancedSearchForm.add(contentSearch);
        searchOpSelect.add(new Option(SearchOperator.OR));
        searchOpSelect.add(new Option(SearchOperator.AND));
        searchFields.add(searchOpSelect);

        advancedSearchForm.setActionURL(advancedSearchForm.getActionURL().concat("#search-tab"));
        advancedSearchForm.add(searchFields);

        advancedSearchForm.add(new Submit("Search"));
        advancedSearchForm.setListener(this, "onAdvancedSearch");
        advancedSearchForm.add(new Submit("Reset", this, "onReset"));

        Column actionColumnNodes = new Column("Actions");
        actionColumnNodes.setTextAlign("center");
        actionColumnNodes.setDecorator(new LinkDecorator(searchResultsTable, new PageLink(), AbstractNode.Key.nodeId.name()) {

            @Override
            protected void renderActionLink(HtmlStringBuffer buffer, AbstractLink link, Context context, Object row, Object value) {

                AbstractNode n = (AbstractNode) row;
                link = new PageLink(AbstractNode.Key.nodeId.name(), getEditPageClass(n)) {

                    @Override
                    public String getHref() {

                        if (getPageClass() == null) {
                            throw new IllegalStateException("target pageClass is not defined");
                        }

                        Context context = getContext();
                        HtmlStringBuffer buffer = new HtmlStringBuffer();

                        buffer.append(context.getRequest().getContextPath());

                        String pagePath = context.getPagePath(getPageClass());

                        if (pagePath != null && pagePath.endsWith(".jsp")) {
                            pagePath = StringUtils.replace(pagePath, ".jsp", ".htm");
                        }

                        buffer.append(pagePath);

                        if (hasParameters()) {
                            buffer.append("?");
                            renderParameters(buffer, getParameters(), context);
                        }

                        //buffer.append("#properties-tab");

                        return context.getResponse().encodeURL(buffer.toString());
                    }
                };

                link.setParameter(NODE_ID_KEY, n.getId());
                link.setImageSrc("/images/table-edit.png");

                super.renderActionLink(buffer, link, context, row, value);

            }
        });

        Column typeColumn = new Column(AbstractNode.Key.type.name());

        LinkDecorator iconDec = new LinkDecorator(searchResultsTable, new PageLink(), AbstractNode.Key.nodeId.name()) {

            @Override
            protected void renderActionLink(HtmlStringBuffer buffer, AbstractLink link, Context context, Object row, Object value) {

                AbstractNode n = (AbstractNode) row;
                link = new PageLink(AbstractNode.Key.nodeId.name(), getEditPageClass(n));
                link.setParameter(NODE_ID_KEY, n.getId());
                link.setImageSrc(n.getIconSrc());

                super.renderActionLink(buffer, link, context, row, value);

            }
        };
        typeColumn.setDecorator(iconDec);
        searchResultsTable.addColumn(typeColumn);

        Column nameColumn = new Column(AbstractNode.Key.name.name());
        LinkDecorator nameDec = new LinkDecorator(searchResultsTable, new PageLink(), "id") {

            @Override
            protected void renderActionLink(HtmlStringBuffer buffer, AbstractLink link, Context context, Object row, Object value) {


                AbstractNode n = (AbstractNode) row;

                PageLink pageLink = new PageLink("id", getEditPageClass(n)) {

                    @Override
                    public String getHref() {

                        if (getPageClass() == null) {
                            throw new IllegalStateException("target pageClass is not defined");
                        }

                        Context context = getContext();
                        HtmlStringBuffer buffer = new HtmlStringBuffer();

                        buffer.append(context.getRequest().getContextPath());

                        String pagePath = context.getPagePath(getPageClass());

                        if (pagePath != null && pagePath.endsWith(".jsp")) {
                            pagePath = StringUtils.replace(pagePath, ".jsp", ".htm");
                        }

                        buffer.append(pagePath);

                        if (hasParameters()) {
                            buffer.append("?");
                            renderParameters(buffer, getParameters(), context);
                        }

                        //buffer.append("#childnodes-tab");

                        return context.getResponse().encodeURL(buffer.toString());
                    }
                };

                pageLink.setParameter(NODE_ID_KEY, n.getId());
                pageLink.setLabel(n.getName());

                super.renderActionLink(buffer, pageLink, context, row, value);

            }
        };
        nameColumn.setDecorator(nameDec);
        searchResultsTable.addColumn(nameColumn);
        searchResultsTable.addColumn(new Column(AbstractNode.Key.lastModifiedDate.name()));
        searchResultsTable.addColumn(new Column(AbstractNode.Key.owner.name()));
        searchResultsTable.addColumn(new Column(AbstractNode.Key.createdBy.name()));
        searchResultsTable.addColumn(new Column(AbstractNode.Key.createdDate.name()));
        searchResultsTable.addColumn(new Column(AbstractNode.Key.position.name()));
        searchResultsTable.addColumn(new Column(AbstractNode.Key.visibleToPublicUsers.name()));
        searchResultsTable.addColumn(actionColumnNodes);
        searchResultsTable.setSortable(true);
        searchResultsTable.setShowBanner(true);
        searchResultsTable.setPageSize(DEFAULT_PAGESIZE);
        searchResultsTable.getControlLink().setParameter(AbstractNode.Key.nodeId.name(), getNodeId());
        searchResultsTable.setClass(TABLE_CLASS);

    }

    @Override
    public void onRender() {
        super.onRender();
        setDefaults();
        restoreState();
        populateSearchResultsTable();
        
        //advancedSearchForm.add(new HiddenField(AbstractNode.Key.nodeId.name(), getNodeId()));*
    }

    public boolean onReset() {
        resetForm();
        return false;
    }

    public void setDefaults() {

        searchOpSelect.setValue(SearchOperator.OR.toString());
        searchTextField.setValue(null);
        inNameCheckbox.setChecked(inNameChecked);
        inTitleCheckbox.setChecked(inTitleChecked);
        inContentCheckbox.setChecked(inContentChecked);

    }

    public void resetForm() {

        Context context = getContext();

        // remove state and set defaults
        searchResults = null;
        searchResultsTable.removeState(context);
        searchOpSelect.removeState(context);
        searchTextField.removeState(context);
        inNameCheckbox.removeState(context);
        inTitleCheckbox.removeState(context);
        inContentCheckbox.removeState(context);

        setDefaults();
    }

    public void saveState() {

        Context context = getContext();

        // save state of search form controls
        context.setSessionAttribute(SEARCH_RESULTS_KEY, searchResults);
        searchResultsTable.saveState(context);
        searchOpSelect.saveState(context);
        searchTextField.saveState(context);
        inNameCheckbox.saveState(context);
        inTitleCheckbox.saveState(context);
        inContentCheckbox.saveState(context);

    }

    public void restoreState() {

        Context context = getContext();

        // restore state from session
        searchResults = (List<AbstractNode>) context.getSessionAttribute(SEARCH_RESULTS_KEY);
        searchResultsTable.restoreState(context);
        searchOpSelect.restoreState(context);
        searchTextField.restoreState(context);
        inNameCheckbox.restoreState(context);
        inTitleCheckbox.restoreState(context);
        inContentCheckbox.restoreState(context);

    }

    public void populateSearchResultsTable() {
        if (searchResults != null && !(searchResults.isEmpty())) {
            searchResultsTable.getControlLink().setParameter(AbstractNode.Key.nodeId.name(), getNodeId());
            searchResultsTable.setDataProvider(new DataProvider() {

                @Override
                public List<AbstractNode> getData() {
                    return (List<AbstractNode>) searchResults;
                }
            });
        }
    }

    /**
     * Simple search
     */
    public boolean onSimpleSearch() {

        if (simpleSearchForm.isValid()) {

            // retrieve search results
            String searchText = searchTextField.getValue();
            List<SearchAttribute> searchAttrs = new LinkedList<SearchAttribute>();

            searchAttrs.add(Search.andName(searchText));

            searchResults = (List<AbstractNode>) Services.command(securityContext, SearchNodeCommand.class).execute(
                    null, // top node
                    false, // include deleted
                    false, // only public
                    searchAttrs);

            populateSearchResultsTable();
            setDefaults();
            saveState();

        }
        return false;

    }

    /**
     * Advanced search
     */
    public boolean onAdvancedSearch() {

        if (advancedSearchForm.isValid()) {

            // assemble search attributes
            List<SearchAttribute> searchAttrs = new LinkedList<SearchAttribute>();

            String searchText = searchTextField.getValue();
            if (inNameCheckbox.isChecked()) {
                searchAttrs.add(Search.orName(searchText));
            }
            if (inTitleCheckbox.isChecked()) {
                searchAttrs.add(Search.orTitle(searchText));
            }
            if (inContentCheckbox.isChecked()) {
                searchAttrs.add(Search.orContent(searchText));
            }

            searchResults = (List<AbstractNode>) Services.command(securityContext, SearchNodeCommand.class).execute(null, false, false, searchAttrs);
            populateSearchResultsTable();
            saveState();

        }
        return false;

    }
}
