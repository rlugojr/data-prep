/*  ============================================================================

 Copyright (C) 2006-2016 Talend Inc. - www.talend.com

 This source code is available under agreement available at
 https://github.com/Talend/data-prep/blob/master/LICENSE

 You should have received a copy of the agreement
 along with this program; if not, write to Talend SA
 9 rue Pages 92150 Suresnes, France

 ============================================================================*/
class DatasetPickerCtrl {
    constructor($rootScope, state, DatasetService, PreparationService) {
        'ngInject';

        this.datasetService = DatasetService;

        this.state = state;
        this.$rootScope = $rootScope;
        this.preparationService = PreparationService;
        this.candidateDatasets = [];
    }

    /**
     * @ngdoc method
     * @name $onInit
     * @methodOf data-prep.preparation-picker.controller:PreparationPickerCtrl
     * @description initializes preparation picker form
     **/
    $onInit() {
        this.isFetchingDatasets = true;
        this.preparationService.getCandidateDatasets(this.preparation.id)
            .then((candidateDatasets) => {
                this.candidateDatasets = candidateDatasets;
            })
            .finally(() => {
                this.isFetchingDatasets = false;
            });
    }

    /**
     * @ngdoc method
     * @name selectDataset
     * @methodOf data-prep.dataset-picker.controller:DatasetPickerCtrl
     * @param {Object} selectedDataset selected preparation
     * @description selects the preparation to apply
     **/
    replaceBaseDataset(datasetId, preparationId) {
        this.displayPicker = false;
        this.$rootScope.$emit('talend.loading.start');
        this.preparationService.update(preparationId, {dataSetId: datasetId})
            .then(() => {
                return this.preparationService.refreshPreparations();
            })
            .then(() => {
                this.$rootScope.$emit('talend.loading.stop');
            });
    }

}

export default DatasetPickerCtrl
