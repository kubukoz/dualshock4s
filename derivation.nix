{ mkSbtDerivation,  which, clang, hidapi, PATHS }:

let pname = "dualshock4s"; in

mkSbtDerivation {
  inherit pname;
  version = "0.1.0";
  depsSha256 = "sha256-uTKMI2s9MBmcN1fd7vyBRFYYNfHxtI/v9x951Ng5xv0=";

  buildInputs = [ which clang hidapi ];

  depsWarmupCommand = ''
    sbt appNative/compile
  '';
  overrideDepsAttrs = final: prev: {
    inherit (PATHS) /* BINDGEN_PATH */ HIDAPI_PATH;
  };
  inherit (PATHS) /* BINDGEN_PATH  */ HIDAPI_PATH;

  src = ./.;

  buildPhase = ''
    echo "which ld"
    which ld
    env
    sbt nativeLink
  '';

  installPhase = ''
    mkdir -p $out/bin
    cp app/.native/target/scala-3.3.1/dualshock4s-out $out/bin/$pname
  '';
}
