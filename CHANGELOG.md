# Changelog

**Note:** `v` versions is for Catalog, `cli-v` is for npm CLI releases.

## Unreleased Catalog

* Added scittlets.reagent.codemirror component

## Unreleased CLI

## cli-v0.2.1

* Fixed issue with scittlets binary not recognising options (#5)

## cli-v0.2.0

* Added `html-local-deps-update.cljs` script to sync local html files in bulk from the local catalog (#3)
* Renamed `scittltes`'s `list` command to `catalog`, and added `--rewrite` option to rewrite src deps to jsDelivr URLs (#3)
* Changed `scittlets` behavior to update all dependencies in an HTML by default, or filter to those specified as arguments(#3)
* Changed `scittlets` TAG argument to also accept a local catalog file path (#3)
* Added CLI tests for the `scittlets` script (#3)

## cli-v0.1.0

* Migrated `deps_update` script to a standalone npm `scittlets` cli tool (#1).

## v0.1.0

* Created Scittlets framework.
* Added `scittlet.reagent.mermaid`.
