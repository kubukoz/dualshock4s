#!/bin/bash
target_path=$(nix build .#packages.aarch64-linux.default --no-link --print-out-paths -L)

nix copy .#packages.aarch64-linux.default --to ssh://192.168.0.242
echo "$target_path/bin/dualshock4s"
