#!/bin/sh

gu install native-image
./mvnw clean install -Pnative --batch-mode --quiet