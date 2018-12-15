#!/usr/bin/env bash

set -xe

readonly ci_registry_image="${REGISTRY_IMAGE:-${CI_REGISTRY_IMAGE}}"
readonly full_image_name="${ci_registry_image:-xview-cluster}"

if [[ ! -z "$CI" ]]; then
  echo "=================== CI ==================="
  cp "$CI_PROJECT_DIR/ci/.sbtopts" "$CI_PROJECT_DIR"
fi

echo "registry image set to $full_image_name"

sbt clean docker:stage

docker build -t xview-cluster/chipper chipper/target/docker/stage

# hack in the group id mod for openshift to reduce image size
if [[ $(id -u) -eq 0 ]]; then
  echo "=== performing group modifications ==="
  chgrp -R 0 cluster/target/docker/stage/opt
  chmod -R g+rwX cluster/target/docker/stage/opt
fi

# build the cluster image
docker build -t xview-cluster cluster/target/docker/stage

if [[ "$full_image_name" != xview-cluster ]]; then
  # tag pdal-cluster
  docker tag xview-cluster "$full_image_name"
  docker tag xview-cluster/chipper "$full_image_name/chipper"

  # clean up
  docker rmi xview-cluster
  docker rmi xview-cluster/chipper
fi
