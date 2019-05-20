# Modality

Modality is a set of tools which stick to the HTTP standard to provide a lightweight [MVC](https://en.wikipedia.org/wiki/Model%E2%80%93view%E2%80%93controller) architecture based on the Java J2EE servlets API.

Its design is based on a [bottom-up](https://en.wikipedia.org/wiki/Top-down_and_bottom-up_design) approach rather than top-down. This makes Modality an *anti-framework*: its various components do stick to the underlying  have a minimal interdependance between them and just do the specific task they are intended to, leveraging the learning curve and added complexity, and nevertheless it covers all the basic needs of a fully functional MVC webapp.

+ the Model layer is addressed with a generic ORM Java API witouth any code generation and with minimal optional caching, providing a complete database structure reverse enginering feature.
+ the View layer is Apache Velocity, but other Java View layer technologies can easily be used instead.
+ there is no centralized Controller, but rather a set of conventions, helpers and practices, and any standalone controller implemented as a J2EE filter (like a user provided data validation one) can easily be added.

Child modules:

+ [modality-core](https://github.com/arkanovicz/modality/tree/master/modality-core) - a model layer for a [pull MVC architecture](https://en.wikipedia.org/wiki/Web_framework#Push-based_vs._pull-based)
+ [modality-api-client](https://github.com/arkanovicz/modality/tree/master/modality-api-client) - API client utility for JSON/XML APIs
+ [modality-api-server](https://github.com/arkanovicz/modality/tree/master/modality-api-server) - API servlet serving Velocity JSON templates
+ [modality-webapp](https://github.com/arkanovicz/modality/tree/master/modality-webapp) - servlets and filters for Modality database configuration and initialization in a J2EE webapp environment
+ [modality-webapp-auth](https://github.com/arkanovicz/modality/tree/master/modality-webapp-auth) - authentication filters assortment
+ [velocity-tools-apiclient](https://github.com/arkanovicz/modality/tree/master/velocity-tools-apiclient) - view layer API client tool
+ [velocity-tools-model](https://github.com/arkanovicz/modality/tree/master/velocity-tools-model) - view layer model tool
+ [modality-webapp-oauth-server](https://github.com/arkanovicz/modality/tree/master/modality-webapp-oauth-server) - OAuth2 server (work in progress)
+ [modality-webapp-oauth-client](https://github.com/arkanovicz/modality/tree/master/modality-webapp-oauth-server) - OAuth2 client (work in progress)

