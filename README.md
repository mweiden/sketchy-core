# Sketchy

[![Build Status](https://travis-ci.org/soundcloud/sketchy-core.png?branch=travis-ci)](https://travis-ci.org/soundcloud/sketchy-core)

Sketchy is a framework for reducing text-based spam and other malicious user
activity on web applications. Sketchy addresses several common issues on web
applications:

* Detect when a user submits text that contains spam content.
* Detect when a user submits multiple texts that are nearly identical.
* Rate-limit malicious actions that users perform repeatedly.
* Check user signatures such as IP addresses against external blacklist APIs.
* Collect and consolidate reports of spam from users.

Additionally, you can customize Sketchy to address a broader range of issues.

Sketchy is divided into two projects: the Sketchy [core](core/) and an
[example](example/) network. Core is an interface for building networks
customizable to user actions on a client application, which the example
network illustrates.

## Documentation

In addition to this README file, there is a directory of [additional documentation](/doc).

The [Getting started](doc/GETTING_STARTED.md) guide helps you to understand the
[example](example/) project, which illustrates what a full implementation of the core
project might look like. The example project addresses all of the common issues previously
listed.

**Important:** All of the limit or threshold values in the example project are placeholders; SoundCloud does not use these values and recommends that you do not use them.

To contribute to Sketchy there is a [Guide to contributing to Sketchy](doc/CONTRIBUTING.md)
and a list of [potential improvements](doc/TODO.md).

## Installation and deployment

For information about installing Sketchy or declaring it as a dependency in your
own project, see the [installation instructions](doc/INSTALLATION.md).

## Copyright

Copyright (c) 2013 [SoundCloud Ltd.](http://soundcloud.com) | [Trust, Safety
& Security Team](mailto:sketchy@soundcloud.com).

See the [LICENSE](LICENSE.md) file for details.

