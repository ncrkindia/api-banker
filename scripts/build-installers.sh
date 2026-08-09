#!/bin/bash
set -e

# Change to project root directory
cd "$(dirname "$0")/.."

echo "===================================================="
echo "ApiBanker - Native Installer Builder (macOS/Linux)"
echo "===================================================="

echo ""
echo "[1/3] Building Fat JAR with Maven..."
mvn clean package -DskipTests

echo ""
echo "[2/3] Preparing jpackage input..."
rm -rf target/jpackage-input
mkdir -p target/jpackage-input
cp target/apibanker-*.jar target/jpackage-input/apibanker-app.jar

echo ""
echo "[3/3] Running jpackage..."
echo "Detecting OS..."

OS="$(uname -s)"
case "${OS}" in
    Linux*)
        echo "Linux detected. Building .deb and .rpm installers..."
        
        # Build DEB
        echo "Building DEB..."
        jpackage --type deb --input target/jpackage-input --dest target/installers --name ApiBanker --main-jar apibanker-app.jar --main-class in.slpro.apibanker.App --linux-shortcut || echo "DEB build failed (do you have dpkg-deb?)"
        
        # Build RPM
        echo "Building RPM..."
        jpackage --type rpm --input target/jpackage-input --dest target/installers --name ApiBanker --main-jar apibanker-app.jar --main-class in.slpro.apibanker.App --linux-shortcut || echo "RPM build failed (do you have rpmbuild?)"
        ;;
    Darwin*)
        echo "macOS detected. Building .pkg and .dmg installers..."
        
        # Build DMG
        echo "Building DMG..."
        jpackage --type dmg --input target/jpackage-input --dest target/installers --name ApiBanker --main-jar apibanker-app.jar --main-class in.slpro.apibanker.App || echo "DMG build failed"
        
        # Build PKG
        echo "Building PKG..."
        jpackage --type pkg --input target/jpackage-input --dest target/installers --name ApiBanker --main-jar apibanker-app.jar --main-class in.slpro.apibanker.App || echo "PKG build failed"
        ;;
    *)
        echo "Unsupported OS for this script: ${OS}. Use build-installers.bat for Windows."
        exit 1
        ;;
esac

echo ""
echo "===================================================="
echo "Done! Check the 'target/installers' directory."
echo "===================================================="
