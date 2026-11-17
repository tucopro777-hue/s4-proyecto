// Chrome sin GPU para pruebas en equipos Windows y entornos sin escritorio.
module.exports = function (config) {
  config.set({
    frameworks: ['jasmine', '@angular-devkit/build-angular'],
    plugins: [require('karma-jasmine'), require('karma-chrome-launcher'), require('@angular-devkit/build-angular/plugins/karma')],
    reporters: ['progress'],
    browsers: ['ChromeHeadlessSinGpu'],
    customLaunchers: {
      ChromeHeadlessSinGpu: {
        base: 'ChromeHeadless',
        flags: ['--disable-gpu', '--in-process-gpu', '--disable-software-rasterizer', '--no-sandbox'],
      },
    },
    singleRun: true,
    restartOnFileChange: false,
  });
};
