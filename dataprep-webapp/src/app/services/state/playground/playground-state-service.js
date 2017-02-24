/*  ============================================================================

 Copyright (C) 2006-2016 Talend Inc. - www.talend.com

 This source code is available under agreement available at
 https://github.com/Talend/data-prep/blob/master/LICENSE

 You should have received a copy of the agreement
 along with this program; if not, write to Talend SA
 9 rue Pages 92150 Suresnes, France

 ============================================================================*/

export const playgroundState = {
	candidatePreparations: [],
	isLoading: false,
	isSavingPreparation: false,
	preparation: null,
	preparationName: '',
	sampleType: 'HEAD',
	isReadOnly: false,
};

export function PlaygroundStateService(RecipeStateService, recipeState,
                                       GridStateService, gridState,
                                       FilterStateService, filterState,
                                       SuggestionsStateService, suggestionsState,
                                       LookupStateService, lookupState,
                                       StatisticsStateService, statisticsState,
                                       ParametersStateService, parametersState) {
	'ngInject';

	playgroundState.recipe = recipeState;
	playgroundState.grid = gridState;
	playgroundState.lookup = lookupState;
	playgroundState.filter = filterState;
	playgroundState.suggestions = suggestionsState;
	playgroundState.statistics = statisticsState;
	playgroundState.parameters = parametersState;

	return {
        // playground
		reset,
		setDataset,
		setIsFetchingStats,
		setIsLoading,
		setIsSavingPreparation,
		setPreparation,
		setPreparationName,
		setNameEditionMode,
		setData,
		updateDatasetRecord,
		updateDatasetStatistics,
		setSampleType,
		setReadOnlyMode,

        // parameters
		toggleDatasetParameters,
		hideDatasetParameters: ParametersStateService.hide,
		setIsSendingDatasetParameters: ParametersStateService.setIsSending,
		setDatasetEncodings: ParametersStateService.setEncodings,

        // recipe
		showRecipe: RecipeStateService.show,
		hideRecipe: RecipeStateService.hide,
		setHoveredStep: RecipeStateService.setHoveredStep,
		setRecipeSteps: RecipeStateService.setSteps,
		setRecipePreviewSteps: RecipeStateService.setPreviewSteps,
		restoreRecipeBeforePreview: RecipeStateService.restoreBeforePreview,
		disableRecipeStepsAfter: RecipeStateService.disableStepsAfter,
		setRecipeAllowDistributedRun: RecipeStateService.setAllowDistributedRun,

        // datagrid
		setColumnFocus: GridStateService.setColumnFocus,
		setGridSelection: GridStateService.setGridSelection,
		toggleColumnSelection: GridStateService.toggleColumnSelection,
		changeRangeSelection: GridStateService.changeRangeSelection,
		setSemanticDomains: GridStateService.setSemanticDomains,
		setPrimitiveTypes: GridStateService.setPrimitiveTypes,

        // lookup
		setLookupActions: LookupStateService.setActions,
		setLookupAddedActions: LookupStateService.setAddedActions,
		setLookupDatasets: LookupStateService.setDatasets,
		setLookupAddMode: LookupStateService.setAddMode,
		setLookupSelectedColumn: LookupStateService.setSelectedColumn,
		setLookupUpdateMode: LookupStateService.setUpdateMode,
		setLookupVisibility,
		updateLookupColumnsToAdd: LookupStateService.updateColumnsToAdd,
		setLookupDatasetsSort: LookupStateService.setSort,
		setLookupDatasetsOrder: LookupStateService.setOrder,

        // filters
		addGridFilter,
		updateGridFilter,
		removeGridFilter,
		removeAllGridFilters,
		enableFilters,
		disableFilters,

        // actions
		selectTransformationsTab: SuggestionsStateService.selectTab,
		setTransformations: SuggestionsStateService.setTransformations,
		setTransformationsLoading: SuggestionsStateService.setLoading,
		updateFilteredTransformations: SuggestionsStateService.updateFilteredTransformations,

        // statistics
		setStatisticsBoxPlot: StatisticsStateService.setBoxPlot,
		setStatisticsDetails: StatisticsStateService.setDetails,
		setStatisticsRangeLimits: StatisticsStateService.setRangeLimits,
		setStatisticsHistogram: StatisticsStateService.setHistogram,
		setStatisticsFilteredHistogram: StatisticsStateService.setFilteredHistogram,
		setStatisticsHistogramActiveLimits: StatisticsStateService.setHistogramActiveLimits,
		setStatisticsPatterns: StatisticsStateService.setPatterns,
		setStatisticsFilteredPatterns: StatisticsStateService.setFilteredPatterns,
	};

    //--------------------------------------------------------------------------------------------------------------
    // --------------------------------------------------PLAYGROUND--------------------------------------------------
    //--------------------------------------------------------------------------------------------------------------
	function setReadOnlyMode(bool) {
		playgroundState.isReadOnly = bool;
	}

	function setSampleType(type) {
		playgroundState.sampleType = type;
	}

	function setDataset(dataset) {
		playgroundState.dataset = dataset;
	}

	function setData(data) {
		playgroundState.data = data;
		GridStateService.setData(data);

		if (filterState.enabled) {
			GridStateService.setFilter(filterState.gridFilters, playgroundState.data);
		}
		else {
			GridStateService.setFilter([], playgroundState.data);
		}
	}

	function setPreparation(preparation) {
		playgroundState.preparation = preparation;
	}

	function setPreparationName(preparationName) {
		playgroundState.preparationName = preparationName;
	}

	function setNameEditionMode(editionMode) {
		playgroundState.nameEditionMode = editionMode;
	}

	function updateDatasetStatistics(metadata) {
		_.forEach(playgroundState.data.metadata.columns, function (col) {
			const correspondingColumn = _.find(metadata.columns, { id: col.id });
			col.statistics = correspondingColumn.statistics;
			col.quality = correspondingColumn.quality;
		});
	}

	function updateDatasetRecord(records) {
		playgroundState.dataset.records = records;
	}

	function setIsFetchingStats(value) {
		playgroundState.isFetchingStats = value;
	}

	function setIsLoading(value) {
		playgroundState.isLoading = value;
	}

	function setIsSavingPreparation(value) {
		playgroundState.isSavingPreparation = value;
	}

	function setLookupVisibility(value) {
		if (value && playgroundState.grid.selectedColumns.length > 1) {
			playgroundState.grid.selectedColumns = [playgroundState.grid.selectedColumns[0]];
		}
		LookupStateService.setVisibility(value);
	}
    //--------------------------------------------------------------------------------------------------------------
    // -------------------------------------------------PARAMETERS---------------------------------------------------
    //--------------------------------------------------------------------------------------------------------------
	function toggleDatasetParameters() {
		if (parametersState.visible) {
			ParametersStateService.hide();
		}
		else {
			showDatasetParameters();
		}
	}

	function showDatasetParameters() {
		ParametersStateService.update(playgroundState.dataset);
		ParametersStateService.show();
	}

    //--------------------------------------------------------------------------------------------------------------
    // ---------------------------------------------------FILTERS----------------------------------------------------
    //--------------------------------------------------------------------------------------------------------------
	function addGridFilter(filter) {
		FilterStateService.addGridFilter(filter);
		GridStateService.setFilter(filterState.gridFilters, playgroundState.data);
		FilterStateService.enableFilters();
	}

	function updateGridFilter(oldFilter, newFilter) {
		FilterStateService.updateGridFilter(oldFilter, newFilter);
		GridStateService.setFilter(filterState.gridFilters, playgroundState.data);
		FilterStateService.enableFilters();
	}

	function removeGridFilter(filter) {
		FilterStateService.removeGridFilter(filter);
		GridStateService.setFilter(filterState.gridFilters, playgroundState.data);
	}

	function removeAllGridFilters() {
		FilterStateService.removeAllGridFilters();
		GridStateService.setFilter(filterState.gridFilters, playgroundState.data);
	}

	function enableFilters() {
		FilterStateService.enableFilters();
		GridStateService.setFilter(filterState.gridFilters, playgroundState.data);
	}

	function disableFilters() {
		FilterStateService.disableFilters();
		GridStateService.setFilter([], playgroundState.data);
	}

	function reset() {
		playgroundState.data = null;
		playgroundState.dataset = null;
		playgroundState.isFetchingStats = false;
		playgroundState.isLoading = false;
		playgroundState.isSavingPreparation = false;
		playgroundState.nameEditionMode = false;
		playgroundState.lookupData = null;
		playgroundState.preparation = null;
		playgroundState.sampleType = 'HEAD';
		playgroundState.isReadOnly = false;

		RecipeStateService.reset();
		FilterStateService.reset();
		GridStateService.reset();
		LookupStateService.reset();
		SuggestionsStateService.reset();
		StatisticsStateService.reset();
		ParametersStateService.reset();
	}
}
