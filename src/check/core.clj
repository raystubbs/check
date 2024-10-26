(ns check.core
  (:require
   [clojure.java.basis :as basis]
   [clojure.java.io :as io]
   [clojure.walk :as walk]
   [clojure.edn :as edn]
   [check.impl :as impl])
  (:import
   java.net.URL
   java.io.PushbackReader))

(def enabled
  (-> (basis/current-basis)
    :argmap
    :me.raystubbs.check/enabled
    boolean))

(def reporter
  (or
    (let [sym (-> (basis/current-basis) :argmap :me.raystubbs.check/reporter)]
      (when (qualified-symbol? sym)
        sym))
    'check.impl/default-reporter))

(def ^:private generators
  (reduce
    (fn [g ^URL url]
      (merge g
        (with-open [rdr (io/reader url)]
          (edn/read (PushbackReader. rdr)))))
    {}
    (-> (Thread/currentThread)
      .getContextClassLoader
      (.getResources "me/raystubbs/check/generators.edn")
      enumeration-seq)))

(defn- samples
  [gen-key gen-opts]
  (walk/postwalk
    (fn [x] (cond->> x (seq? x) (cons 'list)))
    ((requiring-resolve (generators gen-key)) gen-opts)))

(defn- report
  [& {:as report}]
  (list `(clojure.core/resolve '~reporter) report))

(defn- inner
  [check-key body]
  `(clojure.core/binding
    [check.impl/*key* ~check-key]
     (check.impl/try-catch
       (fn []
         ~@body)
       (fn [~'ex]
         ~(report
            ::status ::failure
            ::key check-key
            ::exception 'ex)))))

(defmacro check
  [check-key & body]
  (when enabled
    (inner check-key
      (walk/postwalk
        (fn [x]
          (if-not (seq? x)
            x
            (case (first x)
              (::sample ::samples)
              (let [[gen-key & {:as gen-opts}] (rest x)]
                (case (first x)
                  ::sample `(clojure.core/first ~(samples gen-key (assoc gen-opts :limit 1)))
                  ::samples (take (or (:limit gen-opts) 128) (samples gen-key gen-opts))))

              ::inner
              (inner check-key (rest x))

              x)))
        body))))
