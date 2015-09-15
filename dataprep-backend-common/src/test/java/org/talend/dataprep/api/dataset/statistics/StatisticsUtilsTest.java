package org.talend.dataprep.api.dataset.statistics;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.TreeSet;

import org.junit.Before;
import org.junit.Test;
import org.talend.dataprep.api.dataset.ColumnMetadata;
import org.talend.dataprep.api.type.Type;
import org.talend.dataquality.statistics.cardinality.CardinalityStatistics;
import org.talend.dataquality.statistics.frequency.DataFrequencyStatistics;
import org.talend.dataquality.statistics.frequency.PatternFrequencyStatistics;
import org.talend.dataquality.statistics.numeric.histogram.HistogramStatistics;
import org.talend.dataquality.statistics.numeric.quantile.QuantileStatistics;
import org.talend.dataquality.statistics.numeric.summary.SummaryStatistics;
import org.talend.dataquality.statistics.quality.ValueQualityStatistics;
import org.talend.dataquality.statistics.text.TextLengthStatistics;
import org.talend.datascience.common.inference.Analyzers;

public class StatisticsUtilsTest {

    private ColumnMetadata stringColumn;

    private ColumnMetadata integerColumn;

    @Before
    public void setUp() throws Exception {
        Analyzers.Result result = new Analyzers.Result();
        // Value quality
        ValueQualityStatistics valueQualityStatistics = new ValueQualityStatistics();
        valueQualityStatistics.setEmptyCount(10);
        valueQualityStatistics.setInvalidCount(20);
        valueQualityStatistics.setValidCount(30);
        result.add(valueQualityStatistics);
        // Cardinality
        CardinalityStatistics cardinalityStatistics = new CardinalityStatistics();
        cardinalityStatistics.incrementCount();
        cardinalityStatistics.add("distinctValue");
        result.add(cardinalityStatistics);
        // Data frequency
        DataFrequencyStatistics dataFrequencyStatistics = new DataFrequencyStatistics();
        dataFrequencyStatistics.add("frequentValue1");
        dataFrequencyStatistics.add("frequentValue1");
        dataFrequencyStatistics.add("frequentValue2");
        dataFrequencyStatistics.add("frequentValue2");
        result.add(dataFrequencyStatistics);
        // Pattern frequency
        PatternFrequencyStatistics patternFrequencyStatistics = new PatternFrequencyStatistics();
        patternFrequencyStatistics.add("999a999");
        patternFrequencyStatistics.add("999a999");
        patternFrequencyStatistics.add("999aaaa");
        patternFrequencyStatistics.add("999aaaa");
        result.add(patternFrequencyStatistics);
        // Quantiles
        QuantileStatistics quantileStatistics = new QuantileStatistics();
        quantileStatistics.add(1d);
        quantileStatistics.add(2d);
        quantileStatistics.endAddValue();
        result.add(quantileStatistics);
        // Summary
        SummaryStatistics summaryStatistics = new SummaryStatistics();
        summaryStatistics.addData(1d);
        summaryStatistics.addData(2d);
        result.add(summaryStatistics);
        // Histogram
        HistogramStatistics histogramStatistics = new HistogramStatistics();
        histogramStatistics.setParameters(2, 1, 2);
        histogramStatistics.add(1);
        histogramStatistics.add(2);
        result.add(histogramStatistics);
        // Text length
        TextLengthStatistics textLengthStatistics = new TextLengthStatistics();
        textLengthStatistics.setMaxTextLength(30);
        textLengthStatistics.setMinTextLength(10);
        textLengthStatistics.setSumTextLength(40);
        textLengthStatistics.setCount(5);
        result.add(textLengthStatistics);

        stringColumn = ColumnMetadata.Builder.column().type(Type.STRING).name("col0").build();
        integerColumn = ColumnMetadata.Builder.column().type(Type.INTEGER).name("col1").build();
        StatisticsUtils.setStatistics(Collections.singletonList(integerColumn), Collections.singletonList(result));
        StatisticsUtils.setStatistics(Collections.singletonList(stringColumn), Collections.singletonList(result));

    }

    @Test
    public void testValue() throws Exception {
        assertEquals(10, stringColumn.getStatistics().getEmpty());
        assertEquals(20, stringColumn.getStatistics().getInvalid());
        assertEquals(30, stringColumn.getStatistics().getValid());
    }

    @Test
    public void testCardinality() throws Exception {
        assertEquals(1, stringColumn.getStatistics().getDistinctCount());
        assertEquals(0, stringColumn.getStatistics().getDuplicateCount());
    }

