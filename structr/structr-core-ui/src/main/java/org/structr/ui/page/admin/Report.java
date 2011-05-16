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

import au.com.bytecode.opencsv.CSVWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.click.Context;
import org.apache.click.control.Checkbox;
import org.apache.click.control.Column;
import org.apache.click.control.Field;
import org.apache.click.control.FieldSet;
import org.apache.click.control.Form;
import org.apache.click.control.Option;
import org.apache.click.control.Panel;
import org.apache.click.control.Select;
import org.apache.click.control.Submit;
import org.apache.click.control.Table;
import org.apache.click.control.TextField;
import org.apache.click.dataprovider.DataProvider;
import org.apache.click.util.ClickUtils;
import org.apache.click.util.PropertyUtils;
import org.apache.commons.io.FileUtils;
import org.structr.common.RelType;
import org.structr.core.node.search.SearchOperator;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.entity.EmptyNode;
import org.structr.core.entity.AbstractNode;
import org.structr.core.module.GetEntitiesCommand;
import org.structr.core.module.GetEntityClassCommand;
import org.structr.core.node.CreateNodeCommand;
import org.structr.core.node.CreateRelationshipCommand;
import org.structr.core.node.NodeAttribute;
import org.structr.core.node.search.TextualSearchAttribute;
import org.structr.core.node.search.SearchNodeCommand;
import org.structr.core.node.StructrTransaction;
import org.structr.core.node.TransactionCommand;
import org.structr.core.node.search.Search;
import org.structr.core.node.search.SearchAttribute;

/**
 *
 * @author amorgner
 */
public class Report extends Nodes {

    private static final Logger logger = Logger.getLogger(Report.class.getName());
    private final static String TYPE_SELECT_KEY = "typeSelect";
    private final static String REPORT_COLUMNS_KEY = "reportColumns";
    private final static String CSV_SEPARATOR_CHAR = "csvSeparatorCharacter";
    private final static String CSV_QUOTE_CHAR = "csvQuoteChar";
    private final static String CSV_ESCAPE_CHAR = "csvEscapeChar";
    protected Form reportForm = new Form("reportForm");
    protected FieldSet propertyFields = new FieldSet("propertyFields");
    protected Panel reportPanel = new Panel("reportPanel", "/panel/report-panel.htm");
    protected Table reportTable = new Table("reportTable");
    protected Select resultTypeSelect = new Select(TYPE_SELECT_KEY, "Node Type", true);
    protected Select csvSeparatorChar = new Select(CSV_SEPARATOR_CHAR, "CSV Separator Character");
    protected Select csvQuoteChar = new Select(CSV_QUOTE_CHAR, "CSV Quote Character");
    protected Select csvEscapeChar = new Select(CSV_ESCAPE_CHAR, "CSV Escape Character");
    protected Submit previewReport = new Submit("previewReport", "Preview", this, "onPreviewReport");
    protected Submit createReport = new Submit("createAndSaveReport", "Create Report", this, "onCreateReport");
    protected Submit reset = new Submit("reset", "Reset Form");
    protected TextField reportName = new TextField("reportName", "Save Report as (filename): ");
    //protected Submit saveReport = new Submit("saveReport", "Save Report", this, "onSaveReport");
    protected List<AbstractNode> reportResults = new LinkedList<AbstractNode>();
    protected List<SearchAttribute> searchAttributes = new LinkedList<SearchAttribute>();
    protected List<Column> columns = new LinkedList<Column>();
    private String resultType;

    @Override
    public String getTemplate() {
        return "/admin/report.htm";
    }

    public Report() {

        super();

        csvSeparatorChar.add(new Option(","));
        csvSeparatorChar.add(new Option(";"));
        csvSeparatorChar.add(new Option("|"));
        csvSeparatorChar.add(new Option("#"));

        csvQuoteChar.add(new Option("\""));
        csvQuoteChar.add(new Option("'"));
        csvQuoteChar.add(new Option(""));

        csvEscapeChar.add(new Option(""));
        csvEscapeChar.add(new Option("\""));
        csvEscapeChar.add(new Option("\\"));
        csvEscapeChar.add(new Option("\\\\"));
        csvEscapeChar.add(new Option("'"));

        reportForm.add(csvSeparatorChar);
        reportForm.add(csvQuoteChar);
        reportForm.add(csvEscapeChar);

        // Get request data for selected type
        ClickUtils.bind(resultTypeSelect);
        reportForm.add(resultTypeSelect);
        reportForm.add(reportName);

        String resultTypeFromRequest = resultTypeSelect.getValue();

        // Temporarily get type from session
        resultTypeSelect.restoreState(getContext());
        String resultTypeFromSession = resultTypeSelect.getValue();

        if (resultTypeFromRequest != null && !(resultTypeFromRequest.equals(resultTypeFromSession))) {
            // Change of type requested
            resultType = resultTypeFromRequest;
            ClickUtils.bind(resultTypeSelect);
        } else {
            resultType = resultTypeFromSession;
        }

        reportTable.setSortable(true);
        reportTable.setShowBanner(true);
        reportTable.setPageSize(DEFAULT_PAGESIZE);
//        reportTable.getControlLink().setParameter(AbstractNode.NODE_ID_KEY, getNodeId());
        reportTable.setClass(TABLE_CLASS);

        populateTypeSelectField();

        ClickUtils.bind(reset);

        if (!(reset.isClicked())) {

            if (resultType != null && !(resultType.isEmpty())) {
                createDynamicFields(resultType);
            } else {
                restoreState();
            }

        }

        propertyFields.setColumns(2);
        reportForm.add(propertyFields);

        //ClickUtils.bind(reportForm);
        reportForm.add(previewReport);
        reportForm.add(createReport);
        reportForm.add(reset);

        addControl(reportForm);
        addControl(reportPanel);
        addControl(reportTable);
        addControl(resultTypeSelect);

    }

    @Override
    public void onRender() {
        super.onRender();
        //setDefaults();
        //restoreState();
        //populateReportResultsTable();
        if (reportForm.isValid()) {
            // Always filter by type
            searchAttributes.add(Search.andExactType(resultType));
            searchResults = (List<AbstractNode>) Services.command(SearchNodeCommand.class).execute(user, null, false, false, searchAttributes);
            populateReportResultsTable();
            saveState();
        }
    }

    @Override
    public void onPost() {

        if (reset.isClicked()) {
            clearState();
            resetForm();
        }
        super.onPost();

    }

    public void setDefaults() {
//        resultTypeSelect.setValue(SearchOperator.OR.toString());
    }

    public void resetForm() {
        reportResults = new LinkedList<AbstractNode>();
        columns = new LinkedList<Column>();
        resultTypeSelect = new Select(TYPE_SELECT_KEY, "Select Result Type", true);
        reportForm.clearValues();
        reportTable = new Table("reportTable");
    }

    public void clearState() {
        Context context = getContext();

        context.removeSessionAttribute(REPORT_RESULTS_KEY);
        context.removeSessionAttribute(REPORT_COLUMNS_KEY);
        resultTypeSelect.removeState(context);
        reportForm.removeState(context);
        reportTable.removeState(context);
    }

    public void saveState() {
        Context context = getContext();

        // save state of search form controls
        context.setSessionAttribute(REPORT_RESULTS_KEY, reportResults);
        context.setSessionAttribute(REPORT_COLUMNS_KEY, columns);
        resultTypeSelect.saveState(context);
        reportForm.saveState(context);
        reportTable.saveState(context);
    }

    public void restoreState() {
        Context context = getContext();

        if (context.hasSessionAttribute(REPORT_RESULTS_KEY)) {

            reportForm.add(resultTypeSelect);
            // restore state from session
            reportResults = (List<AbstractNode>) context.getSessionAttribute(REPORT_RESULTS_KEY);
            resultTypeSelect.restoreState(context);
            columns = (List<Column>) context.getSessionAttribute(REPORT_COLUMNS_KEY);
            if (columns != null) {
                for (Column c : columns) {
                    reportTable.addColumn(c);
                    String fieldName = c.getName();
                    TextField field = new TextField(fieldName);
                    propertyFields.add(field);
                    Checkbox checkbox = new Checkbox(checkBoxName(fieldName), "");
                    propertyFields.add(checkbox);
                }
            } else {
                columns = new LinkedList<Column>();
            }
            reportForm.add(propertyFields);
            reportForm.restoreState(context);

            if (columns != null) {
                for (Column c : columns) {
                    String fieldName = c.getName();
                    String fieldValue = reportForm.getFieldValue(fieldName);
                    searchAttributes.add(new TextualSearchAttribute(fieldName, fieldValue, SearchOperator.AND));
                }
            } else {
                columns = new LinkedList<Column>();
            }

            reportTable.restoreState(context);
        }
    }

