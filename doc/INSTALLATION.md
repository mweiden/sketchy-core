## Installing and deploying Sketchy

If you want to use Sketchy core in a separate project, declare a dependency in
a `build.sbt` file as follows:
```scala
resolvers += "mvn-repo" at "https://raw.github.com/mweiden/mvn-repo/master/releases"

libraryDependencies += "com.soundcloud" %% "sketchy-core" % "0.4.2"
```
If you want to experiment with the example project or make modifications to the core
project itself, check out the code:
```
$ git clone git@github.com:soundcloud/sketchy-core.git
```
After you have the code, use the `Makefile` in the root directory to test and
assemble the the core and example projects:
```
$ make build # tests the core and example projects and assembles the example project
$ make sbt # starts the sbt console
```
To run the example project, edit and source `sketchy.env.example` setting up all
required environment variables. Then use the startup script `example/bin/worker`:
```
$ ./example/bin/worker sketchy.example
```

