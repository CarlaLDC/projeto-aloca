// Karma do front: igual ao padrão do Angular, com cobertura também em lcov para o SonarCloud.
module.exports = function (config) {
  config.set({
    basePath: '',
    frameworks: ['jasmine'],
    plugins: [
      require('karma-jasmine'),
      require('karma-chrome-launcher'),
      require('karma-jasmine-html-reporter'),
      require('karma-coverage'),
    ],
    client: {
      jasmine: {},
    },
    jasmineHtmlReporter: {
      suppressAll: true,
    },
    coverageReporter: {
      dir: require('path').join(__dirname, './coverage/projeto-aloca'),
      subdir: '.',
      reporters: [{ type: 'html' }, { type: 'text-summary' }, { type: 'lcovonly' }],
    },
    reporters: ['progress', 'kjhtml'],
    browsers: ['Chrome'],
    // No CI o Chrome pode demorar a abrir (máquina ocupada): mais tempo e tolerância a uma reconexão.
    captureTimeout: 180000,
    browserNoActivityTimeout: 120000,
    browserDisconnectTimeout: 30000,
    browserDisconnectTolerance: 2,
    restartOnFileChange: true,
  });
};
