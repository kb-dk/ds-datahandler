# Changelog
All notable changes to ds-datahandler will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]


### Added


## [1.5.0](https://github.com/kb-dk/ds-datahandler/releases/tag/v1.4.0) - 2023-12-05
- Pvica authentication now also uses basic authentication using headers. The callback authentication is still active, but will not be called from pvica6
- Use FormatDTO for SOLRJSON format when calling ds-present
- Preliminary support for token based user group resolving, preparing for OAuth2
- logback template changes

## [1.4.0](https://github.com/kb-dk/ds-datahandler/releases/tag/v1.4.0) - 2023-12-05
### Added
- Solr indexing using ds-present streaming client. New parameter 'mTimeFrom' to pick only records modified after this.

### Changed 
- General style of YAML configuration files, by removing the first level of indentation.



## [1.3.0](https://github.com/kb-dk/ds-datahandler/releases/tag/v1.3.0) - 2023-09-05
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
