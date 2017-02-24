/*  ============================================================================

  Copyright (C) 2006-2016 Talend Inc. - www.talend.com

  This source code is available under agreement available at
  https://github.com/Talend/data-prep/blob/master/LICENSE

  You should have received a copy of the agreement
  along with this program; if not, write to Talend SA
  9 rue Pages 92150 Suresnes, France

  ============================================================================*/

describe('Suggestions stats directive', () => {
    'use strict';

    let scope;
    let createElement;
    let element;
    let stateMock;

    beforeEach(angular.mock.module('data-prep.suggestions-stats', ($provide) => {
        stateMock = {
            playground: {
                grid: {},
            }
        };
        $provide.constant('state', stateMock);
    }));

    beforeEach(inject(($rootScope, $compile, $timeout) => {
        scope = $rootScope.$new();
        createElement = () => {
            element = angular.element(
                '<suggestions-stats>' +
                '   <sc-splitter class="suggestions-stats-content" orientation="vertical">' +
                '       <sc-split-first-pane id="help-suggestions">' +
                '           <actions-suggestions class="suggestions-part"></actions-suggestions>' +
                '               </sc-split-first-pane>' +
                '       <sc-split-second-pane id="help-stats">' +
                '           <stats-details class="stats-part"></stats-details>' +
                '       </sc-split-second-pane>' +
                '   </sc-splitter>' +
                '</suggestions-stats>');
            $compile(element)(scope);
            scope.$digest();
            $timeout.flush();
        };
    }));

    afterEach(() => {
        scope.$destroy();
        element.remove();
    });

    it('should set column name in title', () => {
        //given
        stateMock.playground.grid.selectedColumns = [{ name: 'Col 1' }];

        //when
        createElement();

        //then
        expect(element.find('.title').text().trim()).toBe('Col 1');
    });


    it('should render suggestions/stats splitter', inject(() => {
        //when
        createElement();

        //then
        expect(element.find('sc-splitter').length).toBe(1);
        expect(element.find('sc-splitter sc-split-first-pane actions-suggestions').length).toBe(1);
        expect(element.find('sc-splitter sc-split-second-pane stats-details').length).toBe(1);
    }));
});
