(ns ^:no-doc check.impl
  (:require
   [check.protocols]
   [check.core :as-alias core]))

(defn try-catch
  [try-fn catch-fn]
  (try
    (try-fn)
    (catch #?(:clj Throwable :cljs :default) ex
      (catch-fn ex))))

(def ^:private ansi-term-codes
  {:reset "\u001B[0m"
   :red "\u001B[31m"})

(def !status (atom {}))

#?(:clj
   (defn default-reporter
     [report]
     (when (= (::core/status report) ::core/failure)
       (println
         (:red ansi-term-codes)
         "Check Failed" (::core/key report)
         (:reset ansi-term-codes))
       (when-some [error (::core/error report)]
         (println error))))

   :cljs
   (let [has-devtools? (boolean (find-ns 'devtools.formatters.core))
         in-browser? (boolean (.-document js/globalThis))]
     (defn default-reporter
       [report]
       (when (= (::core/status report) ::core/failure)
         (js/console.error 
           (when-not in-browser? (:red ansi-term-codes))
           "Check Failed"
           (cond-> (::core/key report) (not has-devtools?) pr-str)
           (when-not in-browser? (:reset ansi-term-codes))
           "\n"
           (::core/error report))))))

(defn fail
  [error]
  (reify check.protocols/AsyncChain
    (async-chain-fn [_]
      (fn [context complete]
        (complete (assoc context ::core/error error))))))


(defn report
  [context]
  (let [status (if (some? (::core/error context)) ::core/failure ::core/success)]
    ((::core/reporter context)
     (cond->
       {::core/status status
        ::core/key (::core/key context)}
       (= ::core/failure status)
       (assoc ::core/error (::core/error context))))
    (swap! !status assoc (::core/key context) status)))
