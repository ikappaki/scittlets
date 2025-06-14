[![Catalog](https://img.shields.io/github/v/release/ikappaki/scittlets)](https://ikappaki.github.io/scittlets/) [![cli](https://img.shields.io/npm/v/scittlets.svg)](https://www.npmjs.com/package/scittlets)

> [!NOTE]
> This project is in α.
> Features and APIs are unstable and subject to change.

[Scittle](https://babashka.org/scittle/) brings the Small Clojure Interpreter to the browser, allowing you to run ClojureScript using simple `<script>` tags.

## Overview

A **scittlet** is a Clojurescript namespace designed for use with [Scittle](https://babashka.org/scittle/), with a clearly defined set of `<script>` dependencies that can be loaded in any Scittle HTML file.

**Scittlets** is a versioned catalog and repository of these modules, providing examples and instructions for loading from CDN into your Scittle file. It also includes a command-line tool (`scittlets`) to help manage scittlet dependencies in HTML files.

The repository offers the scaffolding needed to develop, test, showcase, and publish scittlets, served via [jsDelivr](https://www.jsdelivr.com/) from the [Scitlets GitHub project](https://github.com/ikappaki/scittlets) itself.

## Usage

Explore available scittlets in the [Web Catalog](https://ikappaki.github.io/scittlets/).

To use one:

1. Add its listed dependencies to your Scittle HTML file manually, or
2. Use the [scittlets](#CLI-scittlets) CLI command available via `npx` to manage dependencies easily.

Follow the usage instructions and demo code provided.

### CLI: scittlets

Manage scittlet dependencies in HTML files.

Use the CLI with `npx`, no install required. Optional global install: `npm install -g scittlets`.

Run commands with:
``` bash
npx scittlets <command> [options]
```

#### Commands
- `new [template] [--directory path]`

  Create a new app from the specified TEMPLATE. If no template is provided, lists available templates.

  Output directory defaults to the template's default directory but can be overridden with `--directory`.

- `add <html> [scittlets..] [--list-scittlets]`

  Add SCITTLETS dependencies to the target HTML file, or list them if none are provided.

  Use `--list-scittlets` (or `-l`) to list available scittlets from the catalog.

- `update <html> [scittlets..]`

  Update all scittlet dependencies in the HTML file at PATH using the latest catalog, or only those SCITTLETS if specified.

- `pack <html> [target]`

  Pack the HTML file by inlining `<script src="">` elements directly into the file.

  Saves the output to `target` (default: `packed.html`).

- `catalog [--release version]`

  List all scittlets and templates in the catalog for the specified catalog release version (default: `latest`).

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

Update dependencies in an HTML file:

```bash
npx scittlets update ./index.html
```

Pack an HTML file and specify output filename:
```bash
npx scittlets pack ./index.html output.html
```

#### Important

- To avoid GitHub API rate limits, set the environment variable `GITHUB_PUBLIC_TOKEN` (no scopes needed):
  ```bash
  export GITHUB_PUBLIC_TOKEN=your_token_here
  # or, on PowerShell
  $env:GITHUB_PUBLIC_TOKEN="your_token_here"
  ```

## Scittlets

### [scittlets.dev.nrepl](https://ikappaki.github.io/scittlets/test/scittlets/dev/nrepl.html)

Provides dependencies and a babashka script for opening up an nREPL connection to the Scittle app.

### [scittlets.reagent](https://ikappaki.github.io/scittlets/test/scittlets/reagent/basic.html)

Provides dependencies for [reagent](https://reagent-project.github.io/), a simple ClojureScript interface to [React](https://react.dev/).

### [scittlets.reagent.codemirror](https://ikappaki.github.io/scittlets/test/scittlets/reagent/codemirror.html)

A [reagent](https://reagent-project.github.io/) component around [CodeMirror](https://codemirror.net/), a code editor component for the web.

### [scittlets.reagent.mermaid](https://ikappaki.github.io/scittlets/test/scittlets/reagent/mermaid.html)

A [reagent](https://reagent-project.github.io/) component around [Mermaid](https://mermaid.js.org/), the diagramming and charting tool.

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
* Card: [test/scittlets/reagent/mermaid_card.cljs](test/scittlets/reagent/mermaid_card.cljs)
* UI Test: [test/scittlets/reagent/mermaid_test.cljs](test/scittlets/reagent/mermaid_test.cljs)
* Test page: [test/scittlets/reagent/mermaid.html](test/scittlets/reagent/mermaid.html)
* Demo code: [examples/mermaid/mermaid_demo.cljs](examples/mermaid/mermaid_demo.cljs)
* Demo page: [examples/mermaid/mermaid_demo.html](examples/mermaid/mermaid_demo.html)

## License

Licensed under the Eclipse Public License 1.0, same as [Scittle](https://github.com/babashka/scittle). See [LICENSE](LICENSE) for details.
