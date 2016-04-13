'use strict';

var path = require('path');
var conf = require('./gulp/conf');

var argv = require('yargs').argv;
var webpackConfig = require('./webpack.conf.test');

var _ = require('lodash');
var wiredep = require('wiredep');

function listFiles() {
    var wiredepOptions = _.extend({}, conf.wiredep, {
        dependencies: true,
        devDependencies: true
    });

    var patterns = wiredep(wiredepOptions).js
        .concat([
            path.join(conf.paths.src, '/app/**/*-module.js'),
            path.join(conf.paths.src, '/app/**/' + (argv.folder ? argv.folder + '/**/' : '') + '*.spec.js'),
            path.join(conf.paths.src, '/**/*.html'),
            path.join(conf.paths.src, '/mocks/**/*.js')
        ]);

    var files = patterns.map(function (pattern) {
        return {
            pattern: pattern
        };
    });
    files.push({
        pattern: path.join(conf.paths.src, '/assets/**/*'),
        included: false,
        served: true,
        watched: false
    });
    return files;
}

module.exports = function (config) {

    var configuration = {
        files: listFiles(),

        singleRun: !argv.auto,

        autoWatch: !!argv.auto,

        ngHtml2JsPreprocessor: {
            stripPrefix: 'src/',
            moduleName: 'htmlTemplates'
        },

        logLevel: config.LOG_WARN,

        frameworks: ['jasmine'],

        browsers: ['PhantomJS'],

        plugins: [
            'karma-chrome-launcher',
            'karma-phantomjs-launcher',
            'karma-coverage',
            'karma-jasmine',
            'karma-ng-html2js-preprocessor',
            'karma-webpack'
        ],

        coverageReporter: {
            type: 'html',
            dir: 'coverage/'
        },

        reporters: ['progress', 'coverage'],

        preprocessors: {
            'src/**/*.html': ['ng-html2js'],
            'src/app/**/*.js': ['webpack']
        },

        webpack: webpackConfig,

        proxies: {
            '/assets/': path.join('/base/', conf.paths.src, '/assets/')
        }
    };

    config.set(configuration);
};