// Lanzador para Chrome en el entorno local restringido de Windows.
module.exports = function (config) {
  config.set({
    frameworks: ['jasmine', '@angular-devkit/build-angular'],
    plugins: [require('karma-jasmine'), require('karma-chrome-launcher'),
      require('@angular-devkit/build-angular/plugins/karma')],
    reporters: ['progress'],
    browsers: ['ChromeLocal'],
    customLaunchers: {
      ChromeLocal: {base: 'ChromeHeadless', flags: ['--no-sandbox', '--disable-gpu', '--disable-software-rasterizer']},
    },
    singleRun: true,
  });
};
