// ============================================================================
//
// Copyright (C) 2006-2016 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// https://github.com/Talend/data-prep/blob/master/LICENSE
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================

package org.talend.dataprep.transformation.api.action;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.talend.dataprep.api.dataset.DataSet;
import org.talend.dataprep.api.dataset.DataSetMetadata;
import org.talend.dataprep.api.dataset.DataSetRow;
import org.talend.dataprep.api.dataset.RowMetadata;
import org.talend.dataprep.api.dataset.statistics.StatisticsAdapter;
import org.talend.dataprep.api.preparation.Action;
import org.talend.dataprep.transformation.api.action.context.TransformationContext;
import org.talend.dataprep.transformation.pipeline.Pipeline;
import org.talend.dataprep.transformation.pipeline.model.*;

public class ActionTestWorkbench {

    private ActionTestWorkbench() {
    }

    public static void test(RowMetadata rowMetadata, Action... actions) {
        test(new DataSetRow(rowMetadata), actions);
    }

    public static void test(DataSetRow input, Action... actions) {
        test(Collections.singletonList(input), actions);
    }

    public static void test(Collection<DataSetRow> input, Action... actions) {
        TransformationContext context = new TransformationContext();
        final List<Action> allActions = new ArrayList<>();
        Collections.addAll(allActions, actions);

        final DataSet dataSet = new DataSet();
        final RowMetadata rowMetadata = input.iterator().next().getRowMetadata();
        final DataSetMetadata dataSetMetadata = new DataSetMetadata();
        dataSetMetadata.setRowMetadata(rowMetadata);
        dataSet.setMetadata(dataSetMetadata);
        dataSet.setRecords(input.stream());
        Pipeline pipeline = Pipeline.Builder.builder() //
                .withInitialMetadata(rowMetadata) //
                .withActions(allActions) //
                .withContext(context) //
                .withStatisticsAdapter(new StatisticsAdapter()) //
                .withOutput(TestOutputNode::new) //
                .build();
        pipeline.execute(dataSet);
    }

    private static class TestOutputNode implements Node {

        @Override
        public void receive(DataSetRow row, RowMetadata metadata) {
            row.setRowMetadata(metadata);
        }

        @Override
        public Link getLink() {
            return NullLink.INSTANCE;
        }

        @Override
        public void setLink(Link link) {
            // Nothing to do
        }

        @Override
        public void signal(Signal signal) {
            // Nothing to do
        }

        @Override
        public void accept(Visitor visitor) {
            // Nothing to do
        }
    }
}
