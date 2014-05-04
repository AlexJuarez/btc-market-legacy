(ns whitecity.models.audit
  (:refer-clojure :exclude [get])
  (:use [korma.core]
        [whitecity.db])
  (:require
        [whitecity.util :as util]))

(defn all [user-id]
  (select audits
          (where {:user_id user-id})
          (limit 20)))
