;;This is a thin wrapper around hashidsJava
(ns whitecity.util.hashids)

(def salt "w~nwvJxext~PYqj|R`w3m0&c6pYE+a7jkGH{mj")
(def minlength 8)
(def alphabet "0123456789abcdefghijklmnpqrstuvwxyz")

(defonce h (HashidsJava.Hashids. salt minlength alphabet))

(defn encrypt [& nums]
  (.encrypt h (long-array (seq nums))))

(defn decrypt [s]
  (let [nums (vec (.decrypt h s))]
    (if (> (count nums) 1)
      nums
      (first nums))))
