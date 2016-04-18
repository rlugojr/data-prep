//  ============================================================================
//
//  Copyright (C) 2006-2016 Talend Inc. - www.talend.com
//
//  This source code is available under agreement available at
//  https://github.com/Talend/data-prep/blob/master/LICENSE
//
//  You should have received a copy of the agreement
//  along with this program; if not, write to Talend SA
//  9 rue Pages 92150 Suresnes, France
//
//  ============================================================================

package org.talend.dataprep.transformation.api.action.metadata.column;

import java.util.*;

import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;
import org.talend.dataprep.api.dataset.ColumnMetadata;
import org.talend.dataprep.api.dataset.DataSetRow;
import org.talend.dataprep.api.dataset.Quality;
import org.talend.dataprep.api.dataset.RowMetadata;
import org.talend.dataprep.transformation.api.action.context.ActionContext;
import org.talend.dataprep.transformation.api.action.metadata.category.ActionCategory;
import org.talend.dataprep.transformation.api.action.metadata.common.ActionMetadata;
import org.talend.dataprep.transformation.api.action.metadata.common.ColumnAction;
import org.talend.dataprep.transformation.api.action.metadata.common.ImplicitParameters;
import org.talend.dataprep.transformation.api.action.metadata.common.OtherColumnParameters;
import org.talend.dataprep.transformation.api.action.parameters.Parameter;
import org.talend.dataprep.transformation.api.action.parameters.ParameterType;

/**
 * This action reorder columns. The column will be move to the selected column.
 * All other columns will be moved as well.
 */
@Component( Reorder.ACTION_BEAN_PREFIX + Reorder.REORDER_ACTION_NAME )
public class Reorder extends ActionMetadata implements ColumnAction {

    /**
     * The action name.
     */
    public static final String REORDER_ACTION_NAME = "reorder"; //$NON-NLS-1$

    /**
     * @see ActionMetadata#getName()
     */
    @Override
    public String getName() {
        return REORDER_ACTION_NAME;
    }

    /**
     * @see ActionMetadata#getCategory()
     */
    @Override
    public String getCategory() {
        return ActionCategory.COLUMNS.getDisplayName();
    }

    /**
     * @see ActionMetadata#getParameters()
     */
    @Override
    public List<Parameter> getParameters() {
        List<Parameter> parameters = super.getParameters();
        parameters.add(new Parameter( OtherColumnParameters.SELECTED_COLUMN_PARAMETER, ParameterType.COLUMN, StringUtils.EMPTY, false, false));
        return parameters;
    }

    /**
     * @see ActionMetadata#acceptColumn(ColumnMetadata)
     */
    @Override
    public boolean acceptColumn(ColumnMetadata column) {
        // accept all types of columns
        return true;
    }

