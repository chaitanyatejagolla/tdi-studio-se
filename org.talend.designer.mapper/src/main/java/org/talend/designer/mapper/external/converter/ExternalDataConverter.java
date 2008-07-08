// ============================================================================
//
// Copyright (C) 2006-2007 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.designer.mapper.external.converter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.talend.core.model.metadata.IMetadataTable;
import org.talend.core.model.process.EConnectionType;
import org.talend.designer.abstractmap.model.table.IDataMapTable;
import org.talend.designer.abstractmap.model.tableentry.ITableEntry;
import org.talend.designer.mapper.external.connection.IOConnection;
import org.talend.designer.mapper.external.data.ExternalMapperData;
import org.talend.designer.mapper.external.data.ExternalMapperTable;
import org.talend.designer.mapper.external.data.ExternalMapperTableEntry;
import org.talend.designer.mapper.external.data.ExternalMapperUiProperties;
import org.talend.designer.mapper.managers.MapperManager;
import org.talend.designer.mapper.model.MapperModel;
import org.talend.designer.mapper.model.table.AbstractDataMapTable;
import org.talend.designer.mapper.model.table.InputTable;
import org.talend.designer.mapper.model.table.OutputTable;
import org.talend.designer.mapper.model.table.VarsTable;
import org.talend.designer.mapper.model.tableentry.AbstractInOutTableEntry;
import org.talend.designer.mapper.model.tableentry.FilterTableEntry;
import org.talend.designer.mapper.model.tableentry.VarTableEntry;
import org.talend.designer.mapper.ui.visualmap.table.InputDataMapTableView;

/**
 * Convert external data to internal data and conversely.
 * 
 * $Id$
 * 
 */
public class ExternalDataConverter {

    private ArrayList<ExternalMapperTable> inputTables;

    private ArrayList<ExternalMapperTable> outputTables;

    private ArrayList<ExternalMapperTable> varsTables;

    private MapperManager mapperManager;

    /**
     * DOC amaumont ExternalDataConverter constructor comment.
     * 
     * @param main
     */
    public ExternalDataConverter(MapperManager mapperManager) {
        super();
        this.mapperManager = mapperManager;
    }

    /**
     * Prepare internal data from connections and external data.
     * 
     * @param inputConnections
     * @param outputConnections
     * @param outputMetadataTables
     * @param externalData
     * @param checkProblems
     */
    public MapperModel prepareModel(List<IOConnection> inputs, List<IOConnection> outputs,
            List<IMetadataTable> outputMetadataTables, ExternalMapperData externalData, boolean checkProblems) {

        if (checkProblems) {
            mapperManager.getProblemsManager().checkProblems();
        }

        ArrayList<InputTable> inputDataMapTables = prepareInputTables(inputs, externalData);

        ArrayList<OutputTable> outputDataMapTables = prepareOutputTables(outputs, outputMetadataTables, externalData);

        List<VarsTable> varsTablesList = prepareVarsTables(externalData);

        return new MapperModel(inputDataMapTables, outputDataMapTables, varsTablesList);

    }

    private List<VarsTable> prepareVarsTables(ExternalMapperData externalData) {
        List<VarsTable> varsTablesList = new ArrayList<VarsTable>();
        if (externalData != null) {
            List<ExternalMapperTable> varsExternalTables = externalData.getVarsTables();
            for (ExternalMapperTable persistentTable : varsExternalTables) {
                VarsTable varsTable = new VarsTable(this.mapperManager, persistentTable.getName());
                varsTable.initFromExternalData(persistentTable);
                varsTablesList.add(varsTable);
            }
        }
        if (varsTablesList.size() == 0) {
            VarsTable varsTable = new VarsTable(this.mapperManager, VarsTable.PREFIX_VARS_TABLE_NAME);
            varsTable.setMinimized(true);
            varsTablesList.add(varsTable);
        }
        return varsTablesList;
    }

