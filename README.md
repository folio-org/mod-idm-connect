# mod-idm-connect

Copyright (C) 2021 The Open Library Foundation

This software is distributed under the terms of the Apache License, Version 2.0. See the
file "[LICENSE](LICENSE)" for more information.

## Introduction

The module manages walk-in contracts. It is tailored to the specific requirements at Leipzig
University Library. You may not find this module useful in different environments.

## Configuration

For the module to be able to connect to the external IDM system you need to provide following
environment variables:

* `IDM_TOKEN` - the required access token for the IDM endpoints
* `IDM_URL` - search endpoint
* `IDM_CONTRACT_URL` - contracts endpoint
