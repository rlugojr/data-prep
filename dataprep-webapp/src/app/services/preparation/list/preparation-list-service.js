/*  ============================================================================

 Copyright (C) 2006-2016 Talend Inc. - www.talend.com

 This source code is available under agreement available at
 https://github.com/Talend/data-prep/blob/master/LICENSE

 You should have received a copy of the agreement
 along with this program; if not, write to Talend SA
 9 rue Pages 92150 Suresnes, France

 ============================================================================*/

/**
 * @ngdoc service
 * @name data-prep.services.preparation.service:PreparationListService
 * @description Preparation list service. This service holds the preparations list and adapt them for the application.<br/>
 * <b style="color: red;">WARNING : do NOT use this service directly.
 * {@link data-prep.services.preparation.service:PreparationService PreparationService} must be the only entry point for preparations</b>
 * @requires data-prep.services.preparation.service:PreparationRestService
 * @requires data-prep.services.state.service:StateService
 */
export default function PreparationListService(PreparationRestService, StateService) {
    'ngInject';

    var preparationsPromise;

    return {
        refreshPreparations: refreshPreparations,
        getPreparationsPromise: getPreparationsPromise,
        hasPreparationsPromise: hasPreparationsPromise,

        create: create,
        clone: clone,
        update: update,
        delete: deletePreparation,
        getCandidateDatasets: getCandidateDatasets
    };

    /**
     * @ngdoc method
     * @name refreshPreparations
     * @methodOf data-prep.services.preparation.service:PreparationListService
     * @description Refresh the preparations list
     * @returns {promise} The process promise
     */
    function refreshPreparations() {
        preparationsPromise = PreparationRestService.getPreparations()
            .then((response) => {
                StateService.setPreparations(response.data);
                return response.data;
            })
            .catch(() => {
                StateService.setPreparations([]);
                return [];
            })
            .finally(() => preparationsPromise = null);
        return preparationsPromise;
    }

    /**
     * @ngdoc method
     * @name getPreparationsPromise
     * @methodOf data-prep.services.preparation.service:PreparationService
     * @description Return preparation promise that resolve current preparation list if not empty, or call GET service
     * @returns {promise} The process promise
     */
    function getPreparationsPromise() {
        return preparationsPromise ? preparationsPromise : refreshPreparations();
    }

    /**
     * @ngdoc method
     * @name hasPreparationsPromise
     * @methodOf data-prep.services.preparation.service:PreparationService
     * @description Check if there is a fetch in progress
     * @returns {promise} Truthy when a fetch is in progress
     */
    function hasPreparationsPromise() {
        return preparationsPromise;
    }

    /**
     * @ngdoc method
     * @name create
     * @methodOf data-prep.services.preparation.service:PreparationListService
     * @param {string} datasetId The dataset id
     * @param {string} name The preparation name
     * @description Create a new preparation
     * @returns {promise} The POST promise
     */
    function create(datasetId, name) {
        var createdPreparationId;
        return PreparationRestService.create(datasetId, name)
            .then((response) => createdPreparationId = response.data)
            .then(refreshPreparations)
            .then((preparations) => _.find(preparations, {id: createdPreparationId}));
    }

    /**
     * @ngdoc method
     * @name create
     * @methodOf data-prep.services.preparation.service:PreparationListService
     * @param {string} preparationId The preparation id
     * @description Clone the preparation
     * @returns {promise} The GET promise
     */
    function clone(preparationId) {
        return PreparationRestService.clone(preparationId)
            .then((preparationCloneId) => {
                refreshPreparations();
                    return preparationCloneId;
            });
    }

    /**
     * @ngdoc method
     * @name update
     * @methodOf data-prep.services.preparation.service:PreparationRestService
     * @param {string} preparationId The preparation id
     * @param {string} name The new preparation name
     * @description Update the current preparation name
     * @returns {promise} The PUT promise
     */
    function update(preparationId, name) {
        var updatedPreparationId;
        return PreparationRestService.update(preparationId, {name: name})
            .then((result) => updatedPreparationId = result)
            .then(refreshPreparations)
            .then((preparations) => _.find(preparations, {id: updatedPreparationId}));
    }

    /**
     * @ngdoc method
     * @name delete
     * @methodOf data-prep.services.preparation.service:PreparationListService
     * @param {object} preparation The preparation to delete
     * @description Delete a preparation from backend and from its internal list
     * @returns {promise} The DELETE promise
     */
    function deletePreparation(preparation) {
        return PreparationRestService.delete(preparation.id)
            .then(() => StateService.removePreparation(preparation));
    }

    /**
     * @ngdoc method
     * @name getCandidateDatasets
     * @methodOf data-prep.services.preparation.service:PreparationListService
     * @param {String} preparationId
     * @description fetches the candidate datasets to replace the current one
     * @returns {promise} The GET promise
     */
    function getCandidateDatasets(preparationId) {
        return PreparationRestService.getCandidateDatasets(preparationId);
    }
}