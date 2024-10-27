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

(defmacro samps
  [gen-key & {:as gen-opts}]
  (walk/postwalk
    (fn [x] (cond->> x (seq? x) (cons 'list)))
    ((requiring-resolve (generators gen-key)) gen-opts)))

(defmacro samp
  [gen-key & {:as gen-opts}]
  `(clojure.core/first (samps ~gen-key ~(assoc gen-opts :limit 1))))

(defmacro ^:no-doc async-chain-forms
  [forms context callback]
  (if (empty? forms)
    `(~callback ~context)
    `((check.protocols/async-chain-fn
        (check.impl/try-catch
          (fn [] ~(first forms))
          (fn [error#] (check.impl/fail error#))))
      ~context
      (fn [next-context#]
        (if (::error next-context#)
          (~callback next-context#)
          (async-chain-forms ~(rest forms) next-context# ~callback))))))

(defmacro check
  [check-key & body]
  (when enabled
    `(do
       (swap! check.impl/!status assoc ~check-key ::pending)
       (async-chain-forms ~body
         {::key ~check-key
          ::reporter ~(if &env
                        `(clojure.core/resolve '~reporter)
                        `(clojure.core/requiring-resolve '~reporter))}
         (fn [final-context#]
           (check.impl/report final-context#))))))

(defmacro when-check
  [& body]
  (when enabled
    `(do ~@body)))

(def !status impl/!status)

(comment
  (walk/macroexpand-all
    `(check ::foo
       (do-something-fun)
       (do-something-else)
       (blah)))

  (requiring-resolve 'check.impl/default-reporter)

  (check ::foo
    (prn (samp ::string))))
