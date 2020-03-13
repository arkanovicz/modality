<p align="center">
  <img src="https://raw.githubusercontent.com/arkanovicz/modality/master/src/site/modality.png" title="Modality">
</p>

**Modality** is a lightweight but hightly configurable Java ORM, with a companion set of tools 

# Modality ORM library

Encompases a data access layer, a hierarchical and reentrant model definition gathering SQL queries, database values and names custom filters and much more. See [modality-core's REAME](modality-core).

# Mocality Web anti-framework

While the ORM is usable on its own or within other Java/Kotlin web frameworks, the project comes with a companion set of tools which stick to the J2EE and JDBC standards to provide a lightweight, loosely coupled and highly cusomizable [MVC](https://en.wikipedia.org/wiki/Model%E2%80%93view%E2%80%93controller) [pull](https://en.wikipedia.org/wiki/Web_framework#Push-based_vs._pull-based) architecture based on the Java J2EE servlets API.

Its design is based on a [bottom-up](https://en.wikipedia.org/wiki/Top-down_and_bottom-up_design) approach rather than top-down. This makes Modality an *anti-framework*: its various components do stick to the underlying norms, have a minimal interdependance and just do the specific task they are intended to, leveraging the learning curve and added complexity, and nevertheless those components assembly covers all the basic needs of a fully functional MVC webapp.

+ the Model layer is addressed with a generic ORM Java API witouth any code generation and with minimal optional caching, providing a complete database structure reverse enginering feature.
+ the View layer is Apache Velocity, but other Java View layer technologies can easily be used instead.
+ the Control layer is decentralized in URI to path conventions (like serving *basedir*`/foo.vhtml` for `GET /foo.vhtml`), configuration files and helpers. It can easily be blended in existing frameworks.

Components:

+ [modality-api-client](modality-api-client) - API client utility for JSON/XML APIs.
+ [modality-api-server](modality-api-server) - API servlet serving Velocity JSON templates.
+ [modality-webapp](modality-webapp) - servlets and filters for Modality-core configuration and initialization in a J2EE webapp environment.
+ [modality-webapp-auth](modality-webapp-auth) - authentication filters assortment.
+ [velocity-tools-apiclient](velocity-tools-apiclient) - view layer API client tool which encapsulates modality-api-client for use with Apache Velocity Tools.
+ [velocity-tools-model](velocity-tools-model) - view layer model tool which encapsulates modality-core model objects for use with Apache Velocity Tools.
+ [modality-webapp-oauth-server](modality-webapp-oauth-server) - OAuth2 server (work in progress)
+ [modality-webapp-oauth-client](modality-webapp-oauth-server) - OAuth2 client (work in progress)

The flagship module, [modality-core](modality-core), is a full fledged ORM, mature and usable on its own. It's a complete 2019' rewrite of the [Velosurf](https://github.com/arkanovicz/velosurf) library, which is a Velocity-specific ORM (which I maintained for 18 years). The Velocity specific stuff now belongs to the `velocity-model-tool` package.

Release 1.0 doesn't publish any `modality-webapp-oauth-*`, nor the `modality-api-server` and `velocity-tools-apiclient` modules, which aren't ready.

Modules `modality-webapp` and `modality-webapp-auth` have been published with v1.0 because they are needed by `modality-example-bookshelf`, but shall undergo some refactoring. 

# Usage

## Inclusion in a maven project

Include the needed module(s) in your `pom.xml`'s dependencies section as follow:

      <dependency>
          <groupId>com.republicate.modality</groupId>
          <artifactId>modality-core</artifactId>
          <version>1.0</version>
      </dependency>


