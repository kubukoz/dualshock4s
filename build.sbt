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
  crossPlugin("org.typelevel" % "kind-projector" % "0.11.0"),
  crossPlugin("com.github.cb372" % "scala-typed-holes" % "0.1.3"),
  crossPlugin("com.kubukoz" % "better-tostring" % "0.2.2"),
  compilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1")
)

val commonSettings = Seq(
  scalaVersion := "2.13.2",
  scalacOptions --= Seq("-Xfatal-warnings"),
  name := "dualshock4s",
  updateOptions := updateOptions.value.withGigahorse(false),
  libraryDependencies ++= Seq(
    "org.typelevel" %% "cats-effect" % "2.1.3",
    "org.hid4java" % "hid4java" % "0.5.0",
    "org.scodec" %% "scodec-cats" % "1.0.0",
    "org.scodec" %% "scodec-stream" % "2.0.0",
    "co.fs2" %% "fs2-io" % "2.3.0",
    "org.scalatest" %% "scalatest" % "3.1.0" % Test
  ) ++ compilerPlugins
)

val dualshock4s =
  project.in(file(".")).settings(commonSettings)
