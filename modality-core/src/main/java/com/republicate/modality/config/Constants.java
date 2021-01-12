package com.republicate.modality.config;
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.HashSet;
import java.util.Set;

public interface Constants
{
    String MODALITY_PROPERTIES = "modality.properties";

    String MODALITY_DEFAULTS_PATH = "com/republicate/modality/" + MODALITY_PROPERTIES;

    String MODEL_LOGGER_NAME = "logger.name";

    String MODEL_ID = "id";
    String DEFAULT_MODEL_ID = "model";
    
    String MODEL_WRITE_ACCESS = "write";

    String MODEL_REVERSE_MODE = "reverse";

    // TODO - Velocity-aware model should be a subclass
    String MODEL_VELOCITY_ENGINE = "velocity_engine";

    String MODEL_CONFIGURATION = "configuration";

    String MODEL_DEFINITION = "definition";

    String MODEL_DEFAULT_PATH = "model.xml";

    String MODEL_SCHEMA = "schema";

    String MODEL_IDENTIFIERS_INFLECTOR = "identifiers.inflector";

    String MODEL_IDENTIFIERS_MAPPING = "identifiers.mapping";

    String MODEL_FILTERS_READ = "filters.read";

    String MODEL_FILTERS_WRITE = "filters.write";

    String MODEL_FILTERS_CRYPTOGRAPH = "filters.cryptograph";

    String MODEL_DATASOURCE = "datasource";

    String MODEL_DATABASE = "database";

    String MODEL_CREDENTIALS_USER = "credentials.user";

    String MODEL_CREDENTIALS_PASSWORD = "credentials.password";

    String MODEL_INSTANCES_FACTORY = "instances.factory";

    String MODEL_INSTANCES_CLASSES = "instances.classes";

    String MODEL_NAMESPACE_URI = "https://republicate.com/index.html";

    String MODEL_CONNECTIONS_CHECK_INTERVAL = "connections_check_interval";

    String MODEL_VERSIONNING_SCRIPTS = "versionning_scripts";

    String DATABASE_VERSION = "database_version";

    String CREATE_DATABASE_VERSION = "create_database_version";

    String DEFAULT_MIGRATION_ROOT_PATH = "migrations";
}
