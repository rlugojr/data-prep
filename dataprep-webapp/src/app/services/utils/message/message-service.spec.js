/*  ============================================================================

 Copyright (C) 2006-2016 Talend Inc. - www.talend.com

 This source code is available under agreement available at
 https://github.com/Talend/data-prep/blob/master/LICENSE

 You should have received a copy of the agreement
 along with this program; if not, write to Talend SA
 9 rue Pages 92150 Suresnes, France

 ============================================================================*/

describe('Message service', () => {
    'use strict';

    beforeEach(angular.mock.module('data-prep.services.utils'));

    beforeEach(angular.mock.module('pascalprecht.translate', ($translateProvider) => {
        $translateProvider.translations('en_US', {
            'TITLE': 'TITLE_VALUE',
            'CONTENT_WITHOUT_ARG': 'CONTENT_WITHOUT_ARG_VALUE',
            'CONTENT_WITH_ARG': 'CONTENT_WITH_ARG_VALUE : {{argValue}}'
        });
        $translateProvider.preferredLanguage('en_US');
    }));

    beforeEach(inject((toaster) => {
        spyOn(toaster, 'pop').and.returnValue();
    }));

    describe('error', () => {
        it('should show toast on error without translate arg', inject(($timeout, MessageService, toaster) => {
            //given
            var titleId = 'TITLE';
            var contentId = 'CONTENT_WITHOUT_ARG';

            expect(toaster.pop).not.toHaveBeenCalled();

            //when
            MessageService.error(titleId, contentId);
            $timeout.flush(300);

            //then
            expect(toaster.pop).toHaveBeenCalledWith('error', 'TITLE_VALUE', 'CONTENT_WITHOUT_ARG_VALUE', 0);
        }));

        it('should show toast on error with translate arg', inject(($timeout, MessageService, toaster) => {
            //given
            var titleId = 'TITLE';
            var contentId = 'CONTENT_WITH_ARG';
            var args = {argValue: 'my value'};

            expect(toaster.pop).not.toHaveBeenCalled();

            //when
            MessageService.error(titleId, contentId, args);
            $timeout.flush(300);

            //then
            expect(toaster.pop).toHaveBeenCalledWith('error', 'TITLE_VALUE', 'CONTENT_WITH_ARG_VALUE : my value', 0);
        }));
    });

    describe('warning', () => {
        it('should show toast on warning without translate arg', inject(($timeout, MessageService, toaster) => {
            //given
            var titleId = 'TITLE';
            var contentId = 'CONTENT_WITHOUT_ARG';

            expect(toaster.pop).not.toHaveBeenCalled();

            //when
            MessageService.warning(titleId, contentId);
            $timeout.flush(300);

            //then
            expect(toaster.pop).toHaveBeenCalledWith('warning', 'TITLE_VALUE', 'CONTENT_WITHOUT_ARG_VALUE', 0);
        }));

        it('should show toast on warning with translate arg', inject(($timeout, MessageService, toaster) => {
            //given
            var titleId = 'TITLE';
            var contentId = 'CONTENT_WITH_ARG';
            var args = {argValue: 'my value'};

            expect(toaster.pop).not.toHaveBeenCalled();

            //when
            MessageService.warning(titleId, contentId, args);
            $timeout.flush(300);

            //then
            expect(toaster.pop).toHaveBeenCalledWith('warning', 'TITLE_VALUE', 'CONTENT_WITH_ARG_VALUE : my value', 0);
        }));
    });

    describe('success', () => {
        it('should show toast on success without translate arg', inject(($timeout, MessageService, toaster) => {
            //given
            var titleId = 'TITLE';
            var contentId = 'CONTENT_WITHOUT_ARG';

            expect(toaster.pop).not.toHaveBeenCalled();

            //when
            MessageService.success(titleId, contentId);
            $timeout.flush(300);

            //then
            expect(toaster.pop).toHaveBeenCalledWith('success', 'TITLE_VALUE', 'CONTENT_WITHOUT_ARG_VALUE', 5000);
        }));

        it('should show toast on success with translate arg', inject(($timeout, MessageService, toaster) => {
            //given
            var titleId = 'TITLE';
            var contentId = 'CONTENT_WITH_ARG';
            var args = {argValue: 'my value'};

            expect(toaster.pop).not.toHaveBeenCalled();

            //when
            MessageService.success(titleId, contentId, args);
            $timeout.flush(300);

            //then
            expect(toaster.pop).toHaveBeenCalledWith('success', 'TITLE_VALUE', 'CONTENT_WITH_ARG_VALUE : my value', 5000);
        }));
    });

    describe('buffer', () => {
        it('should buffer different messages and display them after 300ms delay', inject(($timeout, MessageService, toaster) => {
            //given
            var firstMessageTitleId = 'TITLE';
            var firstMessageContentId = 'CONTENT_WITH_ARG';
            var firstMessageArgs = {argValue: 'my value'};

            var secondMessageTitleId = 'TITLE';
            var secondMessageContentId = 'CONTENT_WITHOUT_ARG';

            expect(toaster.pop).not.toHaveBeenCalled();

            //when
            MessageService.success(firstMessageTitleId, firstMessageContentId, firstMessageArgs);
            MessageService.error(secondMessageTitleId, secondMessageContentId);
            expect(toaster.pop).not.toHaveBeenCalled();
            $timeout.flush(300);

            //then
            expect(toaster.pop).toHaveBeenCalledWith('success', 'TITLE_VALUE', 'CONTENT_WITH_ARG_VALUE : my value', 5000);
            expect(toaster.pop).toHaveBeenCalledWith('error', 'TITLE_VALUE', 'CONTENT_WITHOUT_ARG_VALUE', 0);
        }));

        it('should only display distinct message once', inject(($timeout, MessageService, toaster) => {
            //given
            var firstMessageTitleId = 'TITLE';
            var firstMessageContentId = 'CONTENT_WITH_ARG';
            var firstMessageArgs = {argValue: 'my value'};

            var secondMessageTitleId = 'TITLE';
            var secondMessageContentId = 'CONTENT_WITHOUT_ARG';

            expect(toaster.pop).not.toHaveBeenCalled();

            //when
            MessageService.success(firstMessageTitleId, firstMessageContentId, firstMessageArgs);
            MessageService.success(firstMessageTitleId, firstMessageContentId, firstMessageArgs);
            MessageService.success(firstMessageTitleId, firstMessageContentId, firstMessageArgs);
            MessageService.error(secondMessageTitleId, secondMessageContentId);
            $timeout.flush(300);

            //then
            expect(toaster.pop.calls.count()).toBe(2);
            expect(toaster.pop).toHaveBeenCalledWith('success', 'TITLE_VALUE', 'CONTENT_WITH_ARG_VALUE : my value', 5000);
            expect(toaster.pop).toHaveBeenCalledWith('error', 'TITLE_VALUE', 'CONTENT_WITHOUT_ARG_VALUE', 0);
        }));
    });
});