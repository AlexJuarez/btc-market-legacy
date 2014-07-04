(ns whitecity.views.layout
  (:require [selmer.parser :as parser]
            [clojure.string :as s]
            [ring.util.response :refer [content-type response]])
  (:use whitecity.views.filters)
  (:import compojure.response.Renderable))

(defonce template-path "whitecity/views/templates/")

(deftype RenderableTemplate [template params]
  Renderable
  (render [this request]
    (content-type
      (->> (assoc params
                  (keyword (s/replace template #".html" "-selected")) "active")
        (parser/render-file (str template-path template))
        response)
      "text/html; charset=utf-8")))

(defn render [template & [params]]
  (RenderableTemplate. template params))
