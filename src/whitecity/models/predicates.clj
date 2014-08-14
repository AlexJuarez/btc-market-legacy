(ns whitecity.models.predicates
  (:require
        [korma.sql.engine :as eng]))

(defn ilike [k v]
  (eng/infix k "ILIKE" v))
