import bindgen.interface.Binding

inThisBuild(
  List(
    tlBaseVersion := "0.2",
    organization := "com.kubukoz.dualshock4s",
    startYear := Some(2020),
    homepage := Some(url("https://github.com/kubukoz/dualshock4s")),
    licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
    developers := List(tlGitHubDev("kubukoz", "Jakub Kozłowski")),
    sonatypeCredentialHost := Sonatype.sonatypeLegacy,
  )
)

Global / onChangedBuildSource := ReloadOnSourceChanges

def crossPlugin(x: sbt.librarymanagement.ModuleID) = compilerPlugin(x.cross(CrossVersion.full))

val compilerPlugins = List(
  crossPlugin("org.polyvariant" % "better-tostring" % "0.3.17")
)

val commonSettings = Seq(
  scalaVersion := "3.5.2",
  scalacOptions --= Seq("-Xfatal-warnings"),
  scalacOptions ++= Seq(
    "-Wunused:all"
  ),
  resolvers += Resolver.mavenLocal,
  Compile / doc / sources := Nil,
)

val hidapi =
  crossProject(NativePlatform)
    .crossType(CrossType.Full)
    .settings(commonSettings)
    .nativeConfigure(
      _.settings(
        bindgenBindings := Seq(
          Binding(file(sys.env("HIDAPI_PATH")), "libhidapi")
            .withLinkName {
              val isLinux = {
                import sys.process._
                "uname".!!.trim == "Linux"
              }

              // hidraw can contain stale reports
              // which doesn't play well with backpressuring with `.metered`.
              // libusb is more pull-based, so it'll be easier to work with (similar to hidapi on mac).
              if (isLinux) "hidapi-libusb"
              // if (isLinux) "hidapi-hidraw"
              else "hidapi"
            }
        ),
        bindgenBinary := file(sys.env("BINDGEN_PATH")),
        scalacOptions += "-Wconf:msg=unused import:s",
      )
        .enablePlugins(BindgenPlugin)
    )

val app =
  crossProject(JVMPlatform, NativePlatform)
    .crossType(CrossType.Full)
    .settings(commonSettings)
    .settings(
      libraryDependencies ++= Seq(
        "org.typelevel" %%% "cats-effect" % "3.5.2",
        "co.fs2" %%% "fs2-io" % "3.9.3",
        "org.scodec" %%% "scodec-core" % "2.2.2",
        "org.scodec" %%% "scodec-cats" % "1.2.0",
        "io.chrisdavenport" %%% "crossplatformioapp" % "0.1.0",
        "com.monovore" %%% "decline-effect" % "2.4.1",
      ) ++ compilerPlugins
    )
    .jvmConfigure(
      _.enablePlugins(JavaAppPackaging)
        .settings(
          libraryDependencies += "org.hid4java" % "hid4java" % "0.8.0",
          fork := true,
        )
    )
    .nativeConfigure(
      _.settings(
        libraryDependencies ++= Seq(
          "com.armanbilge" %%% "epollcat" % "0.1.6"
        )
      )
        .dependsOn(hidapi.native)
    )

val root = project
  .in(file("."))
  .aggregate(List(app, hidapi).flatMap(_.componentProjects).map(p => p: ProjectReference): _*)
  .settings(publish / skip := true, sonatypeProfileName := "com.kubukoz")