    @Test
    public void testDataFrequency() throws Exception {
        assertEquals("frequentValue2", stringColumn.getStatistics().getDataFrequencies().get(0).data);
        assertEquals(2, stringColumn.getStatistics().getDataFrequencies().get(0).occurrences);
        assertEquals("frequentValue1", stringColumn.getStatistics().getDataFrequencies().get(1).data);
        assertEquals(2, stringColumn.getStatistics().getDataFrequencies().get(1).occurrences);
    }

    @Test
    public void testPatternFrequency() throws Exception {
        assertEquals("999aaaa", stringColumn.getStatistics().getPatternFrequencies().get(0).getPattern());
        assertEquals(2, stringColumn.getStatistics().getPatternFrequencies().get(0).occurrences);
        assertEquals("999a999", stringColumn.getStatistics().getPatternFrequencies().get(1).getPattern());
        assertEquals(2, stringColumn.getStatistics().getPatternFrequencies().get(1).occurrences);
    }

    @Test
    public void testQuantiles() throws Exception {
        assertEquals(1.0, integerColumn.getStatistics().getQuantiles().getLowerQuantile(), 0);
        assertEquals(1.5, integerColumn.getStatistics().getQuantiles().getMedian(), 0);
        assertEquals(2.0, integerColumn.getStatistics().getQuantiles().getUpperQuantile(), 0);
    }

    @Test
    public void testSummary() throws Exception {
        assertEquals(1.0, integerColumn.getStatistics().getMin(), 0);
        assertEquals(1.5, integerColumn.getStatistics().getMean(), 0);
        assertEquals(2.0, integerColumn.getStatistics().getMax(), 0);
    }

    @Test
    public void testHistogram() throws Exception {
        assertEquals(2, integerColumn.getStatistics().getHistogram().size());
        assertEquals(1, integerColumn.getStatistics().getHistogram().get(0).getOccurrences());
        assertEquals(1, integerColumn.getStatistics().getHistogram().get(0).getRange().getMin(), 0);
        assertEquals(1.5, integerColumn.getStatistics().getHistogram().get(0).getRange().getMax(), 0);
        assertEquals(1, integerColumn.getStatistics().getHistogram().get(1).getOccurrences());
        assertEquals(1.5, integerColumn.getStatistics().getHistogram().get(1).getRange().getMin(), 0);
        assertEquals(2, integerColumn.getStatistics().getHistogram().get(1).getRange().getMax(), 0);
    }

    @Test
    public void testTextLengthSummary() throws Exception {
        assertEquals(10, stringColumn.getStatistics().getTextLengthSummary().getMinimalLength(), 0);
        assertEquals(30, stringColumn.getStatistics().getTextLengthSummary().getMaximalLength(), 0);
        assertEquals(8, stringColumn.getStatistics().getTextLengthSummary().getAverageLength(), 0);
    }

    @Test
    public void testComputeRange1() {
        final int maxBuckets = 20;
        TreeSet<Range> result = StatisticsUtils.computeRange(0, 15, maxBuckets);
        assertEquals(15, result.size());
        assertEquals(0, result.first().getMin(), 0);
        assertEquals(1, result.first().getMax(), 0);
        assertEquals(14, result.last().getMin(), 0);
        assertEquals(15, result.last().getMax(), 0);
    }

    @Test
    public void testComputeRange1_bis() {
        final int maxBuckets = 20;
        TreeSet<Range> result = StatisticsUtils.computeRange(12, 350, maxBuckets);
        System.out.println(result);
        assertEquals(18, result.size());
        assertEquals(0, result.first().getMin(), 0);
        assertEquals(20, result.first().getMax(), 0);
        assertEquals(340, result.last().getMin(), 0);
        assertEquals(360, result.last().getMax(), 0);
    }

    @Test
    public void testComputeRange1_ter() {
        final int maxBuckets = 20;
        TreeSet<Range> result = StatisticsUtils.computeRange(16.7, 217.1, maxBuckets);
        System.out.println(result);
        assertEquals(11, result.size());
        assertEquals(0, result.first().getMin(), 0);
        assertEquals(20, result.first().getMax(), 0);
        assertEquals(200, result.last().getMin(), 0);
        assertEquals(220, result.last().getMax(), 0);
    }

