(ns whitecity.util.markdown
  (:use hiccup.core)
  (:require
   [markdown.core :as md]
   [clojure.string :as s]
   [whitecity.util.image :as image]
   [markdown.transformers :as mdts]))

(defn image-transform [text state]
  (loop [out []
         tokens (seq text)]
    (if (empty? tokens)
      [(s/join out) state]
      (let [tokens (seq text)
            [head xs]   (split-with (partial not= \[) tokens)
            [title ys]  (split-with (partial not= \]) xs)
            [dud zs]    (split-with (partial not= \() ys)
            [link tail] (split-with (partial not= \)) zs)]
        (if (or (< (count link) 2)
                (< (count tail) 1)
                (> (count dud) 1))
          (recur (concat out head title dud link) tail)
          (recur
           (into out
                 (let [alt (s/join (rest title))
                       [url title] (split-with (partial not= \space) (rest link))
                       title (s/join (rest title))]
                   (concat (butlast head) (image/img (s/join url) (s/join title) alt))))
           (rest tail)))))))


(defn input-transform [text state]
  (loop [out []
         tokens (seq text)]
    (if (empty? tokens)
      [(s/join out) state]
      (let [tokens (seq text)
            [label tail] (split-with (partial not= \=) tokens)]
        (if (< (count label) 1)
          (recur label tail)
          (recur
           (into out
                 (html [:input {:name label :type "text"}])))
          )
        ))

    ))

(defn md->html
    "reads a markdown string and returns the html"
    [string]
  (md/md-to-html-string string :replacement-transformers [mdts/empty-line
                                                          mdts/hr mdts/li
                                                          mdts/heading mdts/italics
                                                          mdts/em mdts/strong
                                                          mdts/bold mdts/strikethrough
                                                          mdts/superscript mdts/blockquote
                                                          mdts/paragraph image-transform
                                                          mdts/br]))
