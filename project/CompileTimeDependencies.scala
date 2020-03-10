import sbt._

object Versions {
  lazy val cats               = "2.1.1"
  lazy val catsEffect         = "2.1.3"
  lazy val circe              = "0.12.1"
  lazy val circeOptics        = "0.12.0"
  lazy val enumeratum         = "1.6.1"
  lazy val google             = "0.14.0"
  lazy val loggingInterceptor = "4.7.2"
  lazy val monix              = "3.2.2"
  lazy val scalaCache         = "0.28.0"
  lazy val scalaCompat        = "2.1.6"
  lazy val shapeless          = "2.3.3"
  lazy val squants            = "1.6.0"
}

object CompileTimeDependencies {

  lazy val cats                 = "org.typelevel"          %% "cats-core"               % Versions.cats
  lazy val catsEffect           = "org.typelevel"          %% "cats-effect"             % Versions.catsEffect
  lazy val catsKernel           = "org.typelevel"          %% "cats-kernel"             % Versions.cats
  lazy val circe                = "io.circe"               %% "circe-core"              % Versions.circe
  lazy val circeGeneric         = "io.circe"               %% "circe-generic"           % Versions.circe
  lazy val circeGenericExtras   = "io.circe"               %% "circe-generic-extras"    % Versions.circe
  lazy val circeOptics          = "io.circe"               %% "circe-optics"            % Versions.circeOptics
  lazy val circeParser          = "io.circe"               %% "circe-parser"            % Versions.circe
  lazy val circeRefined         = "io.circe"               %% "circe-refined"           % Versions.circe
  lazy val enumeratum           = "com.beachape"           %% "enumeratum"              % Versions.enumeratum
  lazy val googleMaps           = "com.google.maps"         % "google-maps-services"    % Versions.google
  lazy val loggingInterceptor   = "com.squareup.okhttp3"    % "logging-interceptor"     % Versions.loggingInterceptor
  lazy val monixEval            = "io.monix"               %% "monix-eval"              % Versions.monix
  lazy val monixExecution       = "io.monix"               %% "monix-execution"         % Versions.monix
  lazy val scalaCache           = "com.github.cb372"       %% "scalacache-core"         % Versions.scalaCache
  lazy val scalaCacheCaffeine   = "com.github.cb372"       %% "scalacache-caffeine"     % Versions.scalaCache
  lazy val scalaCacheCatsEffect = "com.github.cb372"       %% "scalacache-cats-effect"  % Versions.scalaCache
  lazy val scalaCacheCirce      = "com.github.cb372"       %% "scalacache-circe"        % Versions.scalaCache
  lazy val scalaCacheRedis      = "com.github.cb372"       %% "scalacache-redis"        % Versions.scalaCache
  lazy val scalaCompat          = "org.scala-lang.modules" %% "scala-collection-compat" % Versions.scalaCompat
  lazy val shapeless            = "com.chuusai"            %% "shapeless"               % Versions.shapeless
  lazy val squants              = "org.typelevel"          %% "squants"                 % Versions.squants
}
