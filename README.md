# mod-idm-connect

Copyright (C) 2021-2026 The Open Library Foundation

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

### Proxy Configuration

HTTP/HTTPS proxy configuration is supported for connections to the external IDM system.

#### Java System Properties

Proxy settings are configured via JVM system properties:

```bash
-Dhttp.proxyHost=proxy.example.com
-Dhttp.proxyPort=8080
-Dhttps.proxyHost=proxy.example.com
-Dhttps.proxyPort=8443
-Dhttp.nonProxyHosts=localhost|127.0.0.1|*.internal
```

#### Docker Environment Variables

When running in a Docker container, you can use standard environment variables which are translated
into system properties by the base image:

| Variable      | Description                                   | Example                          |
|---------------|-----------------------------------------------|----------------------------------|
| `HTTP_PROXY`  | HTTP proxy URL                                | `http://proxy.example.com:8080`  |
| `HTTPS_PROXY` | HTTPS proxy URL                               | `https://proxy.example.com:8443` |
| `NO_PROXY`    | Comma-separated list of hosts to bypass proxy | `localhost,127.0.0.1,.internal`  |

#### Docker Compose Example

```yaml
version: '3'
services:
  mod-idm-connect:
    image: mod-idm-connect:latest
    environment:
      - HTTP_PROXY=http://proxy.example.com:8080
      - HTTPS_PROXY=https://proxy.example.com:8443
      - NO_PROXY=localhost,127.0.0.1
      - IDM_URL=http://idm.example.com/api
      - IDM_TOKEN=your-token-here
```
