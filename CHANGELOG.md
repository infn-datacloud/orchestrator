# Changelog

Notable changes to the INDIGO PaaS Orchestrator.

Recent entries are based on the [published GitHub releases](https://github.com/infn-datacloud/orchestrator/releases), with links to the corresponding pull requests and comparisons. Dates are release publication dates (UTC). Release candidate tags are retained as published.

## Unreleased

No unreleased changes are documented yet.

## [v4.3.1](https://github.com/infn-datacloud/orchestrator/releases/tag/v4.3.1) - 2025-10-01

### Fixed

- Fix IAM URL path composition. ([#78](https://github.com/infn-datacloud/orchestrator/pull/78))

[Full comparison](https://github.com/infn-datacloud/orchestrator/compare/v4.3.0...v4.3.1)

## [v4.3.0](https://github.com/infn-datacloud/orchestrator/releases/tag/v4.3.0) - 2025-09-29

### Added

- Integrate the AI Ranker. ([#64](https://github.com/infn-datacloud/orchestrator/pull/64))

[Full comparison](https://github.com/infn-datacloud/orchestrator/compare/v4.2.2...v4.3.0)

## [v4.2.2](https://github.com/infn-datacloud/orchestrator/releases/tag/v4.2.2) - 2025-09-26

### Changed

- Adjust logging levels. ([#76](https://github.com/infn-datacloud/orchestrator/pull/76))

[Full comparison](https://github.com/infn-datacloud/orchestrator/compare/v4.2.1...v4.2.2)

## [v4.2.1](https://github.com/infn-datacloud/orchestrator/releases/tag/v4.2.1) - 2025-06-23

### Fixed

- Update the Dockerfile to support the updated Java version. ([#67](https://github.com/infn-datacloud/orchestrator/pull/67))

[Full comparison](https://github.com/infn-datacloud/orchestrator/compare/v4.2.0...v4.2.1)

## [v4.2.0](https://github.com/infn-datacloud/orchestrator/releases/tag/v4.2.0) - 2025-04-29

### Added

- Allow `getDeployments()` to accept a list of deployment states to exclude (CLOUD-2719). ([#60](https://github.com/infn-datacloud/orchestrator/pull/60))

### Fixed

- Fix Federation Registry DTO issues. ([#61](https://github.com/infn-datacloud/orchestrator/pull/61))

[Full comparison](https://github.com/infn-datacloud/orchestrator/compare/v4.0.1...v4.2.0)

## [v4.2.0-RC1](https://github.com/infn-datacloud/orchestrator/releases/tag/v4.2.0-RC1) - 2025-04-15

Release candidate.

### Added

- Allow `getDeployments()` to accept a list of deployment states to exclude (CLOUD-2719). ([#60](https://github.com/infn-datacloud/orchestrator/pull/60))

### Fixed

- Fix Federation Registry DTO issues. ([#61](https://github.com/infn-datacloud/orchestrator/pull/61))

[Full comparison](https://github.com/infn-datacloud/orchestrator/compare/v4.0.1...v4.2.0-RC1)

## [v4.0.1](https://github.com/infn-datacloud/orchestrator/releases/tag/v4.0.1) - 2025-01-31

### Fixed

- Fix handling of the `privateNetworkProxyUser` parameter in `ComputeService`. ([#55](https://github.com/infn-datacloud/orchestrator/pull/55))

[Full comparison](https://github.com/infn-datacloud/orchestrator/compare/v4.0.0...v4.0.1)

## [v4.0.0](https://github.com/infn-datacloud/orchestrator/releases/tag/v4.0.0) - 2025-01-28

### Added

- Integrate the Federation Registry service. ([#54](https://github.com/infn-datacloud/orchestrator/pull/54))

### Changed

- Update Maven dependency repositories to use `cnaf-sd-nexus-repository`. ([#44](https://github.com/infn-datacloud/orchestrator/pull/44))
- Add diagnostic logging. ([#17](https://github.com/infn-datacloud/orchestrator/pull/17))

### Fixed

- Fix IAM client and S3 bucket creation when deployment fails on a provider. ([#28](https://github.com/infn-datacloud/orchestrator/pull/28))

[Full comparison](https://github.com/infn-datacloud/orchestrator/compare/v3.0.1-FINAL...v4.0.0)

## [v4.0.0-RC.2](https://github.com/infn-datacloud/orchestrator/releases/tag/v4.0.0-RC.2) - 2025-01-17

Release candidate.

### Fixed

- Fix the `getCMDBDataUpdate` function. ([#52](https://github.com/infn-datacloud/orchestrator/pull/52))

[Full comparison](https://github.com/infn-datacloud/orchestrator/compare/v4.0.0-RC.1...v4.0.0-RC.2)

## [v4.0.0-RC.1](https://github.com/infn-datacloud/orchestrator/releases/tag/v4.0.0-RC.1) - 2025-01-09

Release candidate.

### Added

- Integrate the Federation Registry service. ([#25](https://github.com/infn-datacloud/orchestrator/pull/25))

### Changed

- Update Maven dependency repositories to use `cnaf-sd-nexus-repository`. ([#44](https://github.com/infn-datacloud/orchestrator/pull/44))
- Add diagnostic logging. ([#17](https://github.com/infn-datacloud/orchestrator/pull/17))

### Fixed

- Fix IAM client and S3 bucket creation when deployment fails on a provider. ([#28](https://github.com/infn-datacloud/orchestrator/pull/28))

[Full comparison](https://github.com/infn-datacloud/orchestrator/compare/v3.0.1-FINAL...v4.0.0-RC.1)

## [v3.0.1-FINAL](https://github.com/infn-datacloud/orchestrator/releases/tag/v3.0.1-FINAL) - 2025-01-09

### Fixed

- Fix IAM client and S3 bucket creation.

[Full comparison](https://github.com/infn-datacloud/orchestrator/compare/v3.0.0-FINAL...v3.0.1-FINAL)

## [v3.0.0-FINAL](https://github.com/infn-datacloud/orchestrator/releases/tag/v3.0.0-FINAL) - 2024-08-19

### Added

- Manage IAM client creation and deletion ([CLOUD-1774](https://issues.infn.it/jira/browse/CLOUD-1774)).
- Manage S3 bucket creation and deletion ([CLOUD-1521](https://issues.infn.it/jira/browse/CLOUD-1521)).
- Add a `force` parameter to the deployment DELETE API and forward it to the Infrastructure Manager ([CLOUD-2119](https://issues.infn.it/jira/browse/CLOUD-2119), [CLOUD-2059](https://issues.infn.it/jira/browse/CLOUD-2059), [im-java-api#73](https://github.com/indigo-dc/im-java-api/pull/73)). **Forced deletion may leave resources behind, including IAM clients and S3 buckets.**
- Add group-based authorization to reject requests for groups the user is not allowed to use.
- Add the `PAAS_DEP_USER` deployment metadata using the `preferred_username` claim from the user’s token ([CLOUD-2135](https://issues.infn.it/jira/browse/CLOUD-2135)).

### Changed

- Skip monitoring information retrieval when no monitoring URL is configured.

### Removed

- Remove the user organization check from request authorization ([CLOUD-1773](https://issues.infn.it/jira/browse/CLOUD-1773)).

---

## Legacy history

The entries below are preserved from the existing changelog. The repository’s published releases start at `v3.0.0-FINAL`; releases between `v1.4.0` and `v3.0.0-FINAL` are not documented here.

Legacy notation: **[Feature]** identifies a user-facing or API change; **[Bug]** identifies a bug fix. Bare issue numbers refer to the original issue tracking system.

## [v1.4.0] - 2017-07-31

### Added:
- Support for deployment of hybrid clusters (#231)
- [Docker] Validate DB connection before starting wildfly (#230)
- Add information of who created the deployments in REST response (#229)
- Allow filtering of deployment though param in REST APIs (#222)

### Fixed:
- Fix IM header generation for IAM federated OpenStacks (#232)

## [v1.3.0] - 2017-04-10

### Added:
- IAM token exchange and refresh (#96)
- Selection of deployment sites through TOSCA SLA policies (#174)
- Support the deployment on AWS (#196)
- Send the Clues client credentials through TOSCA templates (#172)

### Fixed:
- Fix the deploy never timing out (#186)
- Fix TOSCA interfaces not sent to IaaS deployers (#181)

## [v1.2.2] - 2017-03-27

### Added:
- SLAM REST endpoint authentication (#189)

## [v1.2.1] - 2016-12-23

### Added:
- Check access token expiration date and signature (#157)

### Changed:
- Update default monitoring endpoint (#158)
- Update custom INDIGO TOSCA types (#161)
- Improve DB connection handling during shutdown (#163)

## [v1.2.0] - 2016-10-21

### Added:
- Support input substitution in listValue properties (#142)
- Support TOSCA extended node requirements definition in topology templates (#139)

### Removed:
- Removed deprecated occi proxy authentication (#125)

## [v1.1.0] - 2016-09-30

### Added:
- Support `iam_access_token` property in `tosca.nodes.indigo.ElasticCluster` nodes (#109)

### Changed:
- Adapt user info data to the new IAM format (#104)
- Make error reason message in REST response more clear (#119)
- Make the requirement of `openid` scope in auth token explicitly mandatory (#81)
- Sort in reverse-chronological order deployments and resources when retrieved from REST APIs (#101)

### Fixed:
- Support multiple SLAs for the same cloud provider (#110)

## [v1.0.0] - 2016-08-03

### Added:
- **[Feature]** Image ID substitution in TOSCA template (to support multiple CP) (#53)
- **[Feature]** Use selected Cloud Provider for deploy/update/undeploy (#51)
- **[Feature]** Implement AAI support and IM authentication relay (#37)
- **[Feature]** Job Submission from Tosca to Chronos/Mesos (#34)
- **[Feature]** Ranking of the resources via Cloud Provider Ranker (#47)
- **[Feature]** Obtain the information about the monitoring of the IaaS resources (#46)
- **[Feature]** Support for Configuration Database for the IaaS Resources (#45)
- Enable TOSCA user's inputs substitution
- Cloud Provider choice (SLAM, CMDB, Monitoring, CPR integration)
- Retrieve Provider's Service's Image list from CMDB
- Jobs with Parameter Sweep (up to 10k jobs)
- Enable 'force_pull_image' flag in Chronos jobs by default (#70)
- Support Privileged mode for containers in Chronos jobs (#49)
- **[Feature]** Support for the Data Location Scheduling (OneData) (#55)

### Changed:
- Removed OneDock-specific authentication

### Fixed:
- Cannot delete Chronos job if the TOSCA template has some errors (#71)
- TOSCA: required inputs with default value not handled correctly (#73)
- Provider choice override for Chronos single provider (#77)
- Image ID substitution is not done during deployment Update (#86)
- Chronos properties are not handled properly (#84)






# Change Log
All notable changes to this project will be documented in this file.
This project adheres to [Semantic Versioning](http://semver.org/).

---

**Please view this file on the `master` branch; on stable branches it's out of date.**

## Legend:
- **[Feature]** indicates that an interface feature (a user level or an API level one) has changed. Changes not tagged with this indicate an internal code change, that should not affect the external system behavior and hence *should be ignored during Acceptance Testing*.
- **[Bug]** indicates a fixed bug.
- `#<number>` is a link to an issue, `!<number>` is a link to a merge request in the internal issue system.

---
## [v1.4.0] - 2017-07-31

### Added:
- Support for deployment of hybrid clusters (#231)
- [Docker] Validate DB connection before starting wildfly (#230)
- Add information of who created the deployments in REST response (#229)
- Allow filtering of deployment though param in REST APIs (#222)

### Changed:
**NONE**

### Deprecated:
**NONE**

### Removed:
**NONE**

### Fixed:
- Fix IM header generation for IAM federated OpenStacks (#232)

### Security:
**NONE**



## [v1.3.0] - 2017-04-10

### Added:
- IAM token exchange and refresh (#96)
- Selection of deployment sites through TOSCA SLA policies (#174)
- Support the deployment on AWS (#196)
- Send the Clues client credentials through TOSCA templates (#172)

### Changed:
**NONE**

### Deprecated:
**NONE**

### Removed:
**NONE**

### Fixed:
- Fix the deploy never timing out (#186)
- Fix TOSCA interfaces not sent to IaaS deployers (#181)

### Security:
**NONE**




## [v1.2.2] - 2017-03-27

### Added:
- SLAM REST endpoint authentication (#189)

### Changed:
**NONE**

### Deprecated:
**NONE**

### Removed:
**NONE**

### Fixed:
**NONE**

### Security:
**NONE**




## [v1.2.1] - 2016-12-23

### Added:
- Check access token expiration date and signature (#157)

### Changed:
- Update default monitoring endpoint (#158)
- Update custom INDIGO TOSCA types (#161)
- Improve DB connection handling during shutdown (#163)

### Deprecated:
**NONE**

### Removed:
**NONE**

### Fixed:
**NONE**

### Security:
**NONE**



## [v1.2.0] - 2016-10-21

### Added:
- Support input substitution in listValue properties (#142)
- Support TOSCA extended node requirements definition in topology templates (#139)

### Changed:
**NONE**

### Deprecated:
**NONE**

### Removed:
- Removed deprecated occi proxy authentication (#125)

### Fixed:
**NONE**

### Security:
**NONE**



## [v1.1.0] - 2016-09-30

### Added:
- Support `iam_access_token` property in `tosca.nodes.indigo.ElasticCluster` nodes (#109)

### Changed:
- Adapt user info data to the new IAM format (#104)
- Make error reason message in REST response more clear (#119)
- Make the requirement of `openid` scope in auth token explicitly mandatory (#81)
- Sort in reverse-chronological order deployments and resources when retrieved from REST APIs (#101)

### Deprecated:
**NONE**

### Removed:
**NONE**

### Fixed:
- Support multiple SLAs for the same cloud provider (#110)

### Security:
**NONE**



## [v1.0.0] - 2016-08-03

### Added:
- **[Feature]** Image ID substitution in TOSCA template (to support multiple CP) (#53)
- **[Feature]** Use selected Cloud Provider for deploy/update/undeploy (#51)
- **[Feature]** Implement AAI support and IM authentication relay (#37)
- **[Feature]** Job Submission from Tosca to Chronos/Mesos (#34)
- **[Feature]** Ranking of the resources via Cloud Provider Ranker (#47)
- **[Feature]** Obtain the information about the monitoring of the IaaS resources (#46)
- **[Feature]** Support for Configuration Database for the IaaS Resources (#45)
- Enable TOSCA user's inputs substitution
- Cloud Provider choice (SLAM, CMDB, Monitoring, CPR integration)
- Retrieve Provider's Service's Image list from CMDB
- Jobs with Parameter Sweep (up to 10k jobs)
- Enable 'force_pull_image' flag in Chronos jobs by default (#70)
- Support Privileged mode for containers in Chronos jobs (#49)
- **[Feature]** Support for the Data Location Scheduling (OneData) (#55)

### Changed:
- Removed OneDock-specific authentication

### Deprecated:
**NONE**

### Removed:
**NONE**

### Fixed:
- Cannot delete Chronos job if the TOSCA template has some errors (#71)
- TOSCA: required inputs with default value not handled correctly (#73)
- Provider choice override for Chronos single provider (#77)
- Image ID substitution is not done during deployment Update (#86)
- Chronos properties are not handled properly (#84)

### Security:
**NONE**



[v1.0.0 (Unreleased)]: ../../compare/0.0.5...HEAD
