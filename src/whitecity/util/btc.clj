(ns whitecity.util.btc
  (:require
   [clj-btc.core :as btc]))

(def config {:rpcpassword "whitecity"
             :rpcuser "devil"
             :rpchost "http://127.0.0.1"
             :rpcport "8332"})

(defn address [account]
  (btc/getaccountaddress :account (str account) :config config))

(defn newaddress [account]
  (btc/getnewaddress :account (str account) :config config))

(defn validate [address] address)
