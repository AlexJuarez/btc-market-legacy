(ns whitecity.views.tags
  (:require [selmer.parser :refer [render add-tag!]]
            [noir.io :as noirio]
            [whitecity.cache :as cache]
            [ring.util.anti-forgery :refer [anti-forgery-field]]
            [whitecity.util.image :as image]
            [whitecity.util :as util]
            [environ.core :refer [env]])
  (:use selmer.filters
        hiccup.core))

(defn- parse-args [args]
  (into [] (map keyword (.split args "\\."))))

(defn- computed-args [args context-map]
  (map #(get-in context-map (parse-args %)) args))

(add-tag! :csrf-token (fn [_ _] (anti-forgery-field)))

(add-tag! :shipping-selectors
          (fn [args context-map]
            (let [args (computed-args args context-map)
                  select (map util/parse-int (second args))
                  recent (remove #{1} (sort (map #(:region_id %) (last args))))
                  common (remove (into #{} recent) [13 40 243 258])
                  regions (apply merge (map #(hash-map (:id %) (:name %)) (first args))) ;;remove undelared
                  regions-remaining (sort (keys (apply dissoc regions (concat [1] common recent))))
                  ]
              (str
               (html [:option (merge {:value 1} (if (some #{1} select) {:selected "selected"})) "Worldwide"])
               (when (not (empty? recent))
                 (html [:optgroup {:label "Recent"}
                        (map #(vector :option (merge {:value %} (if (some #{%} select) {:selected "selected"})) (regions %)) recent)
                        ]))
               (when (not (empty? common))
                 (html [:optgroup {:label "Common Countries"}
                        (map #(vector :option (merge {:value %} (if (some #{%} select) {:selected "selected"})) (regions %)) common)
                        ]))
              (html [:optgroup {:label "All Countries"}
               (map #(vector :option (merge {:value %} (if (some #{%} select) {:selected "selected"})) (regions %)) regions-remaining)
               ])
              ))))

(add-tag! :ifcontains (fn [args context-map content]
                        (let [args (computed-args args context-map)]
                          (if (some #(= (second args) (:region_id %)) (first args))
                            (render (get-in content [:ifcontains :content]) context-map)
                            "")
                          ))
          :endifcontains)

(add-tag! :image (fn [args context-map]
  (let [id (first (computed-args args context-map))]
    (image/create-image id "_max"))))

(add-tag! :image-thumbnail (fn [args context-map]
   (let [id (first (computed-args args context-map))]
     (image/create-image id "_thumb"))))

(add-tag! :load-styles
   (fn [args context-map]
     (let [route (first args)]
       (html [:style {:type "text/css"}
                      (if (:dev env)
                        (slurp (str (noirio/resource-path) route))
                        (cache/cache! route
                          (slurp (str (noirio/resource-path) route))))
                      ]))))
