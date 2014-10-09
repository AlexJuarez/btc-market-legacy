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
      (let [[head xs]   (split-with (partial not= \[) tokens)
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

(defn radio [label text selected]
  [:label [:input (conj {:name label :type "radio"} (when selected {:checked "checked"}))] (s/trim text)])

(defn checkbox [label text selected]
  [:label [:input (conj {:name label :type "checkbox"} (when selected {:checked "checked"}))] (s/trim text)])

(defn option [text]
  (let [options (s/split text #"-&gt;")
        value (s/trim (first options))
        selected (and (= (first value) \() (= (last value) \)))
        label (or (second options) value)]
    [:option (conj {:value (if selected (s/join (rest (drop-last value))) value) :label label} (when selected {:selected "selected"}))]))

(defn input-transform [text state]
  (let [[label tail] (split-with (partial not= \=) (seq text))
        tail (rest tail)
        tails (s/trim (s/join tail))
        label (s/trim (s/join label))]
    (if (empty? label)
      [text state]
      (cond
       (and (empty? (drop-while #{\_ \space} tail))
            (= (count (remove #{\space} tail)) 3)) [(html [:label label [:br] [:input {:name label :type "text"}]]) state]
       (> (count (re-seq #"\( \)|\(x\)+" tails)) 1)
         [(html (map #(if (not (empty? %)) (if (= (take 3 %) (seq "(x)")) (radio label (s/join (drop 3 %)) true) (radio label % false))) (s/split tails #"\( \)"))) state]
       (> (count (re-seq #"\[ \]|\[x\]+" tails)) 1)
         [(html (map #(if (not (empty? %)) (if (= (take 3 %) (seq "[x]")) (checkbox label (s/join (drop 3 %)) true) (checkbox label % false))) (s/split tails #"\[ \]"))) state]
       (and (= (first tails) \{) (= (last tails) \})) [(html [:label label [:select {:name label} (map #(option %) (s/split (s/join (rest (drop-last tails))) #","))]]) state]
       :else [text state]
       )
      )
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
                                                          input-transform
                                                          image-transform
                                                          mdts/paragraph
                                                          mdts/br]))
