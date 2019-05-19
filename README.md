# Modality

Modality is an anti-framework: it's a set of tools which stick to the HTTP standard to provide a lightweight [MVC](https://en.wikipedia.org/wiki/Model%E2%80%93view%E2%80%93controller) architecture based on the Java J2EE servlets API.

+ the Model layer is addressed with a generic ORM Java API witouth any code generation and with minimal optional caching.
+ the View layer is Apache Velocity, but other view Java technologies can easily be plugged in.
+ there is no centralized Controller, but rather a set of conventions and helpers.

Child modules:

+ [modality-core](tree/master/modality-core) - a model layer for a [pull MVC architecture](https://en.wikipedia.org/wiki/Web_framework#Push-based_vs._pull-based)
+ [modality-api-client](tree/master/modality-api-client) - API client utility for JSON APIs
+ [modality-api-server](tree/master/modality-api-server) - API servlet serving Velocity JSON templates
+ [modality-webapp](tree/master/modality-webapp) - servlets and filters for Modality database configuration and initialization in a J2EE webapp environment
+ [modality-webapp-auth](tree/master/modality-webapp-auth) - authentication filters assortment
+ [velocity-tools-apiclient](tree/master/velocity-tools-apiclient) - view layer API client tool
+ [velocity-tools-model](tree/master/velocity-tools-model) - view layer model tool
+ [modality-webapp-oauth-server](tree/master/modality-webapp-oauth-server) - OAuth2 server (work in progress)
+ [modality-webapp-oauth-client](tree/master/modality-webapp-oauth-server) - OAuth2 client (work in progress)

