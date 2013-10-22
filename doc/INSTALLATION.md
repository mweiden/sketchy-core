## Installing and deploying Sketchy

If you want to use Sketchy core in a separate project, declare a dependency in
a `build.sbt` file as follows:
```scala
resolvers += "mvn-repo" at "https://raw.github.com/mweiden/mvn-repo/master/releases"

libraryDependencies += "com.soundcloud" %% "sketchy-core" % "0.4.3"
```
If you want to experiment with the example project or make modifications to the core
project itself, check out the code:
```
$ git clone git@github.com:soundcloud/sketchy-core.git
```
After you have checked out the code, run `make` in the root directory to test and
assemble the core and example projects using [sbt](http://www.scala-sbt.org):
```
$ make build # tests the core and example projects and assembles the example project
$ make sbt # starts the sbt console
```
To run the example project:

1. Edit the `sketchy.env.example` file to include all of your required environment variables.
2. Run `source sketch.env.example`.
3. Run the startup script `example/bin/worker` as follows: `$ ./example/bin/worker sketchy.example`
