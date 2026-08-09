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
    echo "Usage: ./scripts/release.sh <version>"
    echo "Example: ./scripts/release.sh v1.1.0-beta"
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

echo "Creating release for version: $VERSION"

# 1. Create new branch from current commit
git checkout -b "release/$VERSION"

# 2. Push the new branch to remote
git push origin "release/$VERSION"

# 3. Switch back to develop branch
git checkout develop

# 4. Create a tag on the current develop commit
git tag "$VERSION"

# 5. Push the tag to remote
git push origin "$VERSION"

echo "Release $VERSION created and pushed successfully!"
