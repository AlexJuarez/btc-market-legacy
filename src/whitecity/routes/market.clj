(ns whitecity.routes.market
  (:use compojure.core)
  (:require [whitecity.views.layout :as layout]
            [whitecity.util :as util]))

(defn home-page []
  (layout/render
    "home.html" {:content (util/md->html "/md/docs.md")}))

(defn about-page []
  (layout/render "about.html"))

(defroutes market-routes
  (GET "/market/" [] (home-page))
  (GET "/market/about" [] (about-page)))
