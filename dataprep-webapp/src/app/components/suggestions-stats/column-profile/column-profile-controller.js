/*  ============================================================================

  Copyright (C) 2006-2016 Talend Inc. - www.talend.com

  This source code is available under agreement available at
  https://github.com/Talend/data-prep/blob/master/LICENSE

  You should have received a copy of the agreement
  along with this program; if not, write to Talend SA
  9 rue Pages 92150 Suresnes, France

  ============================================================================*/

/**
 * @ngdoc controller
 * @name data-prep.actions-suggestions-stats.controller:ColumnProfileCtrl
 * @description Column profile controller.
 * @requires data-prep.services.state.constant:state
 * @requires data-prep.statistics.service:StatisticsService
 * @requires data-prep.statistics.service:StatisticsTooltipService
 * @requires data-prep.services.filter.service:FilterService
 */
export default function ColumnProfileCtrl($translate, $timeout, $filter, state, StatisticsService, StatisticsTooltipService, FilterService) {
    'ngInject';

    var vm = this;
    vm.chartConfig = {};
    vm.state = state;
    vm.statisticsService = StatisticsService;
    vm.statisticsTooltipService = StatisticsTooltipService;

    //------------------------------------------------------------------------------------------------------
    //------------------------------------------------FILTER------------------------------------------------
    //------------------------------------------------------------------------------------------------------
    function addExactFilter(value, keyName) {
        var column = state.playground.grid.selectedColumn;
        return value.length ?
            FilterService.addFilterAndDigest('exact', column.id, column.name, {
                phrase: value,
                caseSensitive: true
            }, null, keyName) :
            FilterService.addFilterAndDigest('empty_records', column.id, column.name, null, null, keyName);
    }

    /**
     * @ngdoc property
     * @name addBarchartFilter
     * @propertyOf data-prep.actions-suggestions-stats.controller:ColumnProfileCtrl
     * @description Add an "exact" case sensitive filter if the value is not empty, an "empty_records" filter otherwise
     * @type {array}
     */
    vm.addBarchartFilter = function addBarchartFilter(item, keyName) {
        return addExactFilter(item.data, keyName);
    };

    /**
     * @ngdoc method
     * @name addRangeFilter
     * @methodOf data-prep.actions-suggestions-stats.controller:ColumnProfileCtrl
     * @description Add an "range" filter
     * @param {object} interval The interval [min, max] to filter
     */
    vm.addRangeFilter = function addRangeFilter(interval, keyName) {
        var selectedColumn = state.playground.grid.selectedColumn;

        const min = interval.min,
            max = interval.max;

        var removeFilterFn = StatisticsService.getRangeFilterRemoveFn();
        FilterService.addFilterAndDigest('inside_range',
            selectedColumn.id,
            selectedColumn.name,
            {
                interval: [min, max],
                label: FilterService.getIntervalLabelFor([min, max], interval.isMaxReached),
                type: selectedColumn.type,
                isMaxReached: interval.isMaxReached
            },
            removeFilterFn,
            keyName);
    };

    vm.changeAggregation = function changeAggregation(column, aggregation) {
        if (aggregation) {
            StatisticsService.processAggregation(column, aggregation);
        }
        else {
            StatisticsService.processClassicChart();
        }
    };
}
