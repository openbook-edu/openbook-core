# openbook-core

openbook-core is a plain Scala sbt module that handles data services and domain logic for
the openbook platform. This is not intended to be run as a standalone project: it is built to
be imported by other projects that want to be able to manipulate and provide access to
our database. It defines the types of data we store, and the methods and rules (business
logic) for interacting with that data.

## Usage

There currently is no stable release of openbook-core. We are working on the 1.0 branch and
to include the core in another project (ie: the openbook-api project) you should import the
latest snapshot until such time as we push a stable release.

Do this by adding `"https://maven.openbook.io/repositories/snapshots"` as a resolver in sbt
 and then adding the latest snapshot as a library dependency:

    "ca.openbook" %% "openbook-core" % "1.0-SNAPSHOT"

The core is cross-compiled for both Scala 2.10 and 2.11 and the '%%' sign in the dependency
will automatically add the version. Alternatively, you can specify the version manually:

    "io.openbook" % "openbook-core_2.10" % "1.0-SNAPSHOT"
    "io.openbook" % "openbook-core_2.11" % "1.0-SNAPSHOT"

## SBT

The project is build with Scala's sbt. All commands can be run from within the sbt prompt, or
from the bash shell by passing the command as an argument to sbt. You can run `sbt` and then
enter `compile` at the prompt, or you can just run `sbt compile` from bash.

## Compiling

You can compile the module by running `compile` from the sbt prompt. If you are experiencing problems
or compilation errors that you shouldn't be, one of the first steps to try is cleaning the build
out before compiling by running the `clean` command.

## Testing

Run tests with `test` command.

## Publishing

The module can be published in two ways: you can publish it to a *local* ivy2 repository
on your own computer. When published there, it can be imported into other projects on your machine,
primarily for testing. The command is: `publishLocal`.

Secondly, the module can be published to the openbook maven server. There are two folders
on the maven server for modules: `snapshots` and `releases`. Where your module is published
depends on the version number in your `build.sbt` file. If the version number ends with "-SNAPSHOT"
then it will be published as a snapshot.

The structure of the maven repository is as follows:

`https://maven.openbook.io/repositories/[snapshots|releases]/[top-level domain]/[company name]/[module name with scala version]/[module version]/`

For example our snapshot release will be sent to:

`https://maven.openbook.io/repositories/snapshots/ca/openbook/openbook-core_2.11/1.0-SNAPSHOT/`

While a proper release will be sent to:

`https://maven.openbook.io/repositories/releases/ca/openbook/openbook-core_2.11/1.0.1/`

You can publish the module by running the `publish` sbt command. Ensure that the version number is
 correct before publishing. Generally, however, publishing should be taken care of by our build
 server..

You can then import it into your project by adding our maven server as a resolver and including,
for example, `"io.openbook" % "openbook-core_2.11" % "1.0.1"` as a library dependency.
