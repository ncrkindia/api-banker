#!/bin/bash

# ==============================================================================
# Script Name : diff.sh
# Description : Compares the current codebase or develop branch against a specified
#               git version tag (or auto-detects the version from pom.xml).
#
# USAGE INSTRUCTIONS:
#   1. Auto-detect version from pom.xml:
#      $ ./scripts/diff.sh
#      $ bash scripts/diff.sh
#
#   2. Compare against a specific version tag:
#      $ ./scripts/diff.sh v1.5.0-beta
#      $ ./scripts/diff.sh 1.5.0-beta
#
#   3. Redirect output to a file for analysis or AI processing:
#      $ ./scripts/diff.sh > diff_output.txt
#
# ------------------------------------------------------------------------------
# PROMPT TO RE-USE IN ANTIGRAVITY:
# (Copy & paste the prompt below along with the diff output into Antigravity)
#
# "You are Sr Software Developer . Please run and analyze the git diff.sh script output , comparing
#  the release version tag with the current develop branch.
#
#  Execute the following tasks:
#  1. ANALYZE DIFFERENCES: Carefully analyze all code, UI, and logic changes introduced in this diff.
#     - check all the java file changes
#     - check all the resources file changes
#     - Increse version and do below updates-
#  2. UPDATE JAVADOC: Review modified Java classes and ensure JavaDoc comments (author, version, class/method descriptions) are updated appropriately wherever applicable with increased version.
#  3. UPDATE DOCUMENTATION: Update README.md, Feature.md, AboutUs.md,UserGuide.md and feature listings with the new features, bug fixes, and version increment wherever applicable.
#  4. GENERATE GIT COMMIT MESSAGE: Create a structured, professional Git commit message following Conventional Commits format (e.g., feat/fix/docs/refactor) summarizing all changes.
#
# ==============================================================================

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
    echo "Example: ./scripts/diff.sh v1.5.0-beta"
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