    private ArrayList<OutputTable> prepareOutputTables(List<IOConnection> outputConnections,
            List<IMetadataTable> outputMetadataTables, ExternalMapperData externalData) {
        Map<String, ExternalMapperTable> nameToOutpuPersistentTable = new HashMap<String, ExternalMapperTable>();
        if (externalData != null) {
            for (ExternalMapperTable persistentTable : externalData.getOutputTables()) {
                nameToOutpuPersistentTable.put(persistentTable.getName(), persistentTable);
            }
        }
        Map<String, IOConnection> nameMetadataToOutpuConn = new HashMap<String, IOConnection>();
        if (outputConnections != null) {
            for (IOConnection connection : outputConnections) {
                if (connection.getConnectionType().equals(EConnectionType.FLOW_MAIN)
                        || connection.getConnectionType().equals(EConnectionType.FLOW_REF)
                        || connection.getConnectionType().equals(EConnectionType.FLOW_MERGE)) {
                    nameMetadataToOutpuConn.put(connection.getTable().getTableName(), connection);
                }
            }
        }
        ArrayList<OutputTable> outputDataMapTables = new ArrayList<OutputTable>();
        for (IMetadataTable table : outputMetadataTables) {
            IOConnection connection = nameMetadataToOutpuConn.get(table.getTableName());
            OutputTable outputTable = null;
            if (connection != null) {
                ExternalMapperTable persistentTable = nameToOutpuPersistentTable.get(connection.getName());
                outputTable = new OutputTable(this.mapperManager, connection.getTable(), connection.getName());
                outputTable.initFromExternalData(persistentTable);
            } else {
                ExternalMapperTable persistentTable = nameToOutpuPersistentTable.get(table.getTableName());
                outputTable = new OutputTable(this.mapperManager, table, table.getTableName());
                outputTable.initFromExternalData(persistentTable);
            }
            outputDataMapTables.add(outputTable);
        }
        return outputDataMapTables;
    }

    public ArrayList<InputTable> prepareInputTables(List<IOConnection> inputConnections, ExternalMapperData externalData) {
        ArrayList<InputTable> inputDataMapTables = new ArrayList<InputTable>();
        ArrayList<IOConnection> remainingConnections = new ArrayList<IOConnection>(inputConnections);
        // case externalData IS NOT initialized
        if (externalData == null || externalData.getInputTables().size() == 0) {
            for (IOConnection connection : inputConnections) {
                InputTable inputTable = new InputTable(this.mapperManager, connection, connection.getName());
                inputTable.initFromExternalData(null);
                inputDataMapTables.add(inputTable);
            }
        } else {
            // case externalData IS initialized
            Map<String, IOConnection> nameToInputConnection = new HashMap<String, IOConnection>();
            for (IOConnection connection : inputConnections) {
                nameToInputConnection.put(connection.getName(), connection);
            }

            for (ExternalMapperTable persistentTable : externalData.getInputTables()) {
                IOConnection connection = nameToInputConnection.get(persistentTable.getName());
                if (connection != null) {
                    InputTable inputTable = new InputTable(this.mapperManager, connection, connection.getName());
                    inputTable.initFromExternalData(persistentTable);
                    inputDataMapTables.add(inputTable);
                    remainingConnections.remove(connection);
                }
            }
            for (IOConnection connection : remainingConnections) {
                InputTable inputTable = new InputTable(this.mapperManager, connection, connection.getName());
                inputTable.initFromExternalData(null);
                inputDataMapTables.add(inputTable);
            }
        }

        // sort for put table with main connection at top position of the list
        Collections.sort(inputDataMapTables, new Comparator<InputTable>() {

            public int compare(InputTable o1, InputTable o2) {
                if (o1.isMainConnection()) {
                    return -1;
                } else if (o2.isMainConnection()) {
                    return 1;
                }
                return 0;
            }
        });
        return inputDataMapTables;
    }

    /**
     * 
     * Prepare ExternalMapperData object from internal data.
     * 
     * @param mapperModel
     * @return
     */
    public ExternalMapperData prepareExternalData(MapperModel mapperModel, ExternalMapperUiProperties uiProperties) {
        ExternalMapperData externalData = new ExternalMapperData();
        inputTables = new ArrayList<ExternalMapperTable>();
        externalData.setInputTables(inputTables);
        outputTables = new ArrayList<ExternalMapperTable>();
        externalData.setOutputTables(outputTables);
        varsTables = new ArrayList<ExternalMapperTable>();
        externalData.setVarsTables(varsTables);
        loadInExternalData(mapperModel.getInputDataMapTables());
        loadInExternalData(mapperModel.getVarsDataMapTables());
        loadInExternalData(mapperModel.getOutputDataMapTables());
        externalData.setUiProperties(uiProperties);
        return externalData;
    }

    private void loadInExternalData(Collection<? extends AbstractDataMapTable> tables) {
        for (IDataMapTable table : tables) {
            ExternalMapperTable externalMapperTable = new ExternalMapperTable();
            fillExternalTable(table, externalMapperTable);
            ArrayList<ExternalMapperTableEntry> perTableEntries = new ArrayList<ExternalMapperTableEntry>();
            boolean isVarTable = table instanceof VarsTable;
            for (ITableEntry dataMapTableEntry : table.getColumnEntries()) {
                ExternalMapperTableEntry externalMapperTableEntry = new ExternalMapperTableEntry();
                externalMapperTableEntry.setExpression(dataMapTableEntry.getExpression());
                externalMapperTableEntry.setName(dataMapTableEntry.getName());
                if (isVarTable) {
                    externalMapperTableEntry.setType(((VarTableEntry) dataMapTableEntry).getType());
                    externalMapperTableEntry.setNullable(((VarTableEntry) dataMapTableEntry).isNullable());
                } else {
                    externalMapperTableEntry.setType(((AbstractInOutTableEntry) dataMapTableEntry).getMetadataColumn()
                            .getTalendType());
                    externalMapperTableEntry.setNullable(((AbstractInOutTableEntry) dataMapTableEntry).getMetadataColumn()
                            .isNullable());
                }
                perTableEntries.add(externalMapperTableEntry);
            }
            externalMapperTable.setMetadataTableEntries(perTableEntries);

        }
    }

