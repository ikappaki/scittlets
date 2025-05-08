// The Scittlets catalog
var scittlets = {
  "version" : "main",
  "scittlets.reagent.mermaid" :
  {"home": "https://github.com/ikappaki/scittlets",
   "deps" : ["<script src=\"https://cdn.jsdelivr.net/npm/react@18/umd/react.production.min.js\"></script>",
             "<script src=\"https://cdn.jsdelivr.net/npm/react-dom@18/umd/react-dom.production.min.js\"></script>",
             "<script src=\"https://cdn.jsdelivr.net/npm/scittle@latest/dist/scittle.reagent.min.js\"></script>",
             "<script src=\"https://cdn.jsdelivr.net/npm/mermaid@11.6.0/dist/mermaid.min.js\"></script>",
             "<script src=\"https://cdn.jsdelivr.net/gh/ikappaki/scittlets/src/scittlets/reagent/mermaid.cljs\" type=\"application/x-scittle\"></script>"],
   "see" : {"mermaid" : "https://mermaid.js.org/",
            "reagent" : "https://reagent-project.github.io/",
            }}

   };

if (typeof module !== 'undefined' && module.exports) {
  module.exports = scittlets;   // Node

} else {
  window.scittlets_metadata = scittlets; // browser
}
