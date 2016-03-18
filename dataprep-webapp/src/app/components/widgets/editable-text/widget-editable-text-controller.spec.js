/*  ============================================================================

  Copyright (C) 2006-2016 Talend Inc. - www.talend.com

  This source code is available under agreement available at
  https://github.com/Talend/data-prep/blob/master/LICENSE

  You should have received a copy of the agreement
  along with this program; if not, write to Talend SA
  9 rue Pages 92150 Suresnes, France

  ============================================================================*/

describe('Editable Text Widget', function() {
    'use strict';

    var createController, scope;
    var onValidateFn, onCancelFn;

    beforeEach(angular.mock.module('talend.widget'));
    beforeEach(angular.mock.module('htmlTemplates'));

    beforeEach(inject(function ($rootScope, $componentController) {
        scope = $rootScope.$new();
        onValidateFn = jasmine.createSpy('onValidateFn');
        onCancelFn = jasmine.createSpy('onCancelFn');

        createController = function () {
            return $componentController('talendEditableText',
                {$scope: scope},
                {
                    onValidate: onValidateFn,
                    onCancel: onCancelFn
                }
            );
        };
    }));

    it('should set edition text to the original text', function() {
        //given
        var ctrl = createController();
        ctrl.text = 'Jimmy';

        expect(ctrl.editionText).toBeFalsy();

        //when
        ctrl.reset();

        //then
        expect(ctrl.editionText).toBe('Jimmy');
    });

    describe('edit mode', function() {
        it('should set edition text to the original text', function() {
            //given
            var ctrl = createController();
            ctrl.text = 'Jimmy';

            expect(ctrl.editionText).toBeFalsy();

            //when
            ctrl.edit();

            //then
            expect(ctrl.editionText).toBe('Jimmy');
        });

        it('should sswitch to edition mode', function() {
            //given
            var ctrl = createController();
            ctrl.editionMode = false;

            //when
            ctrl.edit();

            //then
            expect(ctrl.editionMode).toBe(true);
        });
    });

    describe('validation', function() {
        describe('validate everytime', () => {
            it('should execute validation callback when text has changed', function() {
                //given
                var ctrl = createController();
                ctrl.text = 'Jimmy';
                ctrl.editionText = 'new Jimmy';

                expect(onValidateFn).not.toHaveBeenCalled();

                //when
                ctrl.validate();

                //then
                expect(onValidateFn).toHaveBeenCalled();
            });

            it('should execute validation callback when text has NOT changed', function() {
                //given
                var ctrl = createController();
                ctrl.text = 'Jimmy';
                ctrl.editionText = 'Jimmy';

                expect(onValidateFn).not.toHaveBeenCalled();

                //when
                ctrl.validate();

                //then
                expect(onValidateFn).toHaveBeenCalled();
            });
        });

        describe('validate only on change', () => {
            it('should execute validation callback when text has changed', function() {
                //given
                var ctrl = createController();
                ctrl.text = 'Jimmy';
                ctrl.editionText = 'new Jimmy';
                ctrl.validateOnlyOnChange = ''; //defined

                expect(onValidateFn).not.toHaveBeenCalled();

                //when
                ctrl.validate();

                //then
                expect(onValidateFn).toHaveBeenCalled();
            });

            it('should NOT execute validation callback when text has NOT changed', function() {
                //given
                var ctrl = createController();
                ctrl.text = 'Jimmy';
                ctrl.editionText = 'Jimmy';
                ctrl.validateOnlyOnChange = ''; //defined

                expect(onValidateFn).not.toHaveBeenCalled();

                //when
                ctrl.validate();

                //then
                expect(onValidateFn).not.toHaveBeenCalled();
            });
        });

        it('should switch to non edition mode', function() {
            //given
            var ctrl = createController();
            ctrl.editionMode = true;

            //when
            ctrl.validate();

            //then
            expect(ctrl.editionMode).toBe(false);
        });
    });

    describe('cancel', function() {
        it('should execute cancel callback', function() {
            //given
            var ctrl = createController();

            //when
            ctrl.cancel();

            //then
            expect(onCancelFn).toHaveBeenCalled();
        });

        it('should switch to non edition mode', function() {
            //given
            var ctrl = createController();
            ctrl.editionMode = true;

            //when
            ctrl.cancel();

            //then
            expect(ctrl.editionMode).toBe(false);
        });
    });
});