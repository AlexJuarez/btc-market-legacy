(ns whitecity.routes.moderator
  (:use
    [compojure.core :only [GET POST]]
    [noir.util.route :only (def-restricted-routes)]
    [whitecity.helpers.route])
  (:require [whitecity.views.layout :as layout]
            [whitecity.models.order :as order]
            [whitecity.models.review :as review]
            [whitecity.util.hashids :as hashids]
            [whitecity.models.resolution :as resolution]
            [noir.response :as resp]
            [whitecity.util :as util]))

(defn moderator-page [] "")

(def-restricted-routes moderator-routes
  (GET "/market/moderators" [] (moderator-page)))
