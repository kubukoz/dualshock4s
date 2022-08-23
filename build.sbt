inThisBuild(
  List(
    organization := "com.kubukoz",
    homepage := Some(url("https://github.com/kubukoz/dualshock4s")),
    licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
    developers := List(
      Developer(
        "kubukoz",
        "Jakub Kozłowski",
        "kubukoz@gmail.com",
        url("https://kubukoz.com")
      )
    )
  )
)

def crossPlugin(x: sbt.librarymanagement.ModuleID) = compilerPlugin(x.cross(CrossVersion.full))

val compilerPlugins = List(
  crossPlugin("org.polyvariant" % "better-tostring" % "0.3.15")
)

val commonSettings = Seq(
  scalaVersion := "3.1.2",
  scalacOptions --= Seq("-Xfatal-warnings"),
  name := "dualshock4s",
  resolvers += Resolver.mavenLocal,
  libraryDependencies ++= Seq(
    "org.typelevel" %% "cats-effect" % "3.2.7",
    "org.hid4java" % "hid4java" % "develop-SNAPSHOT",
    "org.scodec" %% "scodec-cats" % "1.1.0",
    "org.scodec" %% "scodec-stream" % "3.0.2",
    "co.fs2" %% "fs2-io" % "3.1.2"
  ) ++ compilerPlugins,
  Compile / doc / sources := Nil,
  resolvers += Resolver.mavenLocal
)

val dualshock4s =
  project
    .in(file("."))
    .settings(commonSettings)
    .enablePlugins(JavaAppPackaging)
