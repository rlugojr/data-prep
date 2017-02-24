/*  ============================================================================

  Copyright (C) 2006-2016 Talend Inc. - www.talend.com

  This source code is available under agreement available at
  https://github.com/Talend/data-prep/blob/master/LICENSE

  You should have received a copy of the agreement
  along with this program; if not, write to Talend SA
  9 rue Pages 92150 Suresnes, France

  ============================================================================*/

describe('Transformation form directive', () => {
    'use strict';
    let scope;
    let createElement;

    beforeEach(angular.mock.module('data-prep.transformation-form'));

    beforeEach(angular.mock.module('pascalprecht.translate', ($translateProvider) => {
        $translateProvider.translations('en', {
            COLON: ': ',
        });
        $translateProvider.preferredLanguage('en');
    }));

    beforeEach(inject(($rootScope, $compile) => {
        scope = $rootScope.$new();

        createElement = () => {
            const element = angular.element('<transform-form transformation="transformation" on-submit="onSubmit" is-readonly="isReadonly"></transform-form>');
            $compile(element)(scope);
            scope.$digest();
            return element;
        };
    }));

    it('should render an action with parameters', () => {
        //given
        scope.transformation = {
            name: 'menuWithParam',
            label: 'menu with param',
            parameters: [
                {
                    name: 'param1',
                    label: 'Param 1',
                    type: 'string',
                    inputType: 'text',
                    default: '.',
                },
                {
                    name: 'param2',
                    label: 'Param 2',
                    type: 'integer',
                    inputType: 'number',
                    default: '5',
                },
            ],
        };

        //when
        const element = createElement();

        //then
        expect(element.find('.param-name').length).toBe(2);
        expect(element.find('.param-name').eq(0).text().trim()).toBe('Param 1:');
        expect(element.find('.param-name').eq(1).text().trim()).toBe('Param 2:');
        expect(element.find('.param-input').length).toBe(2);
        expect(element.find('.param-input').eq(0).find('input[type="text"]').length).toBe(1);
        expect(element.find('.param-input').eq(1).find('input[type="number"]').length).toBe(1);
    });

    it('should render an action with simple choice', () => {
        //given
        scope.transformation = {
            name: 'menuXithParam',
            label: 'menu with param',
            parameters: [
                {
                    name: 'myChoice',
                    label: 'my choice',
                    type: 'select',
                    configuration: {
                        values: [
                            { label: 'noParamChoice1', value: 'noParamChoice1' },
                            { label: 'noParamChoice2', value: 'noParamChoice2' },
                        ],
                    },
                    default: '',
                },
            ],
        };

        //when
        const element = createElement();

        //then
        let paramChoice = element.find('.param').eq(0);
        expect(paramChoice.find('.param-name').length).toBe(1);
        expect(paramChoice.find('.param-name').eq(0).text().trim()).toBe('my choice:');
        expect(paramChoice.find('.param-input').length).toBe(1);
        expect(paramChoice.find('.param-input').eq(0).find('select').length).toBe(1);
        expect(paramChoice.find('.param-input').eq(0).find('option').length).toBe(2);
        expect(paramChoice.find('.param-input').eq(0).find('option').eq(0).text()).toBe('noParamChoice1');
        expect(paramChoice.find('.param-input').eq(0).find('option').eq(1).text()).toBe('noParamChoice2');
    });

    it('should render an action with choice containing parameters', () => {
        //given
        const parameter = {
            name: 'my choice',
            label: 'my choice',
            type: 'select',
            configuration: {
                values: [
                    { name: 'noParamChoice', value: 'noParamChoice' },
                    {
                        name: 'twoParams',
                        value: 'twoParams',
                        parameters: [
                            {
                                label: 'Param 1',
                                name: 'param1',
                                type: 'string',
                                default: '.',
                            },
                            {
                                label: 'Param 2',
                                name: 'param2',
                                type: 'float',
                                default: '5',
                            },
                        ],
                    },
                ],
            },
            default: '',
        };
        scope.transformation = {
            name: 'menu with param',
            parameters: [parameter],
        };
        const element = createElement();
        const renderedParams = element.find('transform-params').eq(0);

        //when
        parameter.value = parameter.configuration.values[0];
        scope.$apply();

        //then
        expect(renderedParams.find('.param-name').length).toBe(1); // choice name only

        //when
        parameter.value = parameter.configuration.values[1].value;
        scope.$apply();

        //then
        expect(renderedParams.find('.param-name').length).toBe(3); // choice name + 2 input params name
        expect(renderedParams.find('.param-name').eq(1).text().trim()).toBe('Param 1:');
        expect(renderedParams.find('.param-name').eq(2).text().trim()).toBe('Param 2:');

        expect(renderedParams.find('.param-input').length).toBe(3); // choice + 2 input params
        expect(renderedParams.find('.param-input').eq(1).find('input[type="text"]').length).toBe(1);
        expect(renderedParams.find('.param-input').eq(2).find('input[type="number"]').length).toBe(1);
    });

    it('should render an action with cluster parameters', () => {
        //given
        scope.transformation = {
            name: 'menu with param',
            cluster: {
                titles: [
                    'We found these values',
                    'And we\'ll keep this value',
                ],
                clusters: [
                    {
                        parameters: [
                            {
                                name: 'Texa',
                                type: 'boolean',
                                description: 'parameter.Texa.desc',
                                label: 'parameter.Texa.label',
                                default: 'true',
                            },
                            {
                                name: 'Tixass',
                                type: 'boolean',
                                description: 'parameter.Tixass.desc',
                                label: 'parameter.Tixass.label',
                                default: 'true',
                            },
                            {
                                name: 'Tex@s',
                                type: 'boolean',
                                description: 'parameter.Tex@s.desc',
                                label: 'parameter.Tex@s.label',
                                default: 'true',
                            },
                        ],
                        replace: {
                            name: 'replaceValue',
                            type: 'string',
                            description: 'parameter.replaceValue.desc',
                            label: 'parameter.replaceValue.label',
                            default: 'Texas',
                        },
                    },
                    {
                        parameters: [
                            {
                                name: 'Massachusetts',
                                type: 'boolean',
                                description: 'parameter.Massachusetts.desc',
                                label: 'parameter.Massachusetts.label',
                                default: 'false',
                            },
                            {
                                name: 'Masachusetts',
                                type: 'boolean',
                                description: 'parameter.Masachusetts.desc',
                                label: 'parameter.Masachusetts.label',
                                default: 'true',
                            },
                            {
                                name: 'Massachussetts',
                                type: 'boolean',
                                description: 'parameter.Massachussetts.desc',
                                label: 'parameter.Massachussetts.label',
                                default: 'true',
                            },
                            {
                                name: 'Massachusets',
                                type: 'boolean',
                                description: 'parameter.Massachusets.desc',
                                label: 'parameter.Massachusets.label',
                                default: 'true',
                            },
                            {
                                name: 'Masachussets',
                                type: 'boolean',
                                description: 'parameter.Masachussets.desc',
                                label: 'parameter.Masachussets.label',
                                default: 'true',
                            },
                        ],
                        replace: {
                            name: 'replaceValue',
                            type: 'string',
                            description: 'parameter.replaceValue.desc',
                            label: 'parameter.replaceValue.label',
                            default: 'Massachussets',
                        },
                    },
                ],
            },
        };

        //when
        const element = createElement();

        //then
        expect(element.find('.cluster').length).toBe(1);
    });

    it('should render doc link when there is a docUrl parameter', () => {
        //given
        scope.transformation = {
            name: 'menuWithParam',
            label: 'menu with param',
            parameters: [
                {
                    name: 'param1',
                    label: 'Param 1',
                    type: 'string',
                    inputType: 'text',
                    default: '.',
                },
                {
                    name: 'param2',
                    label: 'Param 2',
                    type: 'integer',
                    inputType: 'number',
                    default: '5',
                },
            ],
            docUrl: 'http://www.google.com',
        };

        //when
        const element = createElement();

        //then
        const docLink = element.find('.param-buttons > a').eq(0);
        expect(docLink.attr('href')).toBe('http://www.google.com');
    });

    it('should render submit button', () => {
        //given
        scope.isReadonly = false;

        //when
        const element = createElement();

        //then
        expect(element.find('.param-buttons > button').length).toBe(1);
    });

    it('should not render submit button', () => {
        //given
        scope.isReadonly = true;

        //when
        const element = createElement();

        //then
        expect(element.find('.param-buttons > button').length).toBe(0);
    });
});
