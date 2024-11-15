(ns check.core
  (:require-macros check.core)
  (:require
   [check.impl :as impl]))

(def !status impl/!status)

(defn expect
  [f & args]
  (when-not (apply f args)
    (throw
      (ex-info
        "Expectation Unsatisfied"
        {::expect-fun (or (when (fn? f) (some-> f .-name demunge symbol)) f)
         ::expect-args args}))))
