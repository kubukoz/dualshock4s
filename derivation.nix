{ mkSbtDerivation, which, clang, hidapi, PATHS }:

let
  pname = "dualshock4s";
  buildInputs = [ which clang hidapi ];

in mkSbtDerivation {
  inherit pname;
  version = "0.1.0";
  depsSha256 = "sha256-bjkCLkBCbSbKwU1r+nZQfuPgIG+HLSS62ViXuaUidXU=";
  inherit buildInputs;

  depsWarmupCommand = ''
    sbt appNative/compile
  '';
  overrideDepsAttrs = final: prev: {
    inherit buildInputs;
    inherit (PATHS) BINDGEN_PATH HIDAPI_PATH;
  };
  inherit (PATHS) BINDGEN_PATH HIDAPI_PATH;

  src = ./.;

  buildPhase = ''
    sbt nativeLink
  '';

  installPhase = ''
    mkdir -p $out/bin
    cp app/native/target/scala-3.5.2/dualshock4s-out $out/bin/$pname
  '';
}
