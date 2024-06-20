# Changelog
All notable changes to ds-datahandler will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]
### Changed
- Kaltura client implementation. Using ds-kaltura client as dependency instead.


### Added
- Added a DR filter for preservica data as a selectable preservica 7 filter. 

### Changed 
- Changed how origin gets extracted from records to allow for xml namespace prefixes on the field being used for this filtering for Preservica records. 

## [1.7.3](https://github.com/kb-dk/ds-datahandler/releases/tag/ds-datahandler-1.7.3) - 2024-05-28
### 
- Changed how to enrich preservica 7 records with manifestations. [DRA-685](https://kb-dk.atlassian.net/browse/DRA-685)

## [1.7.2](https://github.com/kb-dk/ds-datahandler/releases/tag/ds-datahandler-1.7.2) - 2024-05-27
###
- Added endpoint for enriching preservica 7 records with manifestations extracted through the preservica 7 REST API. [DRA-500](https://kb-dk.atlassian.net/browse/DRA-503)

## [1.7.1](https://github.com/kb-dk/ds-datahandler/releases/tag/ds-datahandler-1.7.1) - 2024-05-14
### Added 
- Added support for dynamically updating the OpenAPI specification with config values.
- Added individual OaiResponseFilters for preservica 5 and preservica 7. [DRA-400](https://kb-dk.atlassian.net/browse/DRA-400)
- Added sample config files and documentation to distribution tar archive. [DRA-422](https://kb-dk.atlassian.net/browse/DRA-422)

### Changed
- Changed parent POM for deployment to internal nexus [DTA-590]()https://kb-dk.atlassian.net/browse/DRA-591

### Removed
- Removed origin enum from openAPI specification.

### Fixed
- Correct resolving of maven build time in project properties. [DRA-413](https://kb-dk.atlassian.net/browse/DRA-413)



## [1.7.0](https://github.com/kb-dk/ds-datahandler/releases/tag/ds-datahandler-1.7) - 2024-03-05
### Added
- Moved OAI job list out into a new method '/monitor/jobs' It was part of the 'monitor/status' method before.
- Bumb sb-parent to v.25
- Added 'integration' tag to some unittests.

### Removed
- Removed origin enum from openAPI specification.

## [1.6.0](https://github.com/kb-dk/ds-datahandler/releases/tag/ds-datahandler-1.6) - 2024-02-20
- Added git information to the status endpoint. It now delivers, deployed branch name, commit hash, time of latest commit and closest tag
- Added support for referencing config values in OpenAPI specification through the maven plugin: _injection-maven-plugin_
- OAI harvest can be configured to use day batching [from,until] instead of open interval [from,-]
- Renamed attributes names in OAI-target. (do not think it other modules uses these attributes).

## [1.5.0](https://github.com/kb-dk/ds-datahandler/releases/tag/ds-datahandler-1.5) - 2024-01-22
### Added
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


[Unreleased](https: