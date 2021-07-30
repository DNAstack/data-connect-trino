#!/bin/sh

config_path=BOOT-INF/classes/static/assets
mkdir -p ${config_path}

echo "{
  \"apiUrl\": \"${SEARCH_API_URL}\"
}" > ${config_path}/appConfig.json

jar uf app.jar ${config_path}/appConfig.json
rm -rf BOOT-INF

java -jar app.jar
