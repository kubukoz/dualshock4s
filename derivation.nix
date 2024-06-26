{ mkSbtDerivation, gitignore-source, which, clang, hidapi, sn-bindgen-cli, PATHS }:

let pname = "dualshock4s"; in

mkSbtDerivation {
  inherit pname;
  version = "0.1.0";
  depsSha256 = "sha256-d44LmzlUwaviqVmpiSPmvl7aFJzsSMVMhQBbjxosoTU=";

  buildInputs = [ which clang ];
  nativeBuildInputs = [
    hidapi
    sn-bindgen-cli
  ];
  depsWarmupCommand = ''
    sbt appNative/compile
  '';
  overrideDepsAttrs = final: prev: {
    buildInputs = [ which clang ];
    inherit (PATHS) BINDGEN_PATH HIDAPI_PATH;
  };
  inherit (PATHS) BINDGEN_PATH HIDAPI_PATH;

  src = gitignore-source.lib.gitignoreSource ./.;

  buildPhase = ''
    sbt nativeLink
  '';

  installPhase = ''
    mkdir -p $out/bin
    cp app/.native/target/scala-3.3.1/dualshock4s-out $out/bin/$pname
  '';
}