    /**
     * DOC amaumont Comment method "processToExternal".
     * 
     * @param table
     * @param externalMapperTable
     */
    private void fillExternalTable(IDataMapTable table, ExternalMapperTable externalMapperTable) {
        if (table instanceof InputTable) {
            fillExternalTable((InputTable) table, externalMapperTable);
        } else if (table instanceof VarsTable) {
            fillExternalTable((VarsTable) table, externalMapperTable);
        } else if (table instanceof OutputTable) {
            fillExternalTable((OutputTable) table, externalMapperTable);
        }
    }

    /**
     * 
     * DOC amaumont Comment method "fillExternalTable".
     * 
     * @param table
     * @param externalMapperTable
     */
    private void fillExternalTable(InputTable table, ExternalMapperTable externalMapperTable) {
        externalMapperTable.setInnerJoin(table.isInnerJoin());
        externalMapperTable.setPersistent(table.isPersistent());
        externalMapperTable.setActivateExpressionFilter(table.isActivateExpressionFilter());
        String expressionFilter = null;
        if (table.getExpressionFilter() != null && table.getExpressionFilter().getExpression() != null) {
            expressionFilter = table.getExpressionFilter().getExpression();
        }
        externalMapperTable.setExpressionFilter(expressionFilter);
        String matchingMode = null;
        if (table.getMatchingMode() != null) {
            matchingMode = table.getMatchingMode().toString();
        }
        externalMapperTable.setMatchingMode(matchingMode);
        fillExternalTableWithCommonsData(table, externalMapperTable);
        inputTables.add(externalMapperTable);
    }

    /**
     * DOC amaumont Comment method "fillCommonsDataForExternalTable".
     * 
     * @param table
     * @param externalMapperTable
     */
    private void fillExternalTableWithCommonsData(IDataMapTable table, ExternalMapperTable externalMapperTable) {
        externalMapperTable.setName(table.getName());
        externalMapperTable.setMinimized(table.isMinimized());
        externalMapperTable.setSizeState(table.getSizeState().toString());
    }

    /**
     * DOC amaumont Comment method "process".
     * 
     * @param externalMapperTable
     * @param table
     */
    private void fillExternalTable(OutputTable table, ExternalMapperTable externalMapperTable) {
        fillExternalTableWithCommonsData(table, externalMapperTable);
        externalMapperTable.setReject(table.isReject());
        externalMapperTable.setRejectInnerJoin(table.isRejectInnerJoin());
        externalMapperTable.setActivateExpressionFilter(table.isActivateExpressionFilter());
        externalMapperTable.setExpressionFilter(table.getExpressionFilter() != null
                && table.getExpressionFilter().getExpression() != null
                && table.getExpressionFilter().getExpression().equals(InputDataMapTableView.DEFAULT_EXPRESSION_FILTER) ? null
                : table.getExpressionFilter().getExpression());
        if (mapperManager.isAdvancedMap()) {
            externalMapperTable.setConstraintTableEntries(null);
        } else {
            ArrayList<ExternalMapperTableEntry> constraintTableEntries = new ArrayList<ExternalMapperTableEntry>();
            for (FilterTableEntry constraintTableEntry : table.getFilterEntries()) {
                ExternalMapperTableEntry externalMapperTableEntry = new ExternalMapperTableEntry();
                externalMapperTableEntry.setExpression(constraintTableEntry.getExpression());
                externalMapperTableEntry.setName(constraintTableEntry.getName());
                constraintTableEntries.add(externalMapperTableEntry);
            }
            externalMapperTable.setConstraintTableEntries(constraintTableEntries);
        }

        outputTables.add(externalMapperTable);
    }

    /**
     * 
     * DOC amaumont Comment method "fillExternalTable".
     * 
     * @param table
     * @param externalMapperTable
     */
    private void fillExternalTable(VarsTable table, ExternalMapperTable externalMapperTable) {
        fillExternalTableWithCommonsData(table, externalMapperTable);
        varsTables.add(externalMapperTable);
    }

}
