// import bindgen.interface.Binding

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

Global / onChangedBuildSource := ReloadOnSourceChanges

def crossPlugin(x: sbt.librarymanagement.ModuleID) = compilerPlugin(x.cross(CrossVersion.full))

val compilerPlugins = List(
  crossPlugin("org.polyvariant" % "better-tostring" % "0.3.17")
)

val commonSettings = Seq(
  scalaVersion := "3.3.1",
  scalacOptions --= Seq("-Xfatal-warnings"),
  scalacOptions ++= Seq(
    "-Wunused:all"
  ),
  name := "dualshock4s",
  resolvers += Resolver.mavenLocal,
  Compile / doc / sources := Nil
)

// val hidapi =
//   crossProject(NativePlatform)
//     .crossType(CrossType.Pure)
//     .settings(commonSettings)
// .nativeConfigure(
//   _.settings(
//     bindgenBindings := Seq(
//       Binding
//         .builder(file(sys.env("HIDAPI_PATH")), "libhidapi")
//         .withLinkName("hidapi")
//         .build
//     ),
//     bindgenBinary := file(sys.env("BINDGEN_PATH"))
//   )
//     .enablePlugins(BindgenPlugin)
// )

val app =
  crossProject(JVMPlatform, NativePlatform)
    .crossType(CrossType.Pure)
    .settings(commonSettings)
    .settings(
      libraryDependencies ++= Seq(
        "org.typelevel" %%% "cats-effect" % "3.5.2",
        "co.fs2" %%% "fs2-io" % "3.9.3",
        "org.scodec" %%% "scodec-cats" % "1.2.0",
        "io.chrisdavenport" %%% "crossplatformioapp" % "0.1.0"
      ) ++ compilerPlugins
    )
    .jvmConfigure(
      _.enablePlugins(JavaAppPackaging)
        .settings(
          libraryDependencies += "org.hid4java" % "hid4java" % "develop-SNAPSHOT"
        )
    )
    .nativeConfigure(
      _.settings(
        libraryDependencies ++= Seq(
          "com.armanbilge" %%% "epollcat" % "0.1.6"
        ),
        nativeLinkingOptions ++= Seq("-v"),
        nativeClang := file {
          import sys.process._
          "which cc".!!.trim
        }
      )
      // .dependsOn(hidapi.native)
    )

val root = project
  .in(file("."))
  .aggregate(app.componentProjects.map(p => p: ProjectReference): _*)
