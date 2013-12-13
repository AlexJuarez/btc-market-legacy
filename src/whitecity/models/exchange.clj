(ns whitecity.models.exchange
  (:use [cheshire.core :as jr]
        [whitecity.db]
        [korma.core]
        [korma.db :only (transaction)]
        [clojure.string :only (split)])
  (:require
    [whitecity.cache :as cache]
    [clj-http.client :as client]))

(defn update-from-remote []
  (let [response (jr/parse-string (slurp "resources/exchange_rates.json"))
              ;;(:body (client/get "https://coinbase.com/api/v1/currencies/exchange_rates" 
              ;;{:conn-timeout 1000
              ;; :content-type :json
              ;; :follow-redirects false
              ;; :as :json
              ;; :accept :json})) 
        prep (map #(let [s (split (str (key %)) #"_")] {:from (.substring (first s) 0 3) :to (.substring (last s) 0 3) :value (Float/parseFloat (val %))}) response)]
    (if-not (empty? response)
      (transaction
        (delete exchange)
        (insert exchange
                (values prep))))))

(defn get [from to]
  (cache/get-set (str from "_" to)
  (do (-> (Thread. update-from-remote) .start)
    (select exchange
          (where {:from from :to to})))))

