# Contributing

## Scripts

Install dev dependencies:
```bash
$ npm install
```

Several Node scripts can be run with npx. Some require `bb` ([babaskha](https://babashka.org/)):

- `npm run scittlets -- <options>`: run the dev version of the `scittlets` CLI.
- `npm run tests`: run CLI tests.
- `npm run test-ui`: run UI tests.
- `npm run html-gen`: update site index pages.

## Scittlets

```
.
├── catalog.json                  (C)
├── api                           (A)
│   └── scittlets
│       ├── reagent
│       │   ├── mermaid.html      (1)
│       │   ├── mermaid_card.cljs (1)
│       │   └── ...
│       └── ... 
│
├── src                           (S)
│   └── scittlets
│       ├── reagent
│       │   ├── mermaid.cljs      (2)
│       │   └── ...
│       └── ...
└── test                          (T)
    └── scittlets
        └── reagent
            ├── mermaid_test.clj  (3)
            └── ...
```

Ⓒ `catalog.json`: scittlets registry

Ⓐ `api/`: API documentation and demo page

① API docs + playground

Ⓢ `src/`: scittlets source code

② Example: `mermaid.cljs`

Ⓣ `test/`: scittlets tests

③ Example UI test: `mermaid_test.clj`

A scittlet is a ClojureScript namespace listed in the catalog. Example entry:
 
```json
 "scittlets.reagent.mermaid" :
 {"home": "https://github.com/ikappaki/scittlets",
  "descr": "A Reagent component around around Mermaid, the diagramming and charting tool",
  "deps": ["<script src=\"https://cdn.jsdelivr.net/npm/react@18/umd/react.production.min.js\"></script>",
           "<script src=\"https://cdn.jsdelivr.net/npm/react-dom@18/umd/react-dom.production.min.js\"></script>",
           "<script src=\"https://cdn.jsdelivr.net/npm/scittle@0.7.23/dist/scittle.reagent.min.js\"></script>",
           "<script src=\"https://cdn.jsdelivr.net/npm/mermaid@11.6.0/dist/mermaid.min.js\"></script>",
           "<script src=\"src/scittlets/reagent/mermaid.cljs\" type=\"application/x-scittle\"></script>"],
  "api" : "api/scittlets/reagent/mermaid.html",
  "see" : {"mermaid" : "https://mermaid.js.org/",
           "reagent" : "https://reagent-project.github.io/"
          }},

```


- Source: [src/scittlets/reagent/mermaid.cljs](src/scittlets/reagent/mermaid.cljs).
- API docs: [api/scittlets/reagent](api/scittlets/reagent).
- UI test (using [Etaoin](https://github.com/clj-commons/etaoin)): [test/scittlets/reagent/mermaid_test.clj](test/scittlets/reagent/mermaid_test.clj).

Suggested workflow:

1. Create the API doc page including dependencies.
2. Add an empty scittlet source file.
3. Start development using [cljs-josh](https://github.com/chr15m/cljs-josh), navigate to the API page.

```shell
$ npx josh
```

## Templates

```
.
├── catalog.json               (C)
├── templates                  (T)
│   ├── mermaid
│   │   ├── mermaid.html       (1)
│   │   └── mermaid.cljs       (1)
│   └── ...
└── test
    └── cli
        └── templates_test.clj (S)
```

Ⓒ `catalog.json`: templates registry

Ⓣ `templates/`: store

① Example: `mermaid` template

⑤ Template test

A template is typically an HTML file with dependencies plus a ClojureScript entry point.
The Mermaid example includes:

- [templates/mermaid/mermaid.html](templates/mermaid/mermaid.html): loads Scittle, the `scittlets.reagent.mermaid` scittlet, and template code
- `mermaid.cljs`: requires Reagent and the scittlet, then renders a sample diagram


A template is an HTML file plus dependencies. Example:  includes Scittle `scittle.min.js`, the `scittlets.reagent.mermaind` scittlet and the [templates/mermaid/mermaid.cljs](templates/mermaid/mermaid.cljs), which renders a sample diagram.
 
The template is registered in[catalog.json](catalog.json) under `templates`. Example mermaid entry:

```json
{
  ...
  "templates" :
  {"reagent/mermaid":
   {"name": "Mermaid Reagent",
    "src": "templates/mermaid",
    "descr": "A Reagent template around Mermaid, the diagramming and charting tool",
    "files": [{"src": "mermaid.html", "dest": "index.html"},
              "mermaid.cljs"],
    "target": "scits_reagent_mermaid"},
   ...
  }
}
```

where

- `src`: the src directory in the repo where the template can be found
- `files`: copied on instantiation
- `target`: default output directory

Instantiate with

```bash
$ npm run scittlets -- new reagent/mermaid -r ./catalog.json
```

Add a test in [test/cli/templates_test.clj](test/cli/templates_test.clj) to confirm successful load.