    @Test
    public void testComputeRange2() {
        final int maxBuckets = 20;
        TreeSet<Range> result = StatisticsUtils.computeRange(4_512, 58_130, maxBuckets);
        System.out.println(result);
        assertEquals(19, result.size());
        assertEquals(3_000, result.first().getMin(), 0);
        assertEquals(6_000, result.first().getMax(), 0);
        assertEquals(57_000, result.last().getMin(), 0);
        assertEquals(60_000, result.last().getMax(), 0);
    }

    @Test
    public void testComputeRange3() {
        final int maxBuckets = 20;
        TreeSet<Range> result = StatisticsUtils.computeRange(10_000, 22_000, maxBuckets);
        System.out.println(result);
        assertEquals(12, result.size());
        assertEquals(10_000, result.first().getMin(), 0);
        assertEquals(11_000, result.first().getMax(), 0);
        assertEquals(21_000, result.last().getMin(), 0);
        assertEquals(22_000, result.last().getMax(), 0);
    }

    @Test
    public void testComputeRange4() {
        final int maxBuckets = 20;
        TreeSet<Range> result = StatisticsUtils.computeRange(9_800, 220_015, maxBuckets);
        System.out.println(result);
        assertEquals(12, result.size());
        assertEquals(0, result.first().getMin(), 0);
        assertEquals(20_000, result.first().getMax(), 0);
        assertEquals(220_000, result.last().getMin(), 0);
        assertEquals(240_000, result.last().getMax(), 0);
    }


    @Test
    public void testComputeRange5() {
        final int maxBuckets = 20;
        TreeSet<Range> result = StatisticsUtils.computeRange(9_800, 2_015_221, maxBuckets);
        System.out.println(result);
        assertEquals(11, result.size());
        assertEquals(0, result.first().getMin(), 0);
        assertEquals(200_000, result.first().getMax(), 0);
        assertEquals(2_000_000, result.last().getMin(), 0);
        assertEquals(2_200_000, result.last().getMax(), 0);
    }

    @Test
    public void testComputeRange6() {
        final int maxBuckets = 20;
        TreeSet<Range> result = StatisticsUtils.computeRange(159_800, 2_015_221, maxBuckets);
        System.out.println(result);
        assertEquals(20, result.size());
        assertEquals(100_000, result.first().getMin(), 0);
        assertEquals(200_000, result.first().getMax(), 0);
        assertEquals(2_000_000, result.last().getMin(), 0);
        assertEquals(2_100_000, result.last().getMax(), 0);
    }

    @Test
    public void testComputeRange7() {
        final int maxBuckets = 20;
        TreeSet<Range> result = StatisticsUtils.computeRange(12, 80, maxBuckets);
        System.out.println(result);
        assertEquals(7, result.size());
        assertEquals(10, result.first().getMin(), 0);
        assertEquals(20, result.first().getMax(), 0);
        assertEquals(70, result.last().getMin(), 0);
        assertEquals(80, result.last().getMax(), 0);
    }

    @Test
    public void testComputeRange7_bis() {
        final int maxBuckets = 20;
        TreeSet<Range> result = StatisticsUtils.computeRange(12, 82, maxBuckets);
        System.out.println(result);
        assertEquals(8, result.size());
        assertEquals(10, result.first().getMin(), 0);
        assertEquals(20, result.first().getMax(), 0);
        assertEquals(80, result.last().getMin(), 0);
        assertEquals(90, result.last().getMax(), 0);
    }

    @Test
    public void testComputeRangeFromBucketSize1() {
        final int bucketSize = 1;
        TreeSet<Range> result = StatisticsUtils.computeRangeFromBucketSize(0, 15, bucketSize);
        assertEquals(15, result.size());
        assertEquals(0, result.first().getMin(), 0);
        assertEquals(1, result.first().getMax(), 0);
        assertEquals(14, result.last().getMin(), 0);
        assertEquals(15, result.last().getMax(), 0);
    }

    @Test
    public void testComputeRangeFromBucketSize2() {
        final int bucketSize = 12;
        TreeSet<Range> result = StatisticsUtils.computeRangeFromBucketSize(0, 100, bucketSize);
        assertEquals(9, result.size());
        assertEquals(0, result.first().getMin(), 0);
        assertEquals(12, result.first().getMax(), 0);
        assertEquals(96, result.last().getMin(), 0);
        assertEquals(108, result.last().getMax(), 0);
    }

    @Test
    public void testComputeBottom() {
        assertEquals(0, StatisticsUtils.computeBottom(5_000, 4_512), 0);
        assertEquals(10_000, StatisticsUtils.computeBottom(5_000, 14_512), 0);
        assertEquals(10_000, StatisticsUtils.computeBottom(1_000, 10_000), 0);
    }

}