/*  ============================================================================

 Copyright (C) 2006-2016 Talend Inc. - www.talend.com

 This source code is available under agreement available at
 https://github.com/Talend/data-prep/blob/master/LICENSE

 You should have received a copy of the agreement
 along with this program; if not, write to Talend SA
 9 rue Pages 92150 Suresnes, France

 ============================================================================*/

import { PLAYGROUND_PREPARATION_ROUTE } from '../../index-route';
/**
 * @ngdoc controller
 * @name data-prep.playground.controller:PlaygroundCtrl
 * @description Playground controller.
 * @requires data-prep.services.state.constant:state
 * @requires data-prep.services.state.service:StateService
 * @requires data-prep.services.playground.service:PlaygroundService
 * @requires data-prep.services.preparation.service:PreparationService
 * @requires data-prep.services.playground.service:PreviewService
 * @requires data-prep.services.onboarding.service:OnboardingService
 * @requires data-prep.services.lookup.service:LookupService
 * @requires data-prep.services.utils.service:MessageService
 */
export default function PlaygroundCtrl($state, $stateParams, state, StateService,
                                       PlaygroundService, DatasetService, PreparationService,
                                       PreviewService, FilterManagerService,
                                       OnboardingService, LookupService) {
	'ngInject';

	const vm = this;
	vm.$stateParams = $stateParams;
	vm.state = state;

	vm.openFeedbackForm = () => StateService.showFeedback();
	vm.toggleParameters = () => StateService.toggleDatasetParameters();
	vm.previewInProgress = () => PreviewService.previewInProgress();
	vm.startOnBoarding = tourId => OnboardingService.startTour(tourId);
	vm.fetchCompatiblePreparations = datasetId => DatasetService.getCompatiblePreparations(datasetId);
	vm.removeAllFilters = () => FilterManagerService.removeAllFilters();

	/**
	 * @ngdoc property
	 * @name showNameValidation
	 * @propertyOf data-prep.playground.controller:PlaygroundCtrl
	 * @description Flag that controls the display of the save/discard window on implicit preparation close.
	 */
	vm.showNameValidation = false;

	/**
	 * @ngdoc property
	 * @name displayPreprationPicker
	 * @propertyOf data-prep.playground.controller:PlaygroundCtrl
	 * @description Flag that controls the display of preparation picker form.
	 */
	vm.displayPreprationPicker = false;

	//--------------------------------------------------------------------------------------------------------------
	// --------------------------------------------------PREPARATION PICKER------------------------------------------
	//--------------------------------------------------------------------------------------------------------------
	/**
	 * @ngdoc method
	 * @name showPreparationPicker
	 * @methodOf data-prep.playground.controller:PlaygroundCtrl
	 * @description Toggle preparation picker modal
	 */
	vm.showPreparationPicker = () => {
		vm.displayPreparationPicker = true;
	};

	/**
	 * @ngdoc method
	 * @name applySteps
	 * @param {string} preparationId The preparation to apply
	 * @methodOf data-prep.playground.controller:PlaygroundCtrl
	 * @description Apply the preparation steps to the current preparation
	 */
	vm.applySteps = (preparationId) => {
		return PlaygroundService.copySteps(preparationId)
			.then(() => {
				vm.displayPreparationPicker = false;
			});
	};

	//--------------------------------------------------------------------------------------------------------------
	// --------------------------------------------------RECIPE HEADER-----------------------------------------------
	//--------------------------------------------------------------------------------------------------------------
	/**
	 * @ngdoc method
	 * @name confirmPrepNameEdition
	 * @methodOf data-prep.playground.controller:PlaygroundCtrl
	 * @description Change the preparation name
	 */
	vm.confirmPrepNameEdition = (name) => {
		const cleanName = name.trim();
		if (!vm.changeNameInProgress && cleanName) {
			changeName(cleanName)
				.then(preparation => $state.go(PLAYGROUND_PREPARATION_ROUTE, { prepid: preparation.id }));
		}
	};

	/**
	 * @ngdoc method
	 * @name changeName
	 * @methodOf data-prep.playground.controller:PlaygroundCtrl
	 * @description [PRIVATE] Create a preparation or update existing preparation name if it already exists
	 * @param {string} name The preparation name
	 * @returns {Promise} The create/update promise
	 */
	function changeName(name) {
		vm.changeNameInProgress = true;
		return PlaygroundService.createOrUpdatePreparation(name)
			.finally(() => vm.changeNameInProgress = false);
	}

	//--------------------------------------------------------------------------------------------------------------
	// ------------------------------------------------------LOOKUP--------------------------------------------------
	//--------------------------------------------------------------------------------------------------------------
	/**
	 * @ngdoc method
	 * @name toggleLookup
	 * @methodOf data-prep.playground.controller:PlaygroundCtrl
	 * @description show hides lookup panel and populates its grid
	 */
	vm.toggleLookup = () => {
		if (state.playground.lookup.visibility) {
			StateService.setLookupVisibility(false);
		}
		else {
			LookupService.initLookups()
				.then(StateService.setLookupVisibility.bind(null, true));
		}
	};

	//--------------------------------------------------------------------------------------------------------------
	// ------------------------------------------------------CLOSE---------------------------------------------------
	//--------------------------------------------------------------------------------------------------------------
	/**
	 * @ngdoc method
	 * @name beforeClose
	 * @methodOf data-prep.playground.controller:PlaygroundCtrl
	 * @description When the preparation is an implicit preparation, we show the save/discard modal and block the
	 *     playground close.
	 * @returns {boolean} True if the playground can be closed (no implicit preparation), False otherwise
	 */
	vm.beforeClose = () => {
		const isDraft = state.playground.preparation && state.playground.preparation.draft;
		if (isDraft) {
			if (state.playground.recipe.current.steps.length) {
				vm.showNameValidation = true;
			}
			else {
				vm.discardSaveOnClose();
			}
		}
		else {
			PlaygroundService.close();
		}
	};

	/**
	 * @ngdoc method
	 * @name discardSaveOnClose
	 * @methodOf data-prep.playground.controller:PlaygroundCtrl
	 * @description Discard implicit preparation save. This trigger a preparation delete.
	 */
	vm.discardSaveOnClose = () => {
		PreparationService.delete(state.playground.preparation).then(PlaygroundService.close);
	};

	/**
	 * @ngdoc method
	 * @name confirmSaveOnClose
	 * @methodOf data-prep.playground.controller:PlaygroundCtrl
	 * @description Save implicit preparation with provided name. The playground is then closed.
	 */
	vm.confirmSaveOnClose = () => {
		vm.saveInProgress = true;
		StateService.setIsSavingPreparation(true);
		let operation;

		const prepId = state.playground.preparation.id;
		const destinationId = vm.destinationFolder.id;
		const cleanName = vm.state.playground.preparationName.trim();
		if (destinationId !== state.inventory.homeFolderId) {
			operation = PreparationService.move(prepId, state.inventory.homeFolderId, destinationId, cleanName);
		}
		else {
			operation = PreparationService.setName(prepId, cleanName);
		}

		return operation
			.then(() => {
				PlaygroundService.close();
			})
			.finally(() => {
				StateService.setIsSavingPreparation(false);
			});
	};

	//--------------------------------------------------------------------------------------------------------------
	// ------------------------------------------DATASET PARAMS------------------------------------------------------
	//--------------------------------------------------------------------------------------------------------------
	/**
	 * @ngdoc method
	 * @name changeDatasetParameters
	 * @methodOf data-prep.playground.controller:PlaygroundCtrl
	 * @description Change the dataset parameters
	 * @param {object} parameters The new dataset parameters
	 */
	vm.changeDatasetParameters = (parameters) => {
		StateService.setIsSendingDatasetParameters(true);
		PlaygroundService.changeDatasetParameters(parameters)
			.then(StateService.hideDatasetParameters)
			.finally(StateService.setIsSendingDatasetParameters.bind(null, false));
	};

	//--------------------------------------------------------------------------------------------------------------
	// ------------------------------------------------INIT----------------------------------------------------------
	//--------------------------------------------------------------------------------------------------------------
	if ($stateParams.prepid) {
		PlaygroundService.initPreparation();
	}
	else if ($stateParams.datasetid) {
		PlaygroundService.initDataset();
	}
}
