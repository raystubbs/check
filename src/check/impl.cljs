(ns ^:no-doc check.impl
  (:require
   [check.core :as-alias core]))

(def ^:dynamic *keys* nil)

(defn try-catch
  [try-fn catch-fn]
  (try
    (try-fn)
    (catch :default ex
      (catch-fn ex))))

(defn default-reporter
  [report]
  (when (= (::core/status report) ::core/failure)
    (js/console.error "Check Failed" (::core/key report) (::core/exception report))))
