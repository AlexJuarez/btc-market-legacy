(ns whitecity.util.btc
  (:require
   [clj-btc.core :as btc]
   [taoensso.timbre :refer [trace debug info warn error fatal]]))

(defonce digits58 "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz")

(def config {:rpcpassword "whitecity"
             :rpcuser "devil"
             :rpchost "http://127.0.0.1"
             :rpcport "8332"})

(defn address [account]
  (try
    (btc/getaccountaddress :account (str account) :config config)
    (catch Exception ex
      (error ex "Address creation error"))))

(defn newaddress [account]
  (try
    (btc/getnewaddress :account (str account) :config config)
    (catch Exception ex
      (error ex "Address creation error - new address"))))

(defn decode-base58 [s]
  (byte-array
    (cons 0
        (seq
          (.toByteArray
            (.toBigInteger (reduce #(+ (* 58 %) %2) (map #(bigint (.indexOf digits58 (str %))) s))))))))

(defn validate [bc]
  (let [bcbytes (decode-base58 bc)
        md (java.security.MessageDigest/getInstance "SHA-256")
        hashone (do (.update md (byte-array (drop-last 4 bcbytes))) (.digest md))
        md (java.security.MessageDigest/getInstance "SHA-256")
        hashtwo (do (.update md hashone) (.digest md))]
    (=
      (take-last 4 bcbytes)
      (take 4 hashtwo))))
