# Changelog

All notable changes to ds-datahandler will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

### Changed

- Moved JobCache from memory to database (*Remember: OPS need to create jobs table in database, the ddl is found in
  /src/main/resources/ddl/create_ds_datahandler_db.ddl*)
- KalturaDeltaUploadJob will now use kaltura.adminSecret if present.

### Fixed

## [3.0.3](https://github.com/kb-dk/ds-datahandler/releases/tag/ds-datahandler-3.0.3) - 2025-09-01

### Changed

- Bumped ds-kaltura to version 3.0.2. Handling new Exception and benefit from changes in kaltura
- Bumped ds-present to version 3.0.1 to benefit from changes in that module
- Bumped ds-storage to version 3.0.1 to benefit from changes in that module

## [3.0.2](https://github.com/kb-dk/ds-datahandler/releases/tag/ds-datahandler-3.0.2) - 2025-07-07

### Changed

- Bumped ds-kalturaclient to version 3.0.1. Added new default values for kaltura session refresh/timeout in
  ds-datahandler-behaviour.yaml

## [3.0.1](https://github.com/kb-dk/ds-datahandler/releases/tag/ds-datahandler-3.0.1) - 2025-06-24

### Fixed

- For Kaltura upload check on storage record if it already has a kaltura id, since there is a delay in kaltura from when
  records are searchable

## [3.0.0](https://github.com/kb-dk/ds-datahandler/releases/tag/ds-datahandler-3.0.0) - 2025-06-12

### Added

- New service method '/kaltura/deltaupload' that will upload new streams to Kaltura. The method will also update storage
  records with kaltura entryId after each upload. When all uploads are completed it will start a solr delta-index job.
  Errors will streams will be marked with StreamErrorTypeDto enum values. License module must be updated so both missing
  kalturaId's and id's that start with 'ERROR_' are filtered away.

### Changed

- Refactored OAI Response filters, to remove duplicate code from Preservica and DR filter
- Make OAI Response filters parse XML correctly through the SAX API.
- Extract id of presentation copy from XML and use it as referenceID before saving object.

### Fixed

## [1.11.0](https://github.com/kb-dk/ds-datahandler/releases/tag/ds-datahandler-1.11.0) - 2025-03-05

### Changed

- removed generation of java-client API since datahandler is not supposed to be called from other modules.
- make all loggers static
- Only build suggest index if there was new documents indexed.
- Bumped SwaggerUI dependency to v5.18.2
- Bumped multiple OpenAPI dependency versions
- Bumped kb-util to v1.6.9 for service2service oauth support.

### Fixed

- Fixed inclusion of the same dependencies from multiple sources.
- Fixed /api-docs wrongly showing petstore example API spec

## [1.10.4](https://github.com/kb-dk/ds-datahandler/releases/tag/ds-datahandler-1.10.4) - 2025-01-07

### Changed

- make all loggers static
- Upgraded dependency cxf-rt-transports-http to v.3.6.4 (fix memory leak)

## [1.10.3](https://github.com/kb-dk/ds-datahandler/releases/tag/ds-datahandler-1.10.3) - 2024-10-28

- All 4 jobs types (OAI,Manifestation,Kaltura entry id, solr index) now registers as a job in the job cache.
  Only one job can be running at the same time for the type and origin. The job status method will
  return all running and completed jobs since restart.
- Fixed solr indexing job start failure.

## [1.10.2](https://github.com/kb-dk/ds-datahandler/releases/tag/ds-datahandler-1.10.2) - 2024-10-28

### Added

- Added better logging of what actually happens during enrichment methods.
- Added status logging in Kaltura ID enrichment method. Now the method logs a short status for every 500 records
  processed.

### Removed

- Removed check for byte availability in inputstreams as it can't be trusted for responses from Preservica.

## [1.10.1](https://github.com/kb-dk/ds-datahandler/releases/tag/ds-datahandler-1.10.1) - 2024-10-15

### Changed

- Update refreshing of Preservica access token

## [1.10.0](https://github.com/kb-dk/ds-datahandler/releases/tag/ds-datahandler-1.10.0) - 2024-10-15

### Added

- Upgraded ds-kaltura to v.1.2.5
- Kaltura client uses kaltura app-tokens instead of mastersecret. Two new properties added: kaltura.token and
  katura.tokenId. Mastersecret can be set to null in property

## [1.9.7](https://github.com/kb-dk/ds-datahandler/releases/tag/ds-datahandler-1.9.7) - 2024-10-03

### Added

- OAI harvest can now have wait times configured in the config files.

### Fixed

- OAI jobs will also be marked as stopped if getting APIException  (dk.kb class)

## [1.9.6](https://github.com/kb-dk/ds-datahandler/releases/tag/ds-datahandler-1.9.6) - 2024-09-23

### Added

- Added retry mechanism for `/enrichment/manifestation` endpoint.

## [1.9.5](https://github.com/kb-dk/ds-datahandler/releases/tag/ds-datahandler-1.9.5) - 2024-09-17

### Changed

- Enrichment with access copy IDs now always happens in parallel with 5 threads.

## [1.9.4](https://github.com/kb-dk/ds-datahandler/releases/tag/ds-datahandler-1.9.4) - 2024-09-16

### Changed

