[![Catalog](https://img.shields.io/github/v/release/ikappaki/scittlets)](https://ikappaki.github.io/scittlets/) [![cli](https://img.shields.io/npm/v/scittlets.svg)](https://www.npmjs.com/package/scittlets)

> [!NOTE]
> This project is in β.
> Features and APIs are subject to change.

👉 **[Visit the Scittlets website](https://ikappaki.github.io/scittlets/)** to browse components with live demos, copy installation commands, and explore starter templates.

[Scittle](https://babashka.org/scittle/) brings the Small Clojure Interpreter to the browser, allowing you to run ClojureScript using simple `<script>` tags.

**Scittlets** (short for *Scittle applets*) is a catalog of ready-made components and starter templates for bulding Scittle apps, covering charting, editing, UI, and developer tools.

## Overview

A **scittlet** is a ready to use ClojureScript component designed for adding functionality to [Scittle](https://babashka.org/scittle/) apps, with its JavaScript dependencies that can be loaded in any Scittle HTML file.

Each component includes examples and instructions for loading from CDN into your Scittle apps. The CLI tool helps organize and scaffold your projects.

The repository offers the scaffolding needed to develop, test, showcase, and publish scittlets, served via [jsDelivr](https://www.jsdelivr.com/) from this GitHub repository.

## Getting Started

**New to Scittle?** Create a starter app from a template with the CLI:
```bash
npx scittlets new
```

**Add components to your Scittle app:**
1. **Using the CLI:** Manage components automatically with the Scittlets CLI (see below)
2. **Manual setup:** Browse components on the [Scittlets Catalog](https://ikappaki.github.io/scittlets/scittlets.html) and copy script tags into your HTML file

Each component includes live demos and copy-paste code examples.

## CLI: scittlets

The `scittlets` CLI helps you create, manage, and pack Scittle apps and components.

Install globally with npm (requires [Node.js and npm](https://nodejs.org/)): `npm install -g scittlets`

Run commands with:
``` bash
npx scittlets <command> [options]
```

#### Commands
- `new [template] [--directory path]`

  Create a new app from the specified TEMPLATE. If no template is provided, lists available templates.

  Output directory defaults to the template's default directory but can be overridden with `--directory` (or `-d`).

- `add <html> [scittlets..] [--list-scittlets]`

  Add SCITTLETS dependencies to the target HTML file, or list them if none are provided.

  Use `--list-scittlets` (or `-l`) to list available scittlets from the catalog.

- `update <html> [scittlets..]`

  Update all scittlet dependencies in the HTML file at PATH using the latest catalog, or only those SCITTLETS if specified.

- `pack <html> [target]`

  Pack the HTML file by inlining `<script src="">` elements directly into the file.

  Saves the output to `target` (default: `packed.html`).

- `catalog [--release version]`

  List all scittlets and templates in the catalog for the specified catalog release VERSION (default: `latest`).

- `releases`

  List all published release versions of the scittlets catalog.

#### Example usage
List available templates:
```bash
npx scittlets new
```

Create a new app with a specific template:

```bash
npx scittlets new reagent/codemirror
```

Add scittlet dependencies to HTML file:

```bash
npx scittlets add ./index.html scittlets.reagent.codemirror

```

List catalog release versions:

```bash
npx scittlets releases
```

List scittlets and templates for the latest release version:
```bash
npx scittlets catalog
```

Update all scittlets dependencies in an HTML file to their latest versions:

```bash
npx scittlets update ./index.html
```

Pack an HTML file and specify output filename:
```bash
npx scittlets pack ./index.html output.html
```

## Contributing & Development

First, clone the repository:

```bash
git clone https://github.com/ikappaki/scittlets.git
cd scittlets
```

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
* Card: [test/scittlets/reagent/mermaid_card.cljs](test/scittlets/reagent/mermaid_card.cljs)
* UI Test: [test/scittlets/reagent/mermaid_test.cljs](test/scittlets/reagent/mermaid_test.cljs)
* Test page: [test/scittlets/reagent/mermaid.html](test/scittlets/reagent/mermaid.html)
* Demo code: [examples/mermaid/mermaid_demo.cljs](examples/mermaid/mermaid_demo.cljs)
* Demo page: [examples/mermaid/mermaid_demo.html](examples/mermaid/mermaid_demo.html)

## License

Licensed under the Eclipse Public License 1.0, same as [Scittle](https://github.com/babashka/scittle). See [LICENSE](LICENSE) for details.
