> [!NOTE]
> Experimental, Proof Of Concept.

[Scittle](https://babashka.org/scittle/) brings the Small Clojure Interpreter to the browser, allowing you to run ClojureScript using simple `<script>` tags.

## Overview

A **scittlet** is a Scittlet namespace with a defined set of `<script>` dependencies that can be loaded in a Scittle Web App.

**Scittlets** is a catalog and repository of such scittlets, offering usage examples and instructions on how to load them into your Scittle web app directly from a CDN.

This repository provides the structure for developing, testing, showcasing, and publishing scittlets, served from this GitHub project behind [jsDelivr](https://www.jsdelivr.com/).

## Usage

Visite the [Scittlets Web Catalog](https://ikappaki.github.io/scittlets/) to explore available scittlets.

To use one:

1. Add its listed dependencies to your Scittle app.
2. Follow the usage instructions and demo code provided.

## Development

Install dependencies
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

### Example Scittlet: `scittlet.reagent.mermaid`

Use this as a starting point:
* Scittlet Code: [src/scittlets/reagent/mermaid.cljs](src/scittlets/reagent/mermaid.cljs)
* HTML page: [test/scittlets/reagent/mermeaid.html](test/scittlets/reagent/mermeaid.html)
* Demo Web App: [test/scittlets/reagent/mermeaid_test.cljs](test/scittlets/reagent/mermeaid_test.cljs)

Demo available at https://ikappaki.github.io/scittlets/.

Scittlets are listed in the catalog file: [catalog.js](catalog.js), which serves as the metadata registry for all published scittlets.
