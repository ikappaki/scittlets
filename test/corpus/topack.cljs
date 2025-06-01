(require '[reagent.dom :as rdom]
         '[scittlets.reagent.mermaid :refer [mermaid+]])

(rdom/render
 [mermaid+ "journey
    title My working day
    section Go to work
      Make tea: 5: Me
      Go upstairs: 3: Me
      Do work: 1: Me, Cat
    section Go home
      Go downstairs: 5: Me
      Sit down: 5: Me"]

 (.getElementById js/document "app"))

