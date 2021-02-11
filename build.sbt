import CompileFlags._
import sbt.Keys.crossScalaVersions

lazy val scala212               = "2.12.11"
lazy val scala213               = "2.13.2"
lazy val supportedScalaVersions = List(scala213, scala212)

ThisBuild / organization := "com.colisweb"
ThisBuild / scalaVersion := scala213
ThisBuild / crossScalaVersions := supportedScalaVersions
ThisBuild / scalafmtOnCompile := true
ThisBuild / scalafmtCheck := true
ThisBuild / scalafmtSbtCheck := true
ThisBuild / scalacOptions ++= crossScalacOptions(scalaVersion.value)

//// Main projects

lazy val root = Project(id = "scala-distances", base = file("."))
  .settings(moduleName := "root")
  .settings(noPublishSettings)
  .aggregate(core, `google-provider`, `redis-cache`, `caffeine-cache`, tests)
  .dependsOn(core, `google-provider`, `redis-cache`, `caffeine-cache`, tests)

lazy val core = project
  .settings(moduleName := "scala-distances-core")
  .settings(
    libraryDependencies ++= Seq(
      CompileTimeDependencies.catsEffect,
      CompileTimeDependencies.circe,
      CompileTimeDependencies.circeGeneric,
      CompileTimeDependencies.circeGenericExtras,
      CompileTimeDependencies.circeOptics,
      CompileTimeDependencies.circeParser,
      CompileTimeDependencies.circeRefined,
      CompileTimeDependencies.enumeratum,
      CompileTimeDependencies.loggingInterceptor,
      CompileTimeDependencies.scalaCache,
      CompileTimeDependencies.scalaCacheCatsEffect,
      CompileTimeDependencies.scalaCacheCirce,
      CompileTimeDependencies.scalaCompat,
      CompileTimeDependencies.squants
    ) ++ Seq(
      CompileTimeDependencies.monix % Test,
      TestDependencies.kantan,
      TestDependencies.kantanCats,
      TestDependencies.kantanGeneric,
      TestDependencies.circeLiteral,
      TestDependencies.scalacheck,
      TestDependencies.scalatestPlus,
      TestDependencies.scalatest,
      TestDependencies.enumeratumScalacheck
    )
  )

//// Providers

lazy val `google-provider` = project
  .in(file("providers/google"))
  .settings(moduleName := "scala-distances-provider-google")
  .settings(libraryDependencies += CompileTimeDependencies.googleMaps)
  .dependsOn(core)

//// Caches

lazy val `redis-cache` = project
  .in(file("caches/redis"))
  .settings(moduleName := "scala-distances-cache-redis")
  .settings(libraryDependencies += CompileTimeDependencies.scalaCacheRedis)
  .dependsOn(core)

lazy val `caffeine-cache` = project
  .in(file("caches/caffeine"))
  .settings(moduleName := "scala-distances-cache-caffeine")
  .settings(libraryDependencies += CompileTimeDependencies.scalaCacheCaffeine)
  .dependsOn(core)

//// Meta projects

lazy val tests = project
  .settings(noPublishSettings)
  .dependsOn(core % "test->test;compile->compile", `google-provider`, `redis-cache`, `caffeine-cache`)
  .settings(libraryDependencies += CompileTimeDependencies.pureconfig)


//// Publishing settings

/**
  * Copied from Cats
  */
lazy val noPublishSettings = Seq(
  publish := {},
  publishLocal := {},
  publishArtifact := false
)

ThisBuild / releaseCrossBuild := true
ThisBuild / licenses := Seq("MIT" -> url("http://opensource.org/licenses/MIT"))
ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://gitlab.com/colisweb-open-source/scala/scala-distances"),
    "scm:git:git@gitlab.com:colisweb-open-source/scala/scala-distances.git"
  )
)
ThisBuild / homepage := Some(url("https://gitlab.com/colisweb-open-source/scala/scala-distances"))
ThisBuild / developers := List(
  Developer("guizmaii", "Jules Ivanic", "jules.ivanic@gmail.com", url("https://guizmaii.github.io/")),
  Developer("simooonbar", "Simon Bar", "simon.bar@colisweb.com", url("https://gitlab.com/snatz"))
)
ThisBuild / bintrayOrganization := Some("colisweb")
ThisBuild / publishMavenStyle := true

//// Aliases

/**
  * Copied from kantan.csv
  */
addCommandAlias("runBench", "benchmark/jmh:run -i 10 -wi 10 -f 2 -t 1")

def compileWithMacroParadise: Command =
  Command.command("compileWithMacroParadise") { state =>
    import Project._
    val extractedState = extract(state)
    val stateWithMacroParadise = CrossVersion.partialVersion(extractedState.get(scalaVersion)) match {
      case Some((2, n)) if n >= 13 => state
      case _ =>
        extractedState.appendWithSession(
          addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full),
          state
        )
    }
    val (stateAfterCompileWithMacroParadise, _) =
      extract(stateWithMacroParadise).runTask(Compile / compile, stateWithMacroParadise)
    stateAfterCompileWithMacroParadise
  }

ThisBuild / commands ++= Seq(compileWithMacroParadise)
addCommandAlias("compile", "compileWithMacroParadise")