- When origin cannot be resolved for a record, it is not added to storage as this should not happen.
- Changed preservica origin resolving to ignore casing of ENUMS: `Moving Image` and `Sound`.

## [1.9.3](https://github.com/kb-dk/ds-datahandler/releases/tag/ds-datahandler-1.9.3) - 2024-09-13

### Changed

- Update OAI harvester to retry when response code is not 200.

## [1.9.2](https://github.com/kb-dk/ds-datahandler/releases/tag/ds-datahandler-1.9.2) - 2024-09-13

### Fixed

- Fixed endless loop in OAI-PMH harvester.

## [1.9.1](https://github.com/kb-dk/ds-datahandler/releases/tag/ds-datahandler-1.9.1) - 2024-09-13

### Added

- Added an automatic retry, when the OAI-PMH harvester encounters http 401s, as Preservica sometimes throws these when
  tickled enough.

## [1.9.0](https://github.com/kb-dk/ds-datahandler/releases/tag/ds-datahandler-1.9.0) - 2024-09-12

### Added

- Enabled OAuth2 on module. Much is copy-paste from ds-image to see it working in two different modules. Plans are to
  refactor common functionality out into kb-util/template projects.
  No methods are defined to require OAuth yet!
- After finished solr indexing the `/solr/index`-endpoint now updates the index for the suggest component as well.
- Added the ability to enrich metadata with extra fragments from a seperate webservice when harvesting records from
  preservica
- handle errors when calling fragment service
- Added manual batchjob to generate Kaltura XML for bulk uploads.

### Changed

- Changed when DsPreservicaClient is initialized.
- Bump ds-kaltura to 1.2.3
- Updated solrJ to 9.6.1 to remove deprecated `HttpSolrClient` and switch to `HttpJdkSolrClient` capable of using Http 1
  and 2.
- Removed solr.updateUrl from configuration files. Use the added solr.update.url instead.

### Removed

- Removed non-resolvable git.tag from build.properties
- Removed double logging of part of the URL by bumping kb util to v1.5.10

## [1.8.2](https://github.com/kb-dk/ds-datahandler/releases/tag/ds-datahandler-1.8.2) - 2024-07-17

### Changed

- Changed DSStorage client method used in methods DsDatahandlerFacade.updateManifestationForRecords and
  DsDatahandlerFacade.fetchKalturaIdsAndUpdateRecords.
- Bumped ds-storage dependency to v. 2.1.1
- Bumped ds-storage dependency to v. 2.1.1
- Bumped ds-present dependency to v 2.0.0
- Bumped ds-kaltura dependency to 1.2.2

## [1.8.1](https://github.com/kb-dk/ds-datahandler/releases/tag/ds-datahandler-1.8.1) - 2024-07-01

### Changed

- Update dependency ds-storage to version 2.0.0
- Update dependency ds-present to version 1.9.0
- Update dependency ds-kaltura to version 1.2.1

## [1.8.0](https://github.com/kb-dk/ds-datahandler/releases/tag/ds-datahandler-1.8.0) - 2024-07-01

### Added

- Added a DR filter for preservica data as a selectable preservica 7 filter.

### Changed

- Changed how origin gets extracted from records to allow for xml namespace prefixes on the field being used for this
  filtering for Preservica records.
- Kaltura client implementation. Using ds-kaltura client as dependency
  instead.https://kb-dk.atlassian.net/browse/DRA-964
- Bumped KB-util version
- Updated endpoint ```enrichment/manifestations``` to check if records without ContentObjects are migrated from DOMS. If
  so, referenceId should be the same as the InformationObject ID.

### Removed

- Removed Preservica 5 data filter.

## [1.7.3](https://github.com/kb-dk/ds-datahandler/releases/tag/ds-datahandler-1.7.3) - 2024-05-28

###  

- Changed how to enrich preservica 7 records with manifestations. [DRA-685](https://kb-dk.atlassian.net/browse/DRA-685)

## [1.7.2](https://github.com/kb-dk/ds-datahandler/releases/tag/ds-datahandler-1.7.2) - 2024-05-27

###

- Added endpoint for enriching preservica 7 records with manifestations extracted through the preservica 7 REST
  API. [DRA-500](https://kb-dk.atlassian.net/browse/DRA-503)

## [1.7.1](https://github.com/kb-dk/ds-datahandler/releases/tag/ds-datahandler-1.7.1) - 2024-05-14

### Added

- Added support for dynamically updating the OpenAPI specification with config values.
- Added individual OaiResponseFilters for preservica 5 and preservica
  7. [DRA-400](https://kb-dk.atlassian.net/browse/DRA-400)
- Added sample config files and documentation to distribution tar
  archive. [DRA-422](https://kb-dk.atlassian.net/browse/DRA-422)

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

- Added git information to the status endpoint. It now delivers, deployed branch name, commit hash, time of latest
  commit and closest tag
- Added support for referencing config values in OpenAPI specification through the maven plugin:
  _injection-maven-plugin_
- OAI harvest can be configured to use day batching [from,until] instead of open interval [from,-]
- Renamed attributes names in OAI-target. (do not think it other modules uses these attributes).

## [1.5.0](https://github.com/kb-dk/ds-datahandler/releases/tag/ds-datahandler-1.5) - 2024-01-22

### Added

- Pvica authentication now also uses basic authentication using headers. The callback authentication is still active,
  but will not be called from pvica6
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
