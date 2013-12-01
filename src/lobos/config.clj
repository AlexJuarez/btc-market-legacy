(ns lobos.config
  (:use lobos.connectivity)
    (:require [whitecity.db :as db]))

(open-global db/db-spec)
