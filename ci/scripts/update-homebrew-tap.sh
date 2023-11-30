#!/bin/bash
set -e

version=$( cat artifactory-repo/build-info.json | jq -r '.buildInfo.modules[0].id' | sed 's/.*:.*:\(.*\)/\1/' )

git clone homebrew-tap-repo updated-homebrew-tap-repo > /dev/null

if [[ $LATEST_GA = true ]]; then
pushd updated-homebrew-tap-repo > /dev/null
  curl https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-cli/${version}/spring-boot-cli-${version}-homebrew.rb --output spring-boot-cli-${version}-homebrew.rb
  rm spring-boot.rb
  mv spring-boot-cli-*.rb spring-boot.rb
  git config user.name "Spring Builds" > /dev/null
  git config user.email "spring-builds@users.noreply.github.com" > /dev/null
  git add spring-boot.rb > /dev/null
  git commit -m "Upgrade to Spring Boot ${version}" > /dev/null
  echo "DONE"
popd > /dev/null
fi
