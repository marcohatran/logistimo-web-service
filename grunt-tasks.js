/*
 * Copyright © 2017 Logistimo.
 *
 * This file is part of Logistimo.
 *
 * Logistimo software is a mobile & web platform for supply chain management and remote temperature monitoring in
 * low-resource settings, made available under the terms of the GNU Affero General Public License (AGPL).
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.  If not, see
 * <http://www.gnu.org/licenses/>.
 *
 * You can be released from the requirements of the license by purchasing a commercial license. To know more about
 * the commercial license, please contact us at opensource@logistimo.com
 */


/**
 * Created by charan on 06/04/15.
 */
/*global module:false*/
module.exports = function(grunt, ref) {
    'use strict';

    // Project configuration.
    grunt.config.merge({
        // Metadata.
        pkg: grunt.file.readJSON('package.json'),

        //Build timestamp
        now: grunt.template.today('yyyymmddhhMM'),

        gruntfile: {
            src: 'Gruntfile.js'
        },
        banner: {
            'short': '/*! ' +
            '<%= pkg.name %>' +
            '<%= pkg.version ? " v" + pkg.version : "" %>' +
            '<%= now %>' +
            ' */'
        },
        env: {
            local: {
                NODE_ENV: 'LOCAL',
                BULLETIN_BOARD: 'NA'
            },
            dev: {
                NODE_ENV: 'DEVELOPMENT',
                BULLETIN_BOARD: 'NA'
            },
            prod: {
                NODE_ENV: 'PRODUCTION',
                BULLETIN_BOARD: 'NA'
            },
            bulletinboard: {
                BULLETIN_BOARD: 'BULLETIN_BOARD'
            }
        },
        shell: {
            convertUTF8: {
                command:[
                    '/usr/bin/native2ascii -encoding UTF-8 target/grunt/resources/LogistimoMessages_en.properties target/grunt/resources/LogistimoMessages_en.properties',
                    '/usr/bin/native2ascii -encoding UTF-8 target/grunt/resources/LogistimoMessages_fr.properties target/grunt/resources/LogistimoMessages_fr.properties',
                    '/usr/bin/native2ascii -encoding UTF-8 target/grunt/resources/LogistimoMessages_hi.properties target/grunt/resources/LogistimoMessages_hi.properties'

                ].join('&&')
            }
        },
        if: {
            default: {
                options: {
                    test: function(){ return grunt.config('custom') != undefined; }
                },
                ifTrue: ['copy:favicon', 'string-replace:custom']

            },
            rb: {

                options: {
                    config: 'custom.rbChanges'
                },
                ifTrue: ['json_string_replace:default', 'json_string_replace:properties',
                    'string-replace:helpcustom']

            }
        },
        copy: {
            main: {
                expand: true,
                src: '<%= baseurl %>/modules/common/src/main/resources/original/*.properties.orig',
                dest: 'target/grunt/resources/',
                flatten: true
            },
            properties : {
                src: '<%= baseurl %>/modules/common/src/main/resources/errors_*.properties',
                dest: 'target/grunt/resources/',
                flatten: true,
                expand: true
            },
            src: {
                expand: true,
                cwd: '<%= baseurl %>/modules/web/src/main/webapp/',
                src: '**',
                dest: 'target/grunt/webapp/'
            },
            favicon: {
                src: 'target/grunt/webapp/<%= custom.favicon%>',
                dest: 'target/grunt/webapp/titleiconnew.ico'
            },
            resource : {
                expand: true,
                src: '<%= baseurl %>/modules/web/src/main/webapp/v2/i18n/resourceBundle*.json',
                dest: 'target/grunt/webapp/v2/i18n/',
                flatten: true
            },
            rb2src : {
                expand: true,
                dest: '<%= baseurl %>/modules/web/src/main/webapp/v2/i18n/',
                src: 'target/grunt/webapp/v2/i18n/resourceBundle*.json',
                flatten: true
            },
            res2src : {
                expand: true,
                dest: '<%= baseurl %>/modules/common/src/main/resources',
                src: 'target/grunt/resources/*_*.properties',
                flatten: true
            },
            bulletinboard: {
                src: '<%= baseurl %>/modules/web/src/main/webapp/v2/index.html',
                dest: 'target/grunt/webapp/v2/bulletinboard.html'
            },
            bulletinboardlist: {
                src: 'target/grunt/webapp/v2/views/bulletinboard/list-bulletin-boards.html',
                dest: 'target/grunt/webapp/v2/views/list-bulletin-boards.html'
            },
            bulletinboardApp: {
                src: '<%= baseurl %>/modules/web/src/main/webapp/v2/js/app.js',
                dest: 'target/grunt/webapp/v2/js/bulletinboard-app.js'
            }
        },
        rename: {
            en: {
                files: [
                    {src: ['target/grunt/resources/LogistimoMessages_en.properties.orig'], dest: 'target/grunt/resources/LogistimoMessages_en.properties'}
                ]
            },
            fr: {
                files: [
                    {src: ['target/grunt/resources/LogistimoMessages_fr.properties.orig'], dest: 'target/grunt/resources/LogistimoMessages_fr.properties'}
                ]
            },
            hi: {
                files: [
                    {src: ['target/grunt/resources/LogistimoMessages_hi.properties.orig'], dest: 'target/grunt/resources/LogistimoMessages_hi.properties'}
                ]
            },
            resource : {
                files: [
                    {src: ['target/grunt/webapp/v2/i18n/resourceBundle_en.json'], dest: 'target/grunt/webapp/v2/i18n/<%= now %>-resourceBundle_en.json'},
                    {src: ['target/grunt/webapp/v2/i18n/resourceBundle_fr.json'], dest: 'target/grunt/webapp/v2/i18n/<%= now %>-resourceBundle_fr.json'},
                    {src: ['target/grunt/webapp/v2/i18n/resourceBundle_hi.json'], dest: 'target/grunt/webapp/v2/i18n/<%= now %>-resourceBundle_hi.json'}
                ]
            }
        },
        propertiesToJSON: {
            en: {
                src: ['target/grunt/resources/LogistimoMessages_en.properties'],
                dest: 'target/grunt/webapp/v2/i18n/resourceBundle_en.json',
                options: {
                    merge: true
                }
            },
            fr: {
                src: ['target/grunt/resources/LogistimoMessages_fr.properties'],
                dest: 'target/grunt/webapp/v2/i18n/resourceBundle_fr.json',
                options: {
                    merge: true
                }
            },
            hi: {
                src: ['target/grunt/resources/LogistimoMessages_hi.properties'],
                dest: 'target/grunt/webapp/v2/i18n/resourceBundle_hi.json',
                options: {
                    merge: true
                }
            }
        },
        cssmin: {
            options: {
                shorthandCompacting: false,
                roundingPrecision: -1
            },
            prod: {
                files: {
                    'target/grunt/webapp/v2/css/<%= pkg.suffix %>-<%= now %>.min.css':
                        ['target/grunt/webapp/v2/css/navbar.css',
                            'target/grunt/webapp/v2/css/logistimo.css',
                            'target/grunt/webapp/v2/css/animations.css',
                            'target/grunt/webapp/v2/css/toastr.css',
                            'target/grunt/webapp/v2/plugins/report/css/menu.css',
                            'target/grunt/webapp/v2/plugins/report/css/widget.css'
                        ],
                    'target/grunt/webapp/v2/css/temperature-<%= now %>.min.css':['target/grunt/webapp/v2/css/temperature.css']
                }
            }
        },
        html2js: {
            options: {
                banner: '<%= banner.short %>',
                base: 'target/grunt/webapp/v2',
                module: 'logistimoApp.templates',
                singleModule: true,
                useStrict: true,
                rename: rename,
                htmlmin: {
                    removeComments: true,
                    keepClosingSlash: true
                }
            },
            main: {
                src: [
                    "target/grunt/webapp/v2/views/**/*.html",
                    "target/grunt/webapp/v2/views/*.html",
                    "target/grunt/webapp/v2/plugins/reports/**/*.html",
                    "target/grunt/webapp/v2/plugins/reports/*.html",
                    "target/grunt/webapp/v2/plugins/storyboards/**/*.html",
                    "target/grunt/webapp/v2/plugins/storyboards/*.html"
                ],
                dest: "target/grunt/webapp/v2/js/<%= pkg.suffix %>-tpl-<%= now %>.js"
            }
        },
        ngAnnotate:{
            options: {
                singleQuotes: true
            },
            prod: {
                files: {
                    'target/grunt/webapp/v2/js/logistimo.js': [
                        'target/grunt/webapp/v2/js/app.js',
                        'target/grunt/webapp/v2/js/re*.js',
                        'target/grunt/webapp/v2/js/api-services.js',
                        'target/grunt/webapp/v2/js/app-*.js',
                        'target/grunt/webapp/v2/js/utils/*.js',
                        'target/grunt/webapp/v2/js/components/*/*.js',
                        'target/grunt/webapp/v2/plugins/reports/*.js',
                        'target/grunt/webapp/v2/plugins/reports/**/*.js',
                        'target/grunt/webapp/v2/plugins/storyboards/*.js',
                        'target/grunt/webapp/v2/plugins/storyboards/**/*.js',
                        'target/grunt/webapp/v2/js/analytics-service.js',
                        'target/grunt/webapp/v2/js/password-validator.js'
                    ]
                }
            },
            bulletinboard: {
                files: {
                    'target/grunt/webapp/v2/js/bulletinboard.js': [
                        'target/grunt/webapp/v2/js/bulletinboard-app.js',
                        'target/grunt/webapp/v2/js/re*.js',
                        'target/grunt/webapp/v2/js/api-services.js',
                        'target/grunt/webapp/v2/js/app-*.js',
                        'target/grunt/webapp/v2/js/utils/*.js',
                        'target/grunt/webapp/v2/js/components/*/*.js',
                        'target/grunt/webapp/v2/plugins/reports/*.js',
                        'target/grunt/webapp/v2/plugins/reports/**/*.js',
                        'target/grunt/webapp/v2/plugins/storyboards/*.js',
                        'target/grunt/webapp/v2/plugins/storyboards/**/*.js',
                        'target/grunt/webapp/v2/js/analytics-service.js'
                    ]
                }
            }
        },
        'string-replace':
        {
            dist: {
                files: {
                    'target/grunt/webapp/v2/js/logistimo.js': 'target/grunt/webapp/v2/js/logistimo.js',
                    'target/grunt/webapp/v2/js/bulletinboard.js': 'target/grunt/webapp/v2/js/bulletinboard.js'
                },
                options: {
                    replacements: [{
                        pattern: /[/*<%]{4}.*[%>*/]{4}/g,
                        replacement: ",'logistimoApp.templates'"
                    }]
                }
            },
            custom: {
                files: {
                    'target/grunt/webapp/m/index.html': 'target/grunt/webapp/m/index.html',
                    'target/grunt/webapp/v2/index.html': 'target/grunt/webapp/v2/index.html',
                    'target/grunt/webapp/v2/bulletinboard.html': 'target/grunt/webapp/v2/bulletinboard.html',
                    'target/grunt/webapp/v2/views/bulletin-board-login.html': 'target/grunt/webapp/v2/views/bulletin-board-login.html',
                    'target/grunt/webapp/v2/views/bulletinboard/list-bulletin-boards.html': 'target/grunt/webapp/v2/views/bulletinboard/list-bulletin-boards.html',
                    'target/grunt/webapp/v2/views/bulletinboard/view-bulletin-board.html':'target/grunt/webapp/v2/views/bulletinboard/view-bulletin-board.html',
                    'target/grunt/webapp/v2/js/app.js': 'target/grunt/webapp/v2/js/app.js',
                    'target/grunt/webapp/v2/js/bulletinboard-app.js': 'target/grunt/webapp/v2/js/bulletinboard-app.js',
                    'target/grunt/webapp/v2/views/login.html': 'target/grunt/webapp/v2/views/login.html',
                    'target/grunt/webapp/v2/views/menu.html': 'target/grunt/webapp/v2/views/menu.html',
                    'target/grunt/webapp/v2/views/forgot-password.html': 'target/grunt/webapp/v2/views/forgot-password.html',
                    'target/grunt/webapp/v2/mobile-pwd-reset-success.html': 'target/grunt/webapp/v2/mobile-pwd-reset-success.html',
                    'target/grunt/webapp/v2/password-request.html': 'target/grunt/webapp/v2/password-request.html',
                    'target/grunt/webapp/v2/password-reset-success.html': 'target/grunt/webapp/v2/password-reset-success.html',
                    'target/grunt/webapp/v2/password-reset-error.html': 'target/grunt/webapp/v2/password-reset-error.html',
                    'target/grunt/webapp/v2/help/': 'target/grunt/webapp/v2/help/**/*.html'
                },
                options: {
                    replacements: [
                        {
                            pattern: /href="tel:\+[0-9]+"/g,
                            replacement: "href=\"tel:<%= custom.contact %>\""
                        },
                        {
                            pattern: /href="mailto:[a-zA-Z0-9\._]+@[a-zA-Z0-9]+\.([a-zA-Z]+(\.?)){2}"/g,
                            replacement: "href=\"<%= custom.mail %>\""
                        },{
                            pattern: "<title>Logistimo</title>",
                            replacement: "<title><%= custom.title %></title>"
                        },{
                            pattern: "client=&key=",
                            replacement: "client=<%= custom.googleid %>"
                        },{
                            pattern: "client=&",
                            replacement: "key=<%= custom.googlekey %>&"
                        }, {
                            pattern: "http://www.logistimo.com/privacy-policy.html",
                            replacement: "<%= custom.privacylink %>"
                        }, {
                            pattern: "acquia_marina_logo.png",
                            replacement: "<%= custom.logo %>"
                        },{
                            pattern: "google-client-id",
                            replacement: "<%= custom.googleid %>"
                        },{
                            pattern: "google-key",
                            replacement: "<%= custom.googlekey %>"
                        },{
                            pattern: "6Le9aWoUAAAAAHLTsF33wMqSvppWReTDi2marGvi",
                            replacement: "<%= custom.captchakey %>"
                        }
                    ]
                }
            },
            helpcustom: {
                files: {
                    'target/grunt/webapp/v2/help/': 'target/grunt/webapp/v2/help/**/*.html'
                },
                options: {
                    replacements: [
                        {
                            pattern: /entity/g,
                            replacement: "store"
                        },
                        {
                            pattern: /Entity/g,
                            replacement: "Store"
                        },{
                            pattern: /entities/g,
                            replacement: "stores"
                        },{
                            pattern: /Entities/g,
                            replacement: "Stores"
                        },
                        {
                            pattern: /vendor/g,
                            replacement: "issuing store"
                        },
                        {
                            pattern: /customer/g,
                            replacement: "receiving store"
                        },
                        {
                            pattern: /purchase\sorders/g,
                            replacement: "indents (receipt)"
                        },
                        {
                            pattern: /purchase\sorder/g,
                            replacement: "indent (receipt)"
                        },
                        {
                            pattern: /sales\sorders/g,
                            replacement: "indents (issue)"
                        },
                        {
                            pattern: /sales\sorder/g,
                            replacement: "indent (issue)"
                        },
                        {
                            pattern: /purchase/g,
                            replacement: "receipt"
                        },
                        {
                            pattern: /Purchase/g,
                            replacement: "Receipt"
                        },
                        {
                            pattern: /sales/g,
                            replacement: "issue"
                        },
                        {
                            pattern: /Sales/g,
                            replacement: "Issue"
                        }
                    ]
                }
            }
        },
        json_string_replace: {
            default: {
                options: {
                    replacements: {
                        "Customer/Vendor":"Receiving/Issuing store",
                        "customer": "receiving store",
                        "Customer": "Receiving store",
                        "Customers": "Receiving stores",
                        "vendor": "issuing store",
                        "Vendor": "Issuing store",
                        "issues": "issues/net utilization",
                        "Issues": "Issues/Net utilization",
                        "entity": "store",
                        "Entity": "Store",
                        "entities": "stores",
                        "Entities": "Stores",
                        "an store": "a store",
                        "An store": "A store",
                        "an Store": "a Store",
                        "An Store": "A Store",
                        "A Issuing store": "An Issuing store",
                        "A issuing store": "An issuing store",
                        "a Issuing store": "an Issuing store",
                        "a issuing store": "an issuing store",
                        "Sales orders": "Indents (issue)",
                        "Purchase orders": "Indents (receipt)",
                        "stock on hand": "total stock",
                        "Stock on hand":"Total stock",
                        "Purchase approval":"Indent (receipt) approval",
                        "Sales approval":"Indent (issue) approval",
                        "Purchases and sales": "Indents (receipt and issue)",
                        "Purchase":"Receipt",
                        "Sales":"Issue",
                        "purchase and sales orders":"indents (receipt and issue)",
                        "purchase or sales orders":"indents (receipt or issue)",
                        "sales orders": "indents (issue)",
                        "purchase orders": "indents (receipt)",
                        "purchase order": "indent (receipt)",
                        "sales order":"indent (issue)",
                        "sales": "issues",
                        "purchase": "receipt",
                        "Iota app": "Store app",
                        "Pulse app": "Monitor app"
                    },
                    global: true
                },
                files: {
                    'rb': ['target/grunt/webapp/v2/i18n/resourceBundle_en.json']
                }
            },
            properties: {
                options: {
                    replacements: {
                        "Customer/Vendor":"Receiving/Issuing store",
                        "customer": "receiving store",
                        "Customer": "Receiving store",
                        "Customers": "Receiving stores",
                        "vendor": "issuing store",
                        "Vendor": "Issuing store",
                        "issues": "issues/net utilization",
                        "Issues": "Issues/Net utilization",
                        "entity": "store",
                        "Entity": "Store",
                        "entities": "stores",
                        "Entities": "Stores",
                        "an store": "a store",
                        "An store": "A store",
                        "an Store": "a Store",
                        "An Store": "A Store",
                        "A Issuing store": "An Issuing store",
                        "A issuing store": "An issuing store",
                        "a Issuing store": "an Issuing store",
                        "a issuing store": "an issuing store",
                        "Sales orders": "Indents (issue)",
                        "Purchase orders": "Indents (receipt)",
                        "stock on hand": "total stock",
                        "Stock on hand":"Total stock",
                        "Purchase approval":"Indent (receipt) approval",
                        "Sales approval":"Indent (issue) approval",
                        "Purchases and sales": "Indents (receipt and issue)",
                        "Purchase":"Receipt",
                        "Sales":"Issue",
                        "purchase and sales orders":"indents (receipt and issue)",
                        "purchase or sales orders":"indents (receipt or issue)",
                        "sales orders": "indents (issue)",
                        "purchase orders": "indents (receipt)",
                        "purchase order": "indent (receipt)",
                        "sales order":"indent (issue)",
                        "sales": "issues",
                        "purchase": "receipt",
                        "Iota app": "Store app",
                        "Pulse app": "Monitor app"
                    },
                    global: true,
                    type: 'props'
                },
                files: {
                    'prop': ['target/grunt/resources/LogistimoMessages_en.properties', 'target/grunt/resources/errors_en.properties']
                }
            }
        },
        uglify: {
            options:{
                banner: '<%= banner.short %>'
            },
            prod: {
                files: {
                    'target/grunt/webapp/v2/js/<%= pkg.suffix %>-<%= now %>.min.js': ['target/grunt/webapp/v2/js/logistimo.js'],
                    'target/grunt/webapp/v2/js/<%= pkg.suffix %>-bb-<%= now %>.min.js': ['target/grunt/webapp/v2/js/bulletinboard.js']
                }
            }
        },
        toggleComments: {
            customOptions: {
                options: {
                    removeCommands: true
                },
                files: {
                    "target/grunt/webapp/v2/index.html": "target/grunt/webapp/v2/index.html",
                    "target/grunt/webapp/v2/bulletinboard.html": "target/grunt/webapp/v2/bulletinboard.html",
                    "target/grunt/webapp/v2/views/list-bulletin-boards.html": "target/grunt/webapp/v2/views/list-bulletin-boards.html",
                    "target/grunt/webapp/v2/mobile-pwd-reset-success.html": "target/grunt/webapp/v2/mobile-pwd-reset-success.html",
                    "target/grunt/webapp/v2/password-reset-error.html": "target/grunt/webapp/v2/password-reset-error.html",
                    "target/grunt/webapp/v2/password-reset-success.html": "target/grunt/webapp/v2/password-reset-success.html",
                    "target/grunt/webapp/v2/views/menu.html": "target/grunt/webapp/v2/views/menu.html",
                    "target/grunt/webapp/v2/views/login.html": "target/grunt/webapp/v2/views/login.html",
                    "target/grunt/webapp/v2/views/forgot-password.html": "target/grunt/webapp/v2/views/forgot-password.html",
                    "target/grunt/resources/samaanguru.properties": "<%= baseurl %>/modules/common/src/main/resources/samaanguru.properties",
                    "target/grunt/webapp/v2/password-request.html": "target/grunt/webapp/v2/password-request.html"
                }
            }
        },
        preprocess: {
            options: {
                context: {
                    name: '<%= pkg.name %>',
                    now: '<%= now %>',
                    version: '<%= pkg.version %>',
                    scriptInc: '<script src="js/<%= pkg.suffix %>-<%= now %>.min.js"></script>',
                    bbScriptInc: '<script src="js/<%= pkg.suffix %>-bb-<%= now %>.min.js"></script>',
                    templateInc: '<script src="js/<%= pkg.suffix %>-tpl-<%= now %>.js"></script>',
                    bbTemplateInc: '<script src="js/<%= pkg.suffix %>-tpl-<%= now %>.js"></script>',
                    cssInc: '<link rel="stylesheet" href="css/<%= pkg.suffix %>-<%= now %>.min.css" />',
                    tempCssInc: '<link rel="stylesheet" href="css/temperature-<%= now %>.min.css" />',
                    appVerScriptInc: "setCookie('web.app.ver','<%= now %>',30);",
                    appVerInc: "web.app.ver=<%= now %>",
                    resourceInc: 'resourceBundleName = "<%= now %>-"+resourceBundleName;'
                }
            },
            afterUglify: {
                files: [
                    {
                        src: 'target/grunt/webapp/v2/index.html',
                        dest: 'target/grunt/webapp/v2/index.html'
                    },
                    {
                        src: 'target/grunt/resources/samaanguru.properties',
                        dest: 'target/grunt/resources/samaanguru.properties'
                    },
                    {
                        src: 'target/grunt/webapp/v2/mobile-pwd-reset-success.html',
                        dest: 'target/grunt/webapp/v2/mobile-pwd-reset-success.html'
                    },
                    {
                        src: 'target/grunt/webapp/v2/password-reset-error.html',
                        dest: 'target/grunt/webapp/v2/password-reset-error.html'
                    },
                    {
                        src: 'target/grunt/webapp/v2/password-reset-success.html',
                        dest: 'target/grunt/webapp/v2/password-reset-success.html'
                    },
                    {
                        src: 'target/grunt/webapp/v2/password-request.html',
                        dest: 'target/grunt/webapp/v2/password-request.html'
                    }
                ]
            },
            beforeUglify: {
                files: [
                    {
                        src: 'target/grunt/webapp/v2/views/temperature/temperature.html',
                        dest: 'target/grunt/webapp/v2/views/temperature/temperature.html'
                    },
                    {
                        src: 'target/grunt/webapp/v2/views/temperature/temperature-devices.html',
                        dest: 'target/grunt/webapp/v2/views/temperature/temperature-devices.html'
                    },
                    {
                        src: 'target/grunt/webapp/v2/views/menu.html',
                        dest: 'target/grunt/webapp/v2/views/menu.html'
                    },
                    {
                        src: 'target/grunt/webapp/v2/views/login.html',
                        dest: 'target/grunt/webapp/v2/views/login.html'
                    },
                    {
                        src: 'target/grunt/webapp/v2/views/forgot-password.html',
                        dest: 'target/grunt/webapp/v2/views/forgot-password.html'
                    },
                    {
                        src: 'target/grunt/webapp/v2/js/app-controller.js',
                        dest: 'target/grunt/webapp/v2/js/app-controller.js'
                    },
                    {
                        src: 'target/grunt/webapp/v2/js/app.js',
                        dest: 'target/grunt/webapp/v2/js/app.js'
                    }
                ]
            },
            bulletinboardUglify: {
                files: [

                    {
                        src: 'target/grunt/webapp/v2/js/bulletinboard-app.js',
                        dest: 'target/grunt/webapp/v2/js/bulletinboard-app.js'
                    }
                ]
            },
            bulletinboardAfterUglify: {
                files: [

                    {
                        src: 'target/grunt/webapp/v2/bulletinboard.html',
                        dest: 'target/grunt/webapp/v2/bulletinboard.html'
                    },
                    {
                        src: 'target/grunt/webapp/v2/views/list-bulletin-boards.html',
                        dest: 'target/grunt/webapp/v2/views/list-bulletin-boards.html'
                    }
                ]
            }
        },
        clean: {
            dist: {
                src: [ 'grunt' ]
            }
        },
       //jshint reporting
        jshint: {
                options: {
                        reporter: require('jshint-jenkins-checkstyle-reporter'),
                        reporterOutput: 'report-jshint-checkstyle.xml'
                },
                files: ['Gruntfile.js', '<%= baseurl %>/modules/web/src/main/webapp/v2/js/*.js','<%= baseurl %>/modules/web/src/main/webapp/v2/js/components/**/*.js','<%= baseurl %>/modules/web/src/main/webapp/v2/js/utils/*.js']
        },
        watch: {
            files: ['**/src/**/*'],
            tasks: ['development']
        }
    });

    grunt.loadNpmTasks('grunt-html2js');
    grunt.loadNpmTasks('grunt-contrib-rename');
    grunt.loadNpmTasks('grunt-contrib-copy');
    grunt.loadNpmTasks('grunt-env');
    grunt.loadNpmTasks('grunt-properties-to-json');
    grunt.loadNpmTasks('grunt-ng-annotate');
    grunt.loadNpmTasks('grunt-contrib-uglify-es');
    grunt.loadNpmTasks('grunt-contrib-clean');
    grunt.loadNpmTasks('grunt-string-replace');
    grunt.loadNpmTasks('grunt-preprocess');
    grunt.loadNpmTasks('grunt-contrib-cssmin');
    grunt.loadNpmTasks('grunt-comment-toggler');
    grunt.loadNpmTasks('grunt-if');
    grunt.loadNpmTasks('json-string-replace');
    grunt.loadNpmTasks('grunt-contrib-jshint');
    grunt.loadNpmTasks('grunt-shell');
    grunt.loadNpmTasks('grunt-contrib-watch');


    grunt.registerTask('default', ['copy:main', 'copy:properties', 'rename:en', 'rename:fr', 'rename:hi','propertiesToJSON:en',
        'propertiesToJSON:fr','propertiesToJSON:hi','shell:convertUTF8','copy:res2src','copy:rb2src']);
    grunt.registerTask('development',['clean:dist', 'env:dev', 'copy:src','copy:main','copy:properties','if:default',
        'rename:en', 'rename:fr', 'rename:hi', 'propertiesToJSON:en', 'propertiesToJSON:fr', 'propertiesToJSON:hi', 'shell:convertUTF8', 'if:rb',
        'rename:resource', 'cssmin:prod', 'copy:bulletinboard', 'copy:bulletinboardlist','copy:bulletinboardApp', 'preprocess:beforeUglify',
        'env:bulletinboard', 'preprocess:bulletinboardUglify', 'env:dev', 'toggleComments', 'html2js', 'ngAnnotate',
        'string-replace:dist', 'uglify', 'preprocess:afterUglify', 'env:bulletinboard', 'preprocess:bulletinboardAfterUglify']);
    grunt.registerTask('production',['clean:dist', 'env:prod', 'copy:src','copy:main','copy:properties', 'if:default',
        'rename:en', 'rename:fr', 'rename:hi', 'propertiesToJSON:en', 'propertiesToJSON:fr', 'propertiesToJSON:hi', 'shell:convertUTF8', 'if:rb',
        'rename:resource', 'cssmin:prod', 'copy:bulletinboard','copy:bulletinboardlist','copy:bulletinboardApp', 'preprocess:beforeUglify',
        'env:bulletinboard', 'preprocess:bulletinboardUglify', 'env:prod', 'toggleComments', 'html2js', 'ngAnnotate',
        'string-replace:dist', 'uglify', 'preprocess:afterUglify', 'env:bulletinboard', 'preprocess:bulletinboardAfterUglify']);

    function rename(moduleName) {
        return moduleName.replace('../modules/web/src/main/webapp/v2/', '');
    }
};
