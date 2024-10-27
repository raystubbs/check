(ns check.core
  (:require-macros check.core)
  (:require
   [check.impl :as impl]))

(def !status impl/!status)
