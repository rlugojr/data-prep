/*  ============================================================================

 Copyright (C) 2006-2016 Talend Inc. - www.talend.com

 This source code is available under agreement available at
 https://github.com/Talend/data-prep/blob/master/LICENSE

 You should have received a copy of the agreement
 along with this program; if not, write to Talend SA
 9 rue Pages 92150 Suresnes, France

 ============================================================================*/

import DataPrepAppModule from './app-module';

describe('App directive', () => {
	'use strict';

	let scope;
	let createElement;
	let element;

	beforeEach(window.module(DataPrepAppModule));

	beforeEach(inject(($rootScope, $compile) => {
		scope = $rootScope.$new(true);
		createElement = () => {
			element = angular.element('<dataprep-app></dataprep-app>');
			$compile(element)(scope);
			scope.$digest();
			return element;
		};
	}));

	beforeEach(inject(($injector, $q, RestURLs, UpgradeVersionService) => {
		RestURLs.setConfig({ serverUrl: '' });

		const $httpBackend = $injector.get('$httpBackend');
		$httpBackend
			.expectGET(RestURLs.exportUrl + '/formats')
			.respond(200, {});

		spyOn(UpgradeVersionService, 'retrieveNewVersions').and.returnValue($q.when([]));
	}));

	afterEach(() => {
		scope.$destroy();
		element.remove();
	});

	it('should hold toaster container', () => {
		//when
		createElement();

		//then
		expect(element.find('#toast-container').length).toBe(1);
	});

	it('should hold loader element', () => {
		//when
		createElement();

		//then
		expect(element.find('talend-loading').length).toBe(1);
	});

	it('should hold svg icons', () => {
		//when
		createElement();

		//then
		expect(element.find('icons-provider').length).toBe(1);
	});

	it('should render router insertion point', () => {
		//when
		createElement();

		//then
		expect(element.find('ui-view.main-layout').length).toBe(1);
	});
});
