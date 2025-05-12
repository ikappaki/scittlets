[![Latest Release](https://img.shields.io/github/v/release/ikappaki/scittlets)](https://ikappaki.github.io/scittlets/)

> [!NOTE]
> Experimental, Proof Of Concept.

[Scittle](https://babashka.org/scittle/) brings the Small Clojure Interpreter to the browser, allowing you to run ClojureScript using simple `<script>` tags.

## Overview

A **scittlet** is a Clojurescript namespace designed for use with [Scittle](https://babashka.org/scittle/), with a clearly defined set of `<script>` dependencies that can be loaded in any Scittle HTML file.

**Scittlets** is a versioned catalog and repository of these modules, providing examples and instructions for loading from CDN into your Scittle file.

This repository offers the scaffolding needed to develop, test, showcase, and publish scittlets, served via [jsDelivr](https://www.jsdelivr.com/) from this GitHub project.

## Usage

Explore available scittlets in the [Web Catalog](https://ikappaki.github.io/scittlets/).

To use one:

1. Add its listed dependencies to your Scittle HTML file manually, or use the [deps_update](#scriptsdeps_updatecljs) script.
2. Follow the usage instructions and demo code provided.

## Scittlets

### [scittlet.reagent.mermaid](https://ikappaki.github.io/scittlets/test/scittlets/reagent/mermaid.html)

A [reagent](https://reagent-project.github.io/) component around [mermaid](https://mermaid.js.org/), the diagramming and charting tool.

## Scripts

### scripts/deps_update.cljs

This [Nbb](https://github.com/babashka/nbb) script manages Scittlet dependencies in HTML files or lists available versions and scittlets from the Catalog:
```bash
$ npx nbb scripts/deps_update.cljs -h

Usage: npx nbb <script> [-h] [version [scittlet file]]
  List catalog versions and scittlets, or update an HTML file with scittlet dependencies.

  No arguments:                List catalog versions from GitHub releases.
                               (Set GITHUB_PUBLIC_TOKEN to avoid API rate limits, no scopes needed)
  <version>:                   List available scittlets in the given VERSION.
                               (":latest" refers to the most recent version available)
  <version> <scittlet>:        List metadata of the SCITTLET in VERSION.
  <version> <scittlet> <file>: Update the HTML FILE with dependencies for the SCITTLET in VERSION.
```

The first time you try to add a scittlet dependency to an HTML file, the script will prompt you to insert dependency markers inside the `<head>` section:
```bash
$ npx nbb scripts/deps_update.cljs :latest scittlets.reagent.mermaid d:/scittlets-slides/index.html
...
Scittlet markers not found in HTML file for: scittlets.reagent.mermaid 

 Please place the following empty markers inside the <HEAD> of the HTML file, then rerun the script:

  <!-- Scittlet dependencies: scittlets.reagent.mermaid -->
  <!-- Scittlet dependencies: end -->

 Ensure this block appears after the Scittle script tag, which typically looks like:
   <script src="https://cdn.jsdelivr.net/npm/scittle@latest/dist/scittle.min.js" type="application/javascript"></script>
```

Once markers are present, the script will insert or update the required script deps and metadata:
```bash
$ npx nbb scripts/deps_update.cljs :latest scittlets.reagent.mermaid d:/scittlets-slides/index.html
...
{:deps/updating
 ("    <meta name=\"scittlets.reagent.mermaid.version\" content=\"v0.1.0b2\">"
  "    <script src=\"https://cdn.jsdelivr.net/npm/react@18/umd/react.production.min.js\"></script>"
  "    <script src=\"https://cdn.jsdelivr.net/npm/react-dom@18/umd/react-dom.production.min.js\"></script>"
  "    <script src=\"https://cdn.jsdelivr.net/npm/scittle@latest/dist/scittle.reagent.min.js\"></script>"
  "    <script src=\"https://cdn.jsdelivr.net/npm/mermaid@11.6.0/dist/mermaid.min.js\"></script>"
  "    <script src=\"https://cdn.jsdelivr.net/gh/ikappaki/scittlets@v0.1.0b2/src/scittlets/reagent/mermaid.cljs\" type=\"application/x-scittle\"></script>")}

:deps/updated d:/scittlets-slides/index.html scittlets.reagent.mermaid
```

## Development

Install dev dependencies:
```bash
$ npm install
```

Run the excellent [cljs-josh](https://github.com/chr15m/cljs-josh) live-reloading Scittle server:
```bash
$ npx josh
Serving ./ on port 8000:
- http://192.168.1.100:8000
- http://127.0.0.1:8000
SSE connection established
```

Scittlets are listed in [catalog.json](catalog.json), the metadata registry.

### Example Scittlet: `scittlet.reagent.mermaid`

Use this scittlet as a starting point for development:
* Code: [src/scittlets/reagent/mermaid.cljs](src/scittlets/reagent/mermaid.cljs)
* Metadata: [catalog.json](catalog.json)
* Test: [test/scittlets/reagent/mermaid_test.cljs](test/scittlets/reagent/mermaid_test.cljs)
* Test page: [test/scittlets/reagent/mermaid.html](test/scittlets/reagent/mermaid.html)
* Demo code: [examples/mermaid/mermaid_demo.cljs](examples/mermaid/mermaid_demo.cljs)
* Demo page: [examples/mermaid/mermaid_demo.html](examples/mermaid/mermaid_demo.html)
