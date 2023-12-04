{ mkSbtDerivation, gitignore-source, which, clang, s2n-tls }:

let pname = "dualshock4s"; in

mkSbtDerivation {
  inherit pname;
  version = "0.1.0";
  depsSha256 = "";

  buildInputs = [ which clang ];
  nativeBuildInputs = [ s2n-tls ];
  depsWarmupCommand = ''
    sbt compile
  '';

  src = gitignore-source.lib.gitignoreSource ./.;

  buildPhase = ''
    sbt nativeLink
  '';

  installPhase = ''
    mkdir -p $out/bin
    cp app/.native/target/scala-3.3.1/dualshock4s-out $out/bin/$pname
  '';
}
