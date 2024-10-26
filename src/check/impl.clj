(ns ^:no-doc check.impl
  (:require
   [check.core :as-alias core]
   [clojure.stacktrace :refer [print-stack-trace]]))

(def ^:dynamic *key* nil)

(defn try-catch
  [try-fn catch-fn]
  (try
    (try-fn)
    (catch Throwable ex
      (catch-fn ex))))

(def ^:private ansi-term-codes
  {:reset "\u001B[0m"
   :red "\u001B[31m"})

(defn default-reporter
  [report]
  (when (= (::core/status report) ::core/failure)
    (println
      (:red ansi-term-codes)
      "Check Failed" (::core/key report)
      (:reset ansi-term-codes))
    (when-some [ex (::core/exception report)]
      (print-stack-trace ex))))
