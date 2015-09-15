package org.talend.dataprep.api.dataset.statistics;

import java.util.*;

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

public class StatisticsUtils {

    private StatisticsUtils() {
    }

    public static void setStatistics(List<ColumnMetadata> columns, List<Analyzers.Result> results) {
        final Iterator<ColumnMetadata> columnIterator = columns.iterator();
        for (Analyzers.Result result : results) {
            final ColumnMetadata currentColumn = columnIterator.next();
            final boolean isNumeric = Type.NUMERIC.isAssignableFrom(Type.get(currentColumn.getType()));
            final boolean isString = Type.STRING.isAssignableFrom(Type.get(currentColumn.getType()));
            final Statistics statistics = currentColumn.getStatistics();
            // Value quality (empty / invalid / ...)
            if (result.exist(ValueQualityStatistics.class)) {
                final ValueQualityStatistics valueQualityStatistics = result.get(ValueQualityStatistics.class);
                statistics.setCount(valueQualityStatistics.getCount());
                statistics.setEmpty(valueQualityStatistics.getEmptyCount());
                statistics.setInvalid(valueQualityStatistics.getInvalidCount());
                statistics.setValid(valueQualityStatistics.getValidCount());
            }
            // Cardinality (distinct + duplicates)
            if (result.exist(CardinalityStatistics.class)) {
                final CardinalityStatistics cardinalityStatistics = result.get(CardinalityStatistics.class);
                statistics.setDistinctCount(cardinalityStatistics.getDistinctCount());
                statistics.setDuplicateCount(cardinalityStatistics.getDuplicateCount());
            }
            // Frequencies (data)
            final DataFrequencyStatistics dataFrequencyStatistics = result.get(DataFrequencyStatistics.class);
            if (result.exist(DataFrequencyStatistics.class)) {
                final Map<String, Long> topTerms = dataFrequencyStatistics.getTopK(15);
                if (topTerms != null) {
                    statistics.getDataFrequencies().clear();
                    topTerms.forEach((s, o) -> statistics.getDataFrequencies().add(new DataFrequency(s, o)));
                }
            }
            // Frequencies (pattern)
            if (result.exist(PatternFrequencyStatistics.class)) {
                final PatternFrequencyStatistics patternFrequencyStatistics = result.get(PatternFrequencyStatistics.class);
                final Map<String, Long> topTerms = patternFrequencyStatistics.getTopK(15);
                if (topTerms != null) {
                    statistics.getPatternFrequencies().clear();
                    topTerms.forEach((s, o) -> statistics.getPatternFrequencies().add(new PatternFrequency(s, o)));
                }
            }
            // Quantiles
            if (result.exist(QuantileStatistics.class)) {
                final QuantileStatistics quantileStatistics = result.get(QuantileStatistics.class);
                final Quantiles quantiles = statistics.getQuantiles();
                quantiles.setLowerQuantile(quantileStatistics.getLowerQuantile());
                quantiles.setMedian(quantileStatistics.getMedian());
                quantiles.setUpperQuantile(quantileStatistics.getUpperQuantile());
            }
            // Summary (min, max, mean, variance)
            if (result.exist(SummaryStatistics.class)) {
                final SummaryStatistics summaryStatistics = result.get(SummaryStatistics.class);
                statistics.setMax(summaryStatistics.getMax());
                statistics.setMin(summaryStatistics.getMin());
                statistics.setMean(summaryStatistics.getMean());
                statistics.setVariance(summaryStatistics.getVariance());
            }
            // Histogram
            if (isNumeric && result.exist(HistogramStatistics.class)) {
                final HistogramStatistics histogramStatistics = result.get(HistogramStatistics.class);
                statistics.getHistogram().clear();
                histogramStatistics.getHistogram().forEach((r, v) -> {
                    final HistogramRange range = new HistogramRange();
                    range.getRange().setMax(r.getUpper());
                    range.getRange().setMin(r.getLower());
                    range.setOccurrences(v);
                    statistics.getHistogram().add(range);
                });
            }
            // Text length
            if (isString && result.exist(TextLengthStatistics.class)) {
                final TextLengthStatistics textLengthStatistics = result.get(TextLengthStatistics.class);
                final TextLengthSummary textLengthSummary = statistics.getTextLengthSummary();
                textLengthSummary.setAverageLength(textLengthStatistics.getAvgTextLength());
                textLengthSummary.setMinimalLength(textLengthStatistics.getMinTextLength());
                textLengthSummary.setMaximalLength(textLengthStatistics.getMaxTextLength());
            }
        }
    }

    /**
     * Computes the better set of range to represents a number distribution.
     *
     * It will assure that the number of buckets is less or equal to maxBuckets, but it could be less.
     *
     * The main goal of this method is to render "meaningfull" range for an end-user. ie: min=4_512, max=58_130, max
     * bucket=20, will render 19 buckets, each with a size of 3_000 (3_000->6_000, 6_000->12_000, etc...)
     *
     * @return an ordered set of ranges (order is assured by Range.compareTo())
     *
     * TODO manage negative numbers
     */
    public static TreeSet<Range> computeRange(double min, double max, int maxBuckets) {
        if (max - min <= maxBuckets) {
            return computeRangeFromBucketSize(min, max, 1);
        }

        // This algo is built in a empiric way, and is quite hard to explain, to improve...
        for (int j = 1; j <= 10; j++) {
            for (int i = 1; i <= 5; i++) {
                double bucketSize = Math.pow(10, j) * i;
                if (isBucketSizeValid(min, max, maxBuckets, bucketSize)) {
                    return computeRangeFromBucketSize(min, max, bucketSize);
                }
            }
        }

        // TODO add tests that use this brutal way

        // If none of the previous work, use the brutal algo:
        return computeRangeFromBucketSize(min, max, (max - min) / maxBuckets);
    }

    private static boolean isBucketSizeValid(double min, double max, int maxBuckets, double bucketSize) {
        // ========================================================
        // The following block tests if the bucket size is a multiple of 10, 100, 1000, ... depending on its value.
        // ie: size < 100 should be multiple of 10, size between 100 & 1000 must be multiple of 100, etc...
        // ========================================================
        double sq = Math.pow(bucketSize, 1 / 10);
        if (!isMultiple(bucketSize, Math.pow(10, sq)))
            return false;
        // ========================================================

        return (bucketSize * maxBuckets > max - min);
    }

    /**
     * Checks if a is a multiple of b.
     * 
     * @return true if a/b is an integer
     */
    private static boolean isMultiple(double a, double b) {
        Double realValue = a / b;
        int intValue = realValue.intValue();
        return realValue == intValue;
    }

    /**
     * Computes all the range objects to represent a number distribution, regarding min, max and the bucket size. No
     * smartness here, it's just an utility method.
     */
    protected static TreeSet<Range> computeRangeFromBucketSize(double min, double max, double bucketSize) {
        TreeSet<Range> toReturn = new TreeSet<>();

        double from = computeBottom(bucketSize, min);

        for (double first = from; first < max; first += bucketSize) {
            toReturn.add(new Range(first, first + bucketSize));
        }
        return toReturn;
    }

    /**
     * Computes what should be the lower range of the first bucket.
     * 
     * Basically, is just about determine if we should start at 0 or to another value.
     * 
     * @param bucketSize size of the bucket
     * @param min minimal value in all distribution
     * @return a double saying which is the lower range of the first bucket
     */
    protected static double computeBottom(double bucketSize, double min) {
        return (min <= bucketSize ? 0 : Math.floor(min / bucketSize) * bucketSize);
    }

}
