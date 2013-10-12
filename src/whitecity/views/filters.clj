(ns whitecity.views.filters
  (:require [clojure.string :as s])
  (:use selmer.filters))

(add-filter! :empty? (fn [x] 
                       (if (string? x) (s/blank? x) (empty? x))))
