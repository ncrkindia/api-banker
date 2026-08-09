#!/bin/bash

cd "$(dirname "$0")"

echo "============================================================"
echo " ApiBanker Build Script (Linux / macOS)"
echo "============================================================"
echo ""

SKIP_TESTS="-DskipTests"
GOAL="package"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --with-tests) SKIP_TESTS=""; shift ;;
    --install)    GOAL="install"; shift ;;
    *) shift ;;
  esac
done

echo "[1/3] Cleaning previous build..."
mvn clean $SKIP_TESTS || { echo "[ERROR] Clean failed!"; exit 1; }

echo ""
echo "[2/3] Compiling and packaging JAR + ZIP bundle..."
mvn $GOAL $SKIP_TESTS || { echo "[ERROR] Maven build failed!"; exit 1; }

echo ""
echo "[3/3] Building native platform installer..."
chmod +x scripts/build-installers.sh
scripts/build-installers.sh || { echo "[ERROR] Installer build failed!"; exit 1; }

echo ""
echo "============================================================"
echo " Build SUCCESS! Artifacts are in: target/artifacts/"
echo "============================================================"
echo "  Portable bundle:    target/artifacts/apibanker.zip"
OS="$(uname -s)"
case "${OS}" in
  Linux*)  echo "  Native installer:   target/artifacts/apibanker.deb (or .rpm)" ;;
  Darwin*) echo "  Native installer:   target/artifacts/ApiBanker.dmg (or .pkg)" ;;
esac
echo "============================================================"
