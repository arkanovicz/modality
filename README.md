<p align="center">
  <img src="https://raw.githubusercontent.com/arkanovicz/modality/master/src/site/logo.svg" width="256" title="Modality">
</p>

Modality is a set of tools which stick to the J2EE and JDBC standards to provide a lightweight, loosely coupled and highly cusomizable [MVC](https://en.wikipedia.org/wiki/Model%E2%80%93view%E2%80%93controller) architecture based on the Java J2EE servlets API.

Its design is based on a [bottom-up](https://en.wikipedia.org/wiki/Top-down_and_bottom-up_design) approach rather than top-down. This makes Modality an *anti-framework*: its various components do stick to the underlying norms, have a minimal interdependance and just do the specific task they are intended to, leveraging the learning curve and added complexity, and nevertheless those components assembly covers all the basic needs of a fully functional MVC webapp.

+ the Model layer is addressed with a generic ORM Java API witouth any code generation and with minimal optional caching, providing a complete database structure reverse enginering feature.
+ the View layer is Apache Velocity, but other Java View layer technologies can easily be used instead.
+ there is no centralized Controller, but rather a set of configuration files (like `WEB-INF/web.xml`, the webapp application descriptor), conventions and helpers, plus any standalone controller implemented as a J2EE filter (like a form data validation one) can easily be added.

Child modules:

+ [modality-core](modality-core) - a model layer for a [pull MVC architecture](https://en.wikipedia.org/wiki/Web_framework#Push-based_vs._pull-based), which encompases a data access layer, a hierarchical and reentrant model definition gathering SQL queries, database values and names filters, ...
+ [modality-api-client](modality-api-client) - API client utility for JSON/XML APIs.
+ [modality-api-server](modality-api-server) - API servlet serving Velocity JSON templates.
+ [modality-webapp](modality-webapp) - servlets and filters for Modality-core configuration and initialization in a J2EE webapp environment.
+ [modality-webapp-auth](modality-webapp-auth) - authentication filters assortment.
+ [velocity-tools-apiclient](velocity-tools-apiclient) - view layer API client tool which encapsulates modality-api-client for use with Apache Velocity Tools.
+ [velocity-tools-model](velocity-tools-model) - view layer model tool which encapsulates modality-core model objects for use with Apache Velocity Tools.
+ [modality-webapp-oauth-server](modality-webapp-oauth-server) - OAuth2 server (work in progress)
+ [modality-webapp-oauth-client](modality-webapp-oauth-server) - OAuth2 client (work in progress)
