# Pekko Persistence MapDB

@@@ index

* [Journal and Snapshots](journal-snapshots.md)
* [Query](query.md)
* [Configuration](configuration.md)

@@@

pekko-persistence-mapdb is a plugin for [pekko-persistence](https://pekko.apache.org/docs/pekko/current/typed/index-persistence.html) which uses MapDB for storing journal and snapshot messages.

## Installation

Add the following dependency to your `build.sbt`:


@@dependency[sbt,Maven] {
  group="io.github.fgrutsch"
  artifact="pekko-persistence-mapdb_$scala.binary.version$"
  version="$version$"
}

## Changelog

For the changelog check [this](https://github.com/fgrutsch/pekko-persistence-mapdb/releases) page.


## Pages

@@ toc
