import sbt._

object TestVersions {
  final lazy val kantan        = "0.6.1"
  final lazy val scalacheck    = "1.15.0"
  final lazy val scalatest     = "3.2.3"
  final lazy val scalatestPlus = "3.1.0.0-RC2"
  final lazy val enumeratum    = "1.6.1"
}

object TestDependencies {
  final lazy val kantan               = "com.nrinaudo"      %% "kantan.csv"               % TestVersions.kantan % Test
  final lazy val kantanCats           = "com.nrinaudo"      %% "kantan.csv-cats"          % TestVersions.kantan % Test
  final lazy val kantanGeneric        = "com.nrinaudo"      %% "kantan.csv-generic"       % TestVersions.kantan % Test
  final lazy val circeLiteral         = "io.circe"          %% "circe-literal"            % Versions.circe % Test
  final lazy val scalacheck           = "org.scalacheck"    %% "scalacheck"               % TestVersions.scalacheck % Test
  final lazy val scalatestPlus        = "org.scalatestplus" %% "scalatestplus-scalacheck" % TestVersions.scalatestPlus % Test
  final lazy val scalatest            = "org.scalatest"     %% "scalatest"                % TestVersions.scalatest % Test
  final lazy val enumeratumScalacheck = "com.beachape"      %% "enumeratum-scalacheck"    % TestVersions.enumeratum % Test
}