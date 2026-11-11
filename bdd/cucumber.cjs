module.exports = {
  default: {
    paths: ['features/**/*.feature'],
    require: ['support/**/*.cjs', 'steps/**/*.cjs'],
    format: ['progress', 'html:reports/cucumber.html', 'json:reports/cucumber.json']
  }
};
