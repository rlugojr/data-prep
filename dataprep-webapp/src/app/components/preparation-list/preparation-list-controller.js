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
 * @name data-prep.preparation-list.controller:PreparationListCtrl
 * @description Preparation list controller.
 * On creation, it fetch the user's preparations and load the requested one if a `prepid` is present as query param
 * @requires data-prep.services.state.constant:state
 * @requires data-prep.services.state.service:StateService
 * @requires data-prep.services.preparation.service:PreparationService
 * @requires data-prep.services.utils.service:MessageService
 * @requires talend.widget.service:TalendConfirmService
 */
export default function PreparationListCtrl($timeout, $rootScope, $state, state, StateService,
                                            PreparationService, MessageService, TalendConfirmService) {
    'ngInject';

    var vm = this;
    vm.preparationService = PreparationService;
    vm.state = state;
    vm.displayDatasetPicker = false;
    vm.preparationToUpdate = null;

    /**
     * @ngdoc method
     * @name load
     * @methodOf data-prep.preparation-list.controller:PreparationListCtrl
     * @param {object} preparation - the preparation to load
     * @description Load a preparation in the playground
     */
    vm.load = function load(preparation) {
        StateService.setPreviousState('nav.index.preparations');
        $state.go('playground.preparation', {prepid: preparation.id});
    };

    /**
     * @ngdoc method
     * @name delete
     * @methodOf data-prep.preparation-list.controller:PreparationListCtrl
     * @param {object} preparation - the preparation to delete
     * @description Delete a preparation
     */
    vm.remove = function remove(preparation) {
        TalendConfirmService.confirm({disableEnter: true}, ['DELETE_PERMANENTLY', 'NO_UNDONE_CONFIRM'], {
                type: 'preparation',
                name: preparation.name
            })
            .then(function () {
                return PreparationService.delete(preparation);
            })
            .then(function () {
                MessageService.success('REMOVE_SUCCESS_TITLE', 'REMOVE_SUCCESS', {
                    type: 'preparation',
                    name: preparation.name
                });
            });
    };

    /**
     * @ngdoc method
     * @name rename
     * @methodOf data-prep.preparation-list.controller:PreparationListCtrl
     * @param {object} preparation The preparation to rename
     * @param {string} newName The new name for the given preparation
     * @description Trigger backend call to update preparation name
     */
    vm.rename = function rename(preparation, newName) {
        var cleanName = newName ? newName.trim() : '';
        if (cleanName) {
            $rootScope.$emit('talend.loading.start');
            return PreparationService.setName(preparation.id, newName)
                .then(function () {
                    MessageService.success('PREPARATION_RENAME_SUCCESS_TITLE', 'PREPARATION_RENAME_SUCCESS');
                })
                .finally(function () {
                    $rootScope.$emit('talend.loading.stop');
                });
        }
    };

    /**
     * @ngdoc method
     * @name clone
     * @methodOf data-prep.preparation-list.controller:PreparationListCtrl
     * @param {object} preparation - the preparation to clone
     * @description trigger backend call to clone preparation
     */
    vm.clone = function clone(preparation) {
        $rootScope.$emit('talend.loading.start');
        return PreparationService.clone(preparation.id)
            .then(function () {
                MessageService.success('PREPARATION_COPYING_SUCCESS_TITLE', 'PREPARATION_COPYING_SUCCESS');
            })
            .finally(function () {
                $rootScope.$emit('talend.loading.stop');
            });
    };

    /**
     * @ngdoc method
     * @name getCandidateDatasets
     * @methodOf data-prep.preparation-list.controller:PreparationListCtrl
     * @param {Object} preparation - the preparation to update
     * @description populates the list of the candidates datasets
     */
    vm.showDatasetPicker = function showDatasetPicker(preparation) {
        vm.displayDatasetPicker = true;
        vm.preparationToUpdate = preparation;
    };

    //vm.replaceBaseDataset = function replaceBaseDataset(datasetId, preparationId){
    //    $rootScope.$emit('talend.loading.start');
    //    PreparationService.update(preparationId, {dataSetId: datasetId})
    //        .then(() => {
    //            return PreparationService.refreshPreparations();
    //        })
    //        .then(() => {
    //            $rootScope.$emit('talend.loading.stop');
    //        });
    //}
}