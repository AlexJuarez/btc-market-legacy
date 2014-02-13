(ns whitecity.models.resolution
  (:use [korma.db :only (defdb)]
        [korma.core]
        [whitecity.db])
  (:require 
        [whitecity.validator :as v]
        [whitecity.util :as util]))

(defn all [order-id user-id]
  (select resolutions
          (where (and {:order_id (util/parse-int order-id)} (or {:user_id user-id} {:seller_id user-id})))))
