#!/bin/bash

# Change to project root directory
cd "$(dirname "$0")/.."

VERSION=$1

# If version is not provided, extract it from pom.xml
if [ -z "$VERSION" ]; then
  if [ -f "pom.xml" ]; then
    # Extract the first <version> tag's content
    VERSION=$(awk -F'[><]' '/<version>/{print $3; exit}' pom.xml)
    echo "No version provided. Extracted version from pom.xml: $VERSION"
  else
    echo "Usage: ./scripts/diff.sh <version>"
    echo "Example: ./scripts/diff.sh v1.1.0-beta"
    echo "Or run from a directory with pom.xml to auto-detect version."
    exit 1
  fi
fi

if [ -z "$VERSION" ]; then
  echo "Failed to extract version from pom.xml. Please provide it manually."
  exit 1
fi

# Ensure version starts with 'v'
if [[ $VERSION != v* ]]; then
  VERSION="v$VERSION"
fi

echo "Comparing $VERSION with develop"
git diff "$VERSION" develop