    @Override
    public void compile( ActionContext actionContext )
    {
        super.compile( actionContext );

        Map<String,String> parameters = actionContext.getParameters();

        RowMetadata rowMetadata = actionContext.getRowMetadata();

        String targetColumnId = parameters.get(OtherColumnParameters.SELECTED_COLUMN_PARAMETER);

        ColumnMetadata selectedColumn = rowMetadata.getById(targetColumnId);

        if (selectedColumn == null){
            return;
        }

        String originColumnId = parameters.get( ImplicitParameters.COLUMN_ID.getKey() );

        // get the origin column
        ColumnMetadata originColumn = rowMetadata.getById( originColumnId );


        // column id may be different from index in the list
        // we cannot rely on id as index
        // so we have to find first the origin and target index
        int index = 0, originIndex = 0, targetIndex = 0;
        for (ColumnMetadata columnMetadata : rowMetadata.getColumns()){
            if (StringUtils.equals( columnMetadata.getId(), originColumnId)){
                originIndex = index;
            }
            if (StringUtils.equals( columnMetadata.getId(), targetColumnId )){
                targetIndex = index;
            }
            index++;
        }

        // now we have both index so we can iterate again and swap few columns
        // we have different case as target can he lower than origin or the opposite
        boolean forwardMove = targetIndex > originIndex;
        /*
        if (!forwardMove) {
            int tmp = targetIndex;
            targetIndex = originIndex;
            originIndex = tmp;
            forwardMove = true;
        }*/

        if (forwardMove) {
            index = 0;
            for ( int size = rowMetadata.getColumns().size(); index < size; index++) {
                if (index >= originIndex && index < targetIndex) {
                    swapColumnMetadata( rowMetadata.getColumns().get( index ), rowMetadata.getColumns().get( index + 1 ) );
                }

            }
        } else {
            index = rowMetadata.getColumns().size() - 1;
            for (; index>0; index--){
                if (index > targetIndex && index <= originIndex) {
                    swapColumnMetadata( rowMetadata.getColumns().get( index ), rowMetadata.getColumns().get( index - 1 ) );
                }
            }
        }


    }
    
    
    protected void swapColumnMetadata( ColumnMetadata originColumn, ColumnMetadata targetColumn ) {


        ColumnMetadata targetColumnCopy = ColumnMetadata.Builder.column().copy( targetColumn ).build();

        targetColumn.setId( originColumn.getId() );
        originColumn.setId( targetColumnCopy.getId() );

        targetColumn.setName( originColumn.getName());
        originColumn.setName( targetColumnCopy.getName() );

        Quality originalQuality = originColumn.getQuality();
        Quality targetQualityCopty = targetColumnCopy.getQuality();

        targetColumn.getQuality().setEmpty( originalQuality.getEmpty() );
        originalQuality.setEmpty( targetQualityCopty.getEmpty() );

        targetColumn.getQuality().setInvalid( originalQuality.getInvalid() );
        originalQuality.setInvalid( targetQualityCopty.getInvalid() );

        targetColumn.getQuality().setValid( originalQuality.getValid() );
        originalQuality.setValid( targetQualityCopty.getValid() );

        targetColumn.getQuality().setInvalidValues( originalQuality.getInvalidValues() );
        originalQuality.setInvalidValues( targetQualityCopty.getInvalidValues() );

        targetColumn.setHeaderSize( originColumn.getHeaderSize() );
        originColumn.setHeaderSize( targetColumnCopy.getHeaderSize() );

        targetColumn.setType(originColumn.getType());
        originColumn.setType( targetColumnCopy.getType() );

        targetColumn.setDiffFlagValue( originColumn.getDiffFlagValue() );
        originColumn.setDiffFlagValue( targetColumnCopy.getDiffFlagValue() );

        targetColumn.setStatistics( originColumn.getStatistics() );
        originColumn.setStatistics( targetColumnCopy.getStatistics() );

        targetColumn.setDomain( originColumn.getDomain() );
        originColumn.setDomain( targetColumnCopy.getDomain() );

        targetColumn.setDomainLabel( originColumn.getDomainLabel() );
        originColumn.setDomainLabel( targetColumnCopy.getDomainLabel() );

        targetColumn.setDomainFrequency( originColumn.getDomainFrequency());
        originColumn.setDomainFrequency( targetColumnCopy.getDomainFrequency() );

        targetColumn.setSemanticDomains( originColumn.getSemanticDomains() );
        originColumn.setSemanticDomains( targetColumnCopy.getSemanticDomains() );

        targetColumn.setDomainForced( originColumn.isDomainForced() );
        originColumn.setDomainForced( targetColumnCopy.isDomainForced() );

        targetColumn.setTypeForced( originColumn.isTypeForced() );
        originColumn.setTypeForced( targetColumnCopy.isTypeForced() );
        
    } 
        

    /**
     * @see ColumnAction#applyOnColumn(DataSetRow, ActionContext)
     */
    @Override
    public void applyOnColumn(DataSetRow row, ActionContext context) {
        /*
        RowMetadata rowMetadata = context.getRowMetadata();
        Map<String, String> parameters = context.getParameters();

        ColumnMetadata selectedColumn = rowMetadata.getById(parameters.get(OtherColumnParameters.SELECTED_COLUMN_PARAMETER));

        if (selectedColumn == null){
            return;
        }

        final String columnId = context.getColumnId();

        // row values still have references to previous id so we need to mimic "swap" :-) to ensure the values are
        // filled with the values from the correct column
        String columnValue = row.get( columnId );
        String selectedColumnValue = row.get( selectedColumn.getId() );

        row.set( columnId, selectedColumnValue == null ? StringUtils.EMPTY : selectedColumnValue );
        row.set( selectedColumn.getId(), columnValue == null ? StringUtils.EMPTY : columnValue );
        */
    }

    @Override
    public Set<Behavior> getBehavior() {
        return Collections.emptySet();
    }

}
