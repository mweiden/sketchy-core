# Sketchy

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
customizeable to user actions on a client application, which the example
network illustrates.

## Documentation

In addition to this README file, there is a directory of [addtional documentation](/doc).

The [Getting Started Guide](doc/GETTING_STARTED.md) will help you understand the
[example](example/) project, which illustrates what a full implementation of the core
project might look like. The example project addresses all of the common issues previously
listed. Please be aware that all limits or thresholds used in the project serve the sole
purpose of placeholders, and do neither reflect values used or recommended by SoundCloud.

If you wish to contribute to Sketchy there is a [Guide to Contributing to Sketchy](doc/CONTRIBUTING.md)
and a list of [Potential Improvements](doc/TODO.md).

## Installation and deployment

For information on installing Sketchy or declaring it as a dependency in your
own project, please see the [Installation Guide](doc/INSTALLATION.md).

## Copyright

Copyright (c) 2013 [SoundCloud Ltd.](http://soundcloud.com) | [Trust, Safety
& Security Team](mailto:sketchy@soundcloud.com).

See [LICENSE](LICENSE.md) for details.

