(ns whitecity.models.exchange
  (:refer-clojure :exclude [get])
  (:use 
        [whitecity.db]
        [korma.core]
        [korma.db :only (transaction)]
        [clojure.string :only (split lower-case)])
  (:require
    [cheshire.core :as jr]
    [whitecity.cache :as cache]
    [whitecity.models.currency :as currency]
    [clj-http.client :as client]))

(defn update-from-remote []
  (let [response (jr/parse-string (slurp "resources/exchange_rates.json"))
              ;;(:body (client/get "https://coinbase.com/api/v1/currencies/exchange_rates" 
              ;;{:conn-timeout 1000
              ;; :content-type :json
              ;; :follow-redirects false
              ;; :as :json
              ;; :accept :json}))
        currencies (apply merge (map #(assoc {} (lower-case (:key %)) (:id %)) (currency/all)))
        prep (map #(let [s (split (str (key %)) #"_")] {:from (currencies (.substring (first s) 0 3)) :to (currencies (.substring (last s) 0 3)) :value (Float/parseFloat (val %))}) response)]
    (if-not (empty? response)
      (do
        (dorun (pmap #(cache/get-set (str (:from %) "-" (:to %)) (:value %)) prep))
        (transaction
          (delete exchange)
          (insert exchange
                  (values prep)))))))

(defn get [from to]
  (when-not (or (nil? from) (nil? to))
    (cache/get-set (str from "-" to)
      (do (-> (Thread. update-from-remote) .start) 
        (:value (first (select exchange
              (where {:from from :to to}))))))))
