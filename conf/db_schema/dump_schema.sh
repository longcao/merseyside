#!/bin/sh

mysqldump -u root -p --no-data --skip-comments --databases merseyside > merseyside_schema.sql