    public void populateReportResultsTable() {
        if (reportResults != null && !(reportResults.isEmpty())) {
            //reportTable.getControlLink().setParameter(AbstractNode.NODE_ID_KEY, getNodeId());
            reportTable.setDataProvider(new DataProvider() {

                @Override
                public List<AbstractNode> getData() {
                    return (List<AbstractNode>) reportResults;
                }
            });
        }
    }

    /**
     * Create report
     */
    public boolean onPreviewReport() {

        if (reportForm.isValid()) {

            // Always filter by type
            searchAttributes.add(Search.andExactType(resultType));
            reportResults = (List<AbstractNode>) Services.command(SearchNodeCommand.class).execute(user, null, false, false, searchAttributes);
            populateReportResultsTable();
            saveState();

        }

        return false;

    }

    /**
     * Create report
     */
    public boolean onCreateReport() {

        onPreviewReport();

        final String reportFileName = reportName.getValue() + ".csv";

        Map methodCache = new HashMap<Object, Object>();
        List<String[]> resultList = new LinkedList<String[]>();

        List<String> keys = new LinkedList<String>();
        for (Column c : columns) {
            keys.add(c.getName());
        }

        // add column headings
        resultList.add((keys.toArray(new String[keys.size()])));

        if (reportResults == null) {
            return false;
        }

        for (AbstractNode s : reportResults) {


//            List<String> cols = new LinkedList<String>();
            List<String> values = new LinkedList<String>();
            for (Column c : columns) {

                try {

                    Object value = PropertyUtils.getValue(s, c.getName(), methodCache);
                    if (value != null) {
                        values.add(value.toString());
                    } else {
                        values.add("");
                    }

                } catch (RuntimeException re) {
                    continue;
                }
            }

            String[] sa = values.toArray(new String[values.size()]);
            resultList.add(sa);
        }

        final File reportFile = new File("/tmp/" + reportFileName);
        CSVWriter csvw = null;

        try {

            char sepChar;
            char quoteChar;
            char escChar;

            String sepFieldValue = reportForm.getFieldValue(CSV_SEPARATOR_CHAR);
            String quoteFieldValue = reportForm.getFieldValue(CSV_QUOTE_CHAR);
            String escFieldValue = reportForm.getFieldValue(CSV_ESCAPE_CHAR);

            if (sepFieldValue != null && !(sepFieldValue.isEmpty())) {
                sepChar = sepFieldValue.charAt(0);

                if (quoteFieldValue != null && !(quoteFieldValue.isEmpty())) {
                    quoteChar = quoteFieldValue.charAt(0);

                    if (escFieldValue != null && !(escFieldValue.isEmpty())) {
                        escChar = escFieldValue.charAt(0);

                        csvw = new CSVWriter(new FileWriter(reportFile),
                                sepChar, quoteChar, escChar);
                    } else {
                        csvw = new CSVWriter(new FileWriter(reportFile),
                                sepChar, quoteChar);
                    }

                } else {
                    csvw = new CSVWriter(new FileWriter(reportFile),
                            sepChar);

                }
            }

            csvw.writeAll(resultList);
            csvw.flush();
            csvw.close();

            AbstractNode s = null;
            Command transaction = Services.command(TransactionCommand.class);

            s = (AbstractNode) transaction.execute(new StructrTransaction() {

                @Override
                public Object execute() throws Throwable {
                    // Save report in database
                    Command createNode = Services.command(CreateNodeCommand.class);
                    Command createRel = Services.command(CreateRelationshipCommand.class);

                    // create node with appropriate type
                    AbstractNode newNode = (AbstractNode) createNode.execute(new NodeAttribute(AbstractNode.TYPE_KEY, File.class.getSimpleName()), user);


                    String relativeFilePath = newNode.getId() + "_" + System.currentTimeMillis();
                    String targetPath = FILES_PATH + "/" + relativeFilePath;

                    String fileUrl = "file:///" + reportFile.getPath();
                    FileUtils.moveFile(reportFile, new File(targetPath));

                    Date now = new Date();
                    newNode.setProperty(AbstractNode.NAME_KEY, reportFileName);
                    newNode.setProperty(AbstractNode.CREATED_DATE_KEY, now);
                    newNode.setProperty(AbstractNode.LAST_MODIFIED_DATE_KEY, now);

                    newNode.setProperty(org.structr.core.entity.File.CONTENT_TYPE_KEY, "text/csv");
                    newNode.setProperty(org.structr.core.entity.File.SIZE_KEY, String.valueOf(reportFile.length()));
                    newNode.setProperty(org.structr.core.entity.File.URL_KEY, fileUrl);
                    newNode.setProperty(org.structr.core.entity.File.RELATIVE_FILE_PATH_KEY, relativeFilePath);

                    // connect report to user node
                    AbstractNode parentNode = user;
                    createRel.execute(parentNode, newNode, RelType.HAS_CHILD);

                    return newNode;
                }
            });


        } catch (IOException ex) {
            Logger.getLogger(Report.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (csvw != null) {
                try {
                    csvw.close();
                } catch (IOException ignore) {
                }
            }
        }

        return false;

    }

    private void createDynamicFields(final String resultType) {

        // Get the corresponding entity class
        //Class<AbstractNode> c = Services.getEntityClass(resultType);
        Class<AbstractNode> c = (Class<AbstractNode>) Services.command(GetEntityClassCommand.class).execute(resultType);

        if (c != null) {

            // Get all the node property fields
            java.lang.reflect.Field[] fields = c.getFields();
            //reportFields.setColumns(fields.length);

            AbstractNode o = new EmptyNode();
            try {

                // Instantiate an object to get the value of the public static fields
                o = (AbstractNode) c.newInstance();
            } catch (Throwable t) {
                logger.log(Level.SEVERE, null, t);
            }

            if (fields != null) {
                for (java.lang.reflect.Field f : fields) {
                    String fieldName = null;
                    try {

                        fieldName = (String) f.get(o);

                        // Type is already there
                        if (AbstractNode.TYPE_KEY.equals(fieldName)) {
                            continue;
                        }

                        TextField text = new TextField(fieldName);

                        // Populate input field with values from request
                        text.bindRequestValue();
                        propertyFields.add(text);

                        Checkbox check = new Checkbox(checkBoxName(fieldName), "");
                        check.bindRequestValue();
                        propertyFields.add(check);

                        String fieldValue = reportForm.getFieldValue(fieldName);

                        if (fieldValue != null && !(fieldValue.isEmpty())) {
                            searchAttributes.add(new TextualSearchAttribute(fieldName, fieldValue, SearchOperator.AND));
                            //field.setValue(fieldValue);
                        }

                        Field field = propertyFields.getField(checkBoxName(fieldName));

                        Checkbox checkbox = null;
                        if (field instanceof Checkbox) {
                            checkbox = (Checkbox) field;
                        }

                        if (checkbox.isChecked()) {
                            Column col;
                            col = new Column(fieldName);
                            reportTable.addColumn(col);
                            columns.add(col);
                        }

                        reportForm.add(propertyFields);

                    } catch (Throwable t) {
                        logger.log(Level.SEVERE, null, t);
                    }



                }
            }

        }
    }

    private void populateTypeSelectField() {
        resultTypeSelect.add(new Option("", "--- Select Node Type ---"));
        resultTypeSelect.setAttribute("onchange", "form.submit();");

        List<String> nodeTypes = new LinkedList<String>(((Map<String, Class>) Services.command(GetEntitiesCommand.class).execute()).keySet());
        Collections.sort(nodeTypes);

        for (String className : nodeTypes) {
            Option o = new Option(className);
            resultTypeSelect.add(o);
        }
    }

    private String checkBoxName(final String name) {
        return name + "Checkbox";
    }
}
