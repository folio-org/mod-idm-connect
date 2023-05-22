# mod-idm-connect

Copyright (C) 2021-2023 The Open Library Foundation

This software is distributed under the terms of the Apache License, Version 2.0. See the
file "[LICENSE](LICENSE)" for more information.

![Development funded by European Regional Development Fund (EFRE)](assets/EFRE_2015_quer_RGB_klein.jpg)

## Introduction

The module manages walk-in contracts. It is tailored to the specific requirements at Leipzig
University Library. You may not find this module useful in different environments.

## Configuration

For the module to be able to connect to the external IDM system you need to provide following
environment variables:

| Variable                | Description                                     | Default value |
|-------------------------|-------------------------------------------------|---------------|
| `IDM_TOKEN`             | Access token required for the IDM endpoints     |               |
| `IDM_URL`               | Search endpoint                                 |               |
| `IDM_CONTRACT_URL`      | Contracts endpoint                              |               |
| `IDM_READER_NUMBER_URL` | Card number endpoint                            |               |
| `IDM_TRUST_ALL`         | Whether all servers (SSL/TLS) should be trusted | false         |
