#! /bin/bash

go run catalogs.go --file ../data/stop_times.txt --topic cat_stop_times --broker localhost:9092
go run catalogs.go --file ../data/stops.txt --topic cat_stops --broker localhost:9092
go run catalogs.go --file ../data/routes.txt --topic cat_routes --broker localhost:9092

