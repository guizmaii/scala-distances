import sbt.Keys.crossScalaVersions

lazy val scala212 = "2.12.8"
lazy val scala211 = "2.11.12"

ThisBuild / organization := "com.colisweb"
ThisBuild / scalaVersion := scala212
ThisBuild / crossScalaVersions := Seq(scala211, scala212)
ThisBuild / scalafmtOnCompile := true
ThisBuild / scalafmtCheck := true
ThisBuild / scalafmtSbtCheck := true

//// Dependencies

val scalaCacheVersion = "0.26.0"

lazy val googleMaps               = "com.google.maps"      % "google-maps-services" % "0.9.4"
lazy val squants                  = "org.typelevel"        %% "squants"             % "1.4.0"
lazy val cats                     = "org.typelevel"        %% "cats-core"           % "2.0.0"
lazy val catsEffect               = "org.typelevel"        %% "cats-effect"         % "2.0.0"
lazy val enumeratum               = "com.beachape"         %% "enumeratum"          % "1.5.13"
lazy val monix                    = "io.monix"             %% "monix"               % "3.0.0"
lazy val okHttpLoggingInterceptor = "com.squareup.okhttp3" % "logging-interceptor"  % "4.1.0"

lazy val circeVersion       = "0.12.1"
lazy val circeOpticsVersion = "0.12.0"

lazy val circeCore          = "io.circe" %% "circe-core"           % circeVersion
lazy val circeGeneric       = "io.circe" %% "circe-generic"        % circeVersion
lazy val circeGenericExtras = "io.circe" %% "circe-generic-extras" % circeVersion
lazy val circeParser        = "io.circe" %% "circe-parser"         % circeVersion
lazy val circeRefined       = "io.circe" %% "circe-refined"        % circeVersion
lazy val circeOptics        = "io.circe" %% "circe-optics"         % circeOpticsVersion

lazy val circeAll = Seq(circeCore, circeGeneric, circeGenericExtras, circeParser, circeRefined, circeOptics)

lazy val scalacacheCore =
  Seq(
    "com.github.cb372" %% "scalacache-core"        % scalaCacheVersion,
    "com.github.cb372" %% "scalacache-cats-effect" % scalaCacheVersion,
    "com.github.cb372" %% "scalacache-circe"       % scalaCacheVersion
  )

lazy val testKit = {
  val kantancsv = (
      (version: String) =>
        Seq(
          "com.nrinaudo" %% "kantan.csv"         % version,
          "com.nrinaudo" %% "kantan.csv-cats"    % version,
          "com.nrinaudo" %% "kantan.csv-generic" % version
        )
    )("0.5.0")

  Seq(
    "org.scalacheck" %% "scalacheck"            % "1.14.0",
    "org.scalatest"  %% "scalatest"             % "3.0.5",
    "com.beachape"   %% "enumeratum-scalacheck" % "1.5.16",
    "io.circe"       %% "circe-literal"         % circeVersion,
    "com.github.pureconfig" %% "pureconfig" % "0.13.0",
    monix
  ) ++ kantancsv
}.map(_ % Test)

//// Main projects

lazy val root = Project(id = "scala-distances", base = file("."))
  .settings(moduleName := "root")
  .settings(noPublishSettings)
  .aggregate(core, `google-provider`, `redis-cache`, `caffeine-cache`, tests)
  .dependsOn(core, `google-provider`, `redis-cache`, `caffeine-cache`, tests)

lazy val core = project
  .settings(moduleName := "scala-distances-core")
  .settings(addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full))
  .settings(
    libraryDependencies ++= Seq(
      squants,
      cats,
      catsEffect,
      enumeratum,
      okHttpLoggingInterceptor
    ) ++ scalacacheCore ++ circeAll ++ testKit
  )

//// Providers

lazy val `google-provider` = project
  .in(file("providers/google"))
  .settings(moduleName := "scala-distances-provider-google")
  .settings(libraryDependencies += googleMaps)
  .dependsOn(core)

//// Caches

lazy val `redis-cache` = project
  .in(file("caches/redis"))
  .settings(moduleName := "scala-distances-cache-redis")
  .settings(libraryDependencies += "com.github.cb372" %% "scalacache-redis" % scalaCacheVersion)
  .dependsOn(core)

lazy val `caffeine-cache` = project
  .in(file("caches/caffeine"))
  .settings(moduleName := "scala-distances-cache-caffeine")
  .settings(libraryDependencies += "com.github.cb372" %% "scalacache-caffeine" % scalaCacheVersion)
  .dependsOn(core)

//// Meta projects

lazy val tests = project
  .settings(noPublishSettings)
  .settings(libraryDependencies ++= testKit)
  .dependsOn(core, `google-provider`, `redis-cache`, `caffeine-cache`)

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
ThisBuild / credentials += Credentials(Path.userHome / ".bintray" / ".credentials")
ThisBuild / licenses := Seq("MIT" -> url("http://opensource.org/licenses/MIT"))
ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://gitlab.com/colisweb-open-source/scala-distances"),
    "scm:git:git@gitlab.com:colisweb-open-source/scala-distances.git"
  )
)
ThisBuild / homepage := Some(url("https://gitlab.com/colisweb-open-source/scala-distances"))
ThisBuild / developers := List(
  Developer("guizmaii", "Jules Ivanic", "jules.ivanic@gmail.com", url("https://guizmaii.github.io/")),
  Developer("simooonbar", "Simon Bar", "simon.bar@colisweb.com", url("https://gitlab.com/snatz"))
)
ThisBuild / bintrayOrganization := Some("colisweb")
ThisBuild / bintrayReleaseOnPublish := true
ThisBuild / publishMavenStyle := true

//// Aliases

/**
  * Copied from kantan.csv
  */
addCommandAlias("runBench", "benchmark/jmh:run -i 10 -wi 10 -f 2 -t 1")
