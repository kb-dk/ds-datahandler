# Changelog
All notable changes to ds-datahandler will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]
### Added
- Solr indexing using ds-present streaming client. New parameter 'mTimeFrom' to pick only records modified after this.

### Changed 
- General style of YAML configuration files, by removing the first level of indentation.



## [1.3.0] - 2023-09-05
### Added

- Jetty port set explicitly to 9071 instead of default 8080 to avoid collisions
  with projects using default tomcat/jetty setup.
- Client generation
- Upgrade to ds-storage API v 1.3

## [1.0.0] - YYYY-MM-DD
### Added

- Initial release of <project>


[Unreleased](https://github.com/kb-dk/ds-datahandler/compare/v1.0.0...HEAD)
[1.0.0](https://github.com/kb-dk/ds-datahandler/releases/tag/v1.0.0)
