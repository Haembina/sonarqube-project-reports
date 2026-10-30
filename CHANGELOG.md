# Changelog

Written by `@haembina/release`, never by hand (rule D9).

## 0.3.1 (2026-10-30)

### Bug Fixes

* the download web service documents its response with an example report

## 0.3.0 (2026-10-30)

### Features

* list a report's sections first and pick its formats from tiles #14.1 #14.2
* download a report in several formats with one click

### Bug Fixes

* a checked format tile shows it is under the pointer too
* the Reports page's branch picker and section rows show they are under the pointer
* save the separate report files in the order their formats are listed
* fetch the separate report files together and save none when one fails
* update the libraries it is built on

## 0.2.1 (2026-10-28)

### Bug Fixes

* the Reports page scrolls in a window shorter than it
* the plugin's description names everything a report holds

## 0.2.0 (2026-10-28)

### Features

* link a report's repository and branch or pull request on its git provider and in SonarQube
* choose a report's sections and list each uncovered line and duplicated block
* mark each report section, metric and status with an icon
* download styled HTML, Markdown, PDF and EPUB reports

### Bug Fixes

* update the libraries it is built on

## [0.1.0](https://github.com/Haembina/sonarqube-project-reports/releases/tag/v0.1.0) (2026-10-28)

### Features

* download any branch or pull request report from a Reports page
* download a project, branch or pull request report as JSON

### Bug Fixes

* let SonarQube load the plugin
