/*  ============================================================================

 Copyright (C) 2006-2016 Talend Inc. - www.talend.com

 This source code is available under agreement available at
 https://github.com/Talend/data-prep/blob/master/LICENSE

 You should have received a copy of the agreement
 along with this program; if not, write to Talend SA
 9 rue Pages 92150 Suresnes, France

 ============================================================================*/

/* eslint-disable angular/window-service */

import angular from 'angular';
import ngSanitize from 'angular-sanitize';
import ngTranslate from 'angular-translate';
import uiRouter from 'angular-ui-router';

import APP_MODULE from './components/app/app-module';
import HOME_MODULE from './components/home/home-module';
import PLAYGROUND_MODULE from './components/playground/playground-module';
import SERVICES_DATASET_MODULE from './services/dataset/dataset-module';
import SERVICES_REST_MODULE from './services/rest/rest-module';
import SERVICES_UTILS_MODULE from './services/utils/utils-module';
import SETTINGS_MODULE from './settings/settings-module';

import routeConfig from './index-route';
import getAppConfiguration from './index-config';

const MODULE_NAME = 'data-prep';

let ws;
let wsPing;
const app = angular.module(MODULE_NAME,
	[
		ngSanitize,
		ngTranslate,
		uiRouter,
		SERVICES_REST_MODULE, // configuration: rest interceptors
		SERVICES_DATASET_MODULE, // configuration: refresh supported encodings
		SERVICES_UTILS_MODULE, // configuration: register constants (version, ...)
		SETTINGS_MODULE, // configuration: get app settings
		HOME_MODULE, // routing: home components
		PLAYGROUND_MODULE, // routing: playground component
		APP_MODULE, // bootstrap: app root
	])
// Performance config
	.config(($httpProvider) => {
		'ngInject';
		$httpProvider.useApplyAsync(true);
	})
	// Translate config
	.config(($translateProvider) => {
		'ngInject';
		$translateProvider.useStaticFilesLoader({
			prefix: 'i18n/',
			suffix: '.json',
		});

		$translateProvider.preferredLanguage('en');
		$translateProvider.useSanitizeValueStrategy(null);
	})

	// Router config
	.config(routeConfig)

	// Language to use at startup (for now only english)
	.run(($window, $translate) => {
		'ngInject';
		$translate.use('en');
	});

window.fetchConfiguration = function fetchConfiguration() {
	return getAppConfiguration()
		.then(({ config, appSettings }) => {
			app
			// Debug config
				.config(($compileProvider) => {
					'ngInject';
					$compileProvider.debugInfoEnabled(config.enableDebug);
				})
				// Configure server api urls
				.run((RestURLs) => {
					'ngInject';
					RestURLs.setConfig(config);
				})
				// Fetch dynamic configuration (supported encodings, ...)
				.run((SettingsService, DatasetService) => {
					'ngInject';
					// base settings
					SettingsService.setSettings(appSettings);
					// dataset encodings
					DatasetService.refreshSupportedEncodings();
				})
				// Open a keepalive websocket if requested
				.run(() => {
					if (!config.serverKeepAliveUrl) return;
					function setupWebSocket() {
						clearInterval(wsPing);

						ws = new WebSocket(config.serverKeepAliveUrl);
						ws.onclose = () => {
							setTimeout(setupWebSocket, 1000);
						};

						wsPing = setInterval(() => {
							ws.send('ping');
						}, 3 * 60 * 1000);
					}

					setupWebSocket();
				});

			angular.module(SERVICES_UTILS_MODULE)
				.value('version', config.version)
				.value('copyRights', config.copyRights)
				.value('documentationSearchURL', config.documentationSearchURL);
		});
};

window.bootstrapDataPrepApplication = function bootstrapDataPrepApplication(modules) {
	angular.element(document)
		.ready(() => angular.bootstrap(document, modules));
};
/* eslint-enable angular/window-service */

export default MODULE_NAME;
