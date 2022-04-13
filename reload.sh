#!/bin/bash
set -e
rm -rf api-jars-new
mkdir api-jars-new
echo --- collect all api jars
for folder_name in ../*-service; do
  service_name=${folder_name%*-service}
  for api_jar in $folder_name/service/target/$service_name-service/lib/$service_name-api-*.jar; do
    if [ -f "$api_jar" ]; then
      cp $api_jar api-jars-new/
    fi
  done
done
echo --- these api jars found
ls api-jars-new/
echo --- verify the new collected api jars
docker run --rm -v "$PWD":"$PWD" -w "$PWD" api-gateway-verifier:2.1 api-jars-new/
echo --- sync the new api jars
rsync -r --delete -v api-jars-new/ api-jars/
echo --- reload gateway
docker-compose restart