{
  inputs.nixpkgs.url = "github:nixos/nixpkgs";
  inputs.flake-utils.url = "github:numtide/flake-utils";
  inputs.sbt-derivation.url = "github:zaninime/sbt-derivation";
  inputs.gitignore-source.url = "github:hercules-ci/gitignore.nix";
  inputs.gitignore-source.inputs.nixpkgs.follows = "nixpkgs";
  inputs.sn-bindgen.url = "github:indoorvivants/sn-bindgen";
  inputs.sn-bindgen.inputs = {
    sbt.follows = "sbt-derivation";
    nixpkgs.follows = "nixpkgs";
  };

  outputs = { self, nixpkgs, flake-utils, sbt-derivation, sn-bindgen, ... }@inputs:
    flake-utils.lib.eachDefaultSystem (
      system:
      let
        pkgs = import nixpkgs {
          inherit system;
          overlays = [
            sbt-derivation.overlays.default
            (

              _: _: {
                sn-bindgen-cli = sn-bindgen.packages.${system}.default;
              }
            )
          ];
        };
        PATHS = {
          BINDGEN_PATH = pkgs.sn-bindgen-cli + "/bin/bindgen";
          HIDAPI_PATH = pkgs.hidapi + "/include/hidapi/hidapi.h";
        };
      in
      {
        devShells.default = pkgs.mkShell {
          nativeBuildInputs = [
            pkgs.hidapi
            pkgs.sn-bindgen-cli
          ];

          inherit (PATHS) BINDGEN_PATH HIDAPI_PATH;
        };
        packages.default = pkgs.callPackage ./derivation.nix { inherit (inputs) gitignore-source; inherit PATHS; };
      }
    );
}
