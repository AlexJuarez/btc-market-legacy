(ns lobos.config
  (:use lobos.connectivity)
    (:require [whitecity.models.schema :as schema]))

(open-global schema/db-spec)
