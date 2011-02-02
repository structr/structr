/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.ui.page.admin;

import au.com.bytecode.opencsv.CSVWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import org.structr.common.SearchOperator;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.entity.EmptyNode;
import org.structr.core.entity.StructrNode;
import org.structr.core.module.GetEntitiesCommand;
import org.structr.core.module.GetEntityClassCommand;
import org.structr.core.node.CreateNodeCommand;
import org.structr.core.node.CreateRelationshipCommand;
import org.structr.core.node.NodeAttribute;
import org.structr.core.node.SearchAttribute;
import org.structr.core.node.SearchNodeCommand;
import org.structr.core.node.StructrTransaction;
import org.structr.core.node.TransactionCommand;

/**
 *
 * @author amorgner
 */
public class Report extends Nodes {

    private final static String TYPE_SELECT_KEY = "typeSelect";
    private final static String REPORT_COLUMNS_KEY = "reportColumns";
    private final static String CSV_SEPARATOR_CHAR = "csvSeparatorCharacter";
    private final static String CSV_QUOTE_CHAR = "csvQuoteChar";
    private final static String CSV_ESCAPE_CHAR = "csvEscapeChar";
    protected Form reportForm = new Form("reportForm");
    protected FieldSet propertyFields = new FieldSet("propertyFields");
    protected Panel reportPanel = new Panel("reportPanel", "/panel/report-panel.htm");
    protected Table reportTable = new Table("reportTable");
    protected Select resultTypeSelect = new Select(TYPE_SELECT_KEY, "Select Result Type", true);
    protected Select csvSeparatorChar = new Select(CSV_SEPARATOR_CHAR, "CSV Separator Character");
    protected Select csvQuoteChar = new Select(CSV_QUOTE_CHAR, "CSV Quote Character");
    protected Select csvEscapeChar = new Select(CSV_ESCAPE_CHAR, "CSV Escape Character");
    protected Submit submit = new Submit("createReport", "Create Report", this, "onCreateReport");
    protected Submit reset = new Submit("reset", "Reset Form");
    protected TextField reportName = new TextField("reportName", "Save Report as (filename): ", true);
    //protected Submit saveReport = new Submit("saveReport", "Save Report", this, "onSaveReport");
    protected List<StructrNode> reportResults = new ArrayList<StructrNode>();
    protected List<SearchAttribute> searchAttributes = new ArrayList<SearchAttribute>();
    protected List<Column> columns = new ArrayList<Column>();

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

        String resultType;
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
//        reportTable.getControlLink().setParameter(StructrNode.NODE_ID_KEY, getNodeId());
        reportTable.setClass(Table.CLASS_SIMPLE);

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
        reportForm.add(submit);
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
            Command search = Services.createCommand(SearchNodeCommand.class);

            searchResults = (List<StructrNode>) search.execute(null, user, true, false, searchAttributes);
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
        reportResults = new ArrayList<StructrNode>();
        columns = new ArrayList<Column>();
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
            reportResults = (List<StructrNode>) context.getSessionAttribute(REPORT_RESULTS_KEY);
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
                columns = new ArrayList<Column>();
            }
            reportForm.add(propertyFields);
            reportForm.restoreState(context);

            if (columns != null) {
                for (Column c : columns) {
                    String fieldName = c.getName();
                    String fieldValue = reportForm.getFieldValue(fieldName);
                    searchAttributes.add(new SearchAttribute(fieldName, fieldValue, SearchOperator.AND));
                }
            } else {
                columns = new ArrayList<Column>();
            }

            reportTable.restoreState(context);
        }
    }

    public void populateReportResultsTable() {
        if (reportResults != null && !(reportResults.isEmpty())) {
            //reportTable.getControlLink().setParameter(StructrNode.NODE_ID_KEY, getNodeId());
            reportTable.setDataProvider(new DataProvider() {

                @Override
                public List<StructrNode> getData() {
                    return (List<StructrNode>) reportResults;
                }
            });
        }
    }

    /**
     * Create report
     */
    public boolean onCreateReport() {

        final String reportFileName = reportName.getValue() + ".csv";

        if (reportForm.isValid()) {

            Command search = Services.createCommand(SearchNodeCommand.class);

            reportResults = (List<StructrNode>) search.execute(null, user, true, false, searchAttributes);
            populateReportResultsTable();
            saveState();

        }

        Map methodCache = new HashMap<Object, Object>();
        List<String[]> resultList = new ArrayList<String[]>();

        List<String> keys = new ArrayList<String>();
        for (Column c : columns) {
            keys.add(c.getName());
        }

        // add column headings
        resultList.add((keys.toArray(new String[keys.size()])));

        if (reportResults == null) {
            return false;
        }

        for (StructrNode s : reportResults) {

//            List<String> cols = new ArrayList<String>();
            List<String> values = new ArrayList<String>();
            for (Column c : columns) {

                Object value = PropertyUtils.getValue(s, c.getName(), methodCache);
                if (value != null) {
                    values.add(value.toString());
                } else {
                    values.add("");
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

            StructrNode s = null;
            Command transaction = Services.createCommand(TransactionCommand.class);

            s = (StructrNode) transaction.execute(new StructrTransaction() {

                @Override
                public Object execute() throws Throwable {
                    // Save report in database
                    Command createNode = Services.createCommand(CreateNodeCommand.class);
                    Command createRel = Services.createCommand(CreateRelationshipCommand.class);

                    // create node with appropriate type
                    StructrNode newNode = (StructrNode) createNode.execute(new NodeAttribute(StructrNode.TYPE_KEY, File.class.getSimpleName()), user);


                    String relativeFilePath = newNode.getId() + "_" + System.currentTimeMillis();
                    String targetPath = FILES_PATH + "/" + relativeFilePath;

                    String fileUrl = "file:///" + reportFile.getPath();
                    FileUtils.moveFile(reportFile, new File(targetPath));

                    Date now = new Date();
                    newNode.setProperty(StructrNode.NAME_KEY, reportFileName);
                    newNode.setProperty(StructrNode.CREATED_DATE_KEY, now);
                    newNode.setProperty(StructrNode.LAST_MODIFIED_DATE_KEY, now);

                    newNode.setProperty(org.structr.core.entity.File.CONTENT_TYPE_KEY, "text/csv");
                    newNode.setProperty(org.structr.core.entity.File.SIZE_KEY, String.valueOf(reportFile.length()));
                    newNode.setProperty(org.structr.core.entity.File.URL_KEY, fileUrl);
                    newNode.setProperty(org.structr.core.entity.File.RELATIVE_FILE_PATH_KEY, relativeFilePath);

                    // connect report to user node
                    StructrNode parentNode = user;
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

        // Always filter by type
        searchAttributes.add(new SearchAttribute(StructrNode.TYPE_KEY, resultType, SearchOperator.AND));

        // Get the corresponding entity class
        //Class<StructrNode> c = Services.getEntityClass(resultType);
        Class<StructrNode> c = (Class<StructrNode>) Services.createCommand(GetEntityClassCommand.class).execute(resultType);

        if (c != null) {

            // Get all the node property fields
            java.lang.reflect.Field[] fields = c.getFields();
            //reportFields.setColumns(fields.length);

            StructrNode o = new EmptyNode();
            try {

                // Instantiate an object to get the value of the public static fields
                o = (StructrNode) c.newInstance();
            } catch (Throwable t) {
                logger.log(Level.SEVERE, null, t);
            }

            if (fields != null) {
                for (java.lang.reflect.Field f : fields) {
                    String fieldName = null;
                    try {

                        fieldName = (String) f.get(o);

                        // Type is already there
                        if (StructrNode.TYPE_KEY.equals(fieldName)) {
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
                            searchAttributes.add(new SearchAttribute(fieldName, fieldValue, SearchOperator.AND));
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


        Set<String> nodeTypes = ((Map<String, Class>) Services.createCommand(GetEntitiesCommand.class).execute()).keySet();
        for (String className : nodeTypes) {
            Option o = new Option(className);
            resultTypeSelect.add(o);
        }
    }

    private String checkBoxName(final String name) {
        return name + "Checkbox";
    }
}
