(ns whitecity.views.filters
  (:require [clojure.string :as s]
            [noir.session :as session]
            [whitecity.cache :as cache]
            [whitecity.models.region :as regions]
            [whitecity.util :as util])
  (:use selmer.filters
        hiccup.core))

(add-filter! :empty? (fn [x] 
                       (if (string? x) (s/blank? x) (empty? x))))

(add-filter! :count-cart
             (fn [x]
               (:quantity ((session/get :cart) x))))

(add-filter! :postage-cart
             (fn [x]
               (:postage ((session/get :cart) x))))

(defn paginate [page maxpage]
  (loop [c 1 o [page]]
    (if (or (> c 5) (>= (count o) 5))
      o
      (let [pageup (+ page c)
            pagedown (- page c)]
        (recur (inc c) (concat (when (> pagedown 0) [pagedown]) o (when (<= pageup maxpage) [pageup])))))))

(add-filter! :pagination
             (fn [x]
               (let [page (if (nil? (:page x)) 1 (util/parse-int (:page x)))
                     m (:max x)
                     params (or (:params x) {})
                     pages (paginate page m)
                     url (:url x)]
                 [:safe (html 
                   [:ul.pagination
                    [:li [:a {:href (str url "?" (util/params (assoc params :page 1)))} "&laquo;"]]
                    (map #(-> [:li (if (= page %) [:strong.selected %] [:a {:href (str url "?" (util/params (assoc params :page %)))} %])]) pages)
                    [:li [:a {:href (str url "?" (util/params (assoc params :page m)))} "&raquo;"]]
                    ])])))
                    ;;[:li [:a {:href (str url "?page=")} ]]]))))

(defn render-tree [tree params]
  (let [children (:children tree)]
    (if-not (empty? children)
      [:li 
       [:a.category {:href (str "/market/category/" (:id tree) params)} (:name tree)] " " [:span.count (:count tree)] 
       [:ul (map #(render-tree % params) children)]]
      [:li
       [:a.category {:href (str "/market/category/" (:id tree) params)} (:name tree)] " " [:span.count (:count tree)]])))

(add-filter! :render-tree
             (fn [x]
               (let [tree (:tree x)
                     params (if-not (empty? (:params x)) (str "?" (util/params (:params x))))]
                 [:safe (html [:ul (render-tree tree params)])])))

(add-filter! :status
             (fn [x]
               (let [status ["processing" "shipping" "in resolution" "finailized"]]
                 (status x))))

(add-filter! :region
             (fn [x]
               (let [regions (cache/cache! "regions_map" (into {} (map #(vector (:id %) (:name %)) (regions/all))))]
                 (regions x))))
