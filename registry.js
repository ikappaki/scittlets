var scittlets = {
  "scittlets.reagent.mermaid" :
  {"deps" : ["<script src=\"https://cdn.jsdelivr.net/npm/react@18/umd/react.production.min.js\"></script>",
             "<script src=\"https://cdn.jsdelivr.net/npm/react-dom@18/umd/react-dom.production.min.js\"></script>",
             "<script src=\"https://cdn.jsdelivr.net/npm/scittle@latest/dist/scittle.reagent.min.js\"></script>",
             "<script src=\"https://cdn.jsdelivr.net/npm/mermaid@11.6.0/dist/mermaid.min.js\"></script>",
             "<script src=\"https://cdn.jsdelivr.net/gh/ikappaki/scittlets/src/scittlets/reagent/mermaid.cljs\" type=\"application/x-scittle\"></script>"]}

   };

if (typeof module !== 'undefined' && module.exports) {
  // Node.js: Export the data
  module.exports = scittlets;
} else {
  // Browser: Assign to a global variable
  window.scittlets_metadata = scittlets;
}
