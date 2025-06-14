(ns scittle-basic)

(let [colors ["#6a8b32" "#c27c7b" "#6a7fb3"]
      box (.createElement js/document "div")]

  ;; Style the box
  (set! (.-textContent box) "✨ Welcome to Scittle! ✨ Click me!")
  (set! (.-id box) "welcome")
  (doto (.-style box)
    (.setProperty "padding" "20px")
    (.setProperty "margin" "40px auto")
    (.setProperty "width" "300px")
    (.setProperty "text-align" "center")
    (.setProperty "font-weight" "bold")
    (.setProperty "border-radius" "12px")
    (.setProperty "background-color" (rand-nth colors))
    (.setProperty "color" "#fff")
    (.setProperty "box-shadow" "0 4px 8px rgba(0,0,0,0.2)")
    (.setProperty "transition" "background-color 0.5s ease, transform 0.15s ease")
    (.setProperty "user-select" "none")
    (.setProperty "cursor" "pointer"))

  ;; Click animation and color change
  (.addEventListener box "click"
    (fn []
      (let [new-color (rand-nth colors)]
        (set! (.-backgroundColor (.-style box)) new-color)
        (set! (.-transform (.-style box)) "scale(0.95)")
        (js/setTimeout #(set! (.-transform (.-style box)) "scale(1)") 150))))

  ;; Add it to the body
  (.appendChild js/document.body box))
