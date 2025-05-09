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

1. Add its listed dependencies to your Scittle app HTML file.
2. Follow the usage instructions and demo code provided.

## Scittlets

### [scittlet.reagent.mermaid](https://ikappaki.github.io/scittlets/test/scittlets/reagent/mermaid.html)

A [reagent](https://reagent-project.github.io/) component around [mermaid](https://mermaid.js.org/), the diagramming and charting tool.

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

Scittlets are listed in [catalog.js](catalog.js), the metadata registry.

### Example Scittlet: `scittlet.reagent.mermaid`

Use this scittlet as a starting point for development:
* Code: [src/scittlets/reagent/mermaid.cljs](src/scittlets/reagent/mermaid.cljs)
* Metadata: [catalog.js](catalog.js)
* Test: [test/scittlets/reagent/mermaid_test.cljs](test/scittlets/reagent/mermaid_test.cljs)
* Test page: [test/scittlets/reagent/mermaid.html](test/scittlets/reagent/mermaid.html)
* Demo code: [examples/mermaid/mermaid_demo.cljs](examples/mermaid/mermaid_demo.cljs)
* Demo page: [examples/mermaid/mermaid_demo.html](examples/mermaid/mermaid_demo.html)
