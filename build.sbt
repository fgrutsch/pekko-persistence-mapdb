import java.time.LocalDate

ThisBuild / versionScheme      := Some("early-semver")
ThisBuild / scalaVersion       := crossScalaVersions.value.last
ThisBuild / crossScalaVersions := Seq("2.13.18", "3.8.2")
ThisBuild / publishTo          := {
  val centralSnapshots = "https://central.sonatype.com/repository/maven-snapshots/"
  if (isSnapshot.value) Some("central-snapshots" at centralSnapshots)
  else localStaging.value
}

addCommandAlias("codeFmt", ";headerCreate;scalafmtAll;scalafmtSbt;scalafixAll")
addCommandAlias("codeVerify", ";scalafmtCheckAll;scalafmtSbtCheck;scalafixAll --check;headerCheck")

lazy val commonSettings = Seq(
  organization := "io.github.fgrutsch",
  startYear    := Some(2021),
  homepage     := Some(url("https://github.com/fgrutsch/pekko-persistence-mapdb")),
  licenses     := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
  scmInfo      := Some(
    ScmInfo(homepage.value.get, "scm:git:https://github.com/fgrutsch/pekko-persistence-mapdb.git")
  ),
  developers += Developer(
    "contributors",
    "Contributors",
    "",
    url("https://github.com/fgrutsch/pekko-persistence-mapdb/graphs/contributors")
  ),
  scalacOptions ++= {
    val common = Seq(
      "-deprecation",
      "-encoding",
      "utf-8",
      "-feature",
      "-language:higherKinds",
      "-unchecked",
      "-Werror"
    )

    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((3, _)) =>
        common ++ List(
          "-explain-types"
        )
      case _ =>
        common ++ List(
          "-explaintypes",
          "-Xcheckinit",
          "-Wdead-code",
          "-Wunused:imports",
          "-Xsource:3"
        )
    }
  },
  Test / parallelExecution := false,
  headerLicense     := Some(HeaderLicense.ALv2(LocalDate.now.getYear.toString, "pekko-persistence-mapdb contributors")),
  semanticdbEnabled := true,
  semanticdbVersion := scalafixSemanticdb.revision
)

lazy val root = project
  .in(file("."))
  .settings(commonSettings)
  .settings(publish / skip := true)
  .aggregate(core, docs)

lazy val core = project
  .in(file("core"))
  .settings(commonSettings)
  .settings(
    name := "pekko-persistence-mapdb",
    libraryDependencies ++= Dependencies.core,
    libraryDependencies ++= {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((3, _)) => Nil
        case _            => Seq(compilerPlugin(Dependencies.betterMonadicFor))
      }
    }
  )

lazy val docs = project
  .in(file("docs"))
  .settings(commonSettings)
  .settings(
    name                         := "pekko-persistence-mapdb-docs",
    publish / skip               := true,
    githubWorkflowArtifactUpload := false,
    paradoxProperties ++= Map(
      "version" -> version.value
    )
  )
  .dependsOn(core)
  .enablePlugins(ParadoxPlugin)
