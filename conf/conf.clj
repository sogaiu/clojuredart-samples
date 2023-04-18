(ns conf
  (:require [babashka.fs :as fs]
            [clojure.string :as cs]))

(def verbose
  (System/getenv "VERBOSE"))

(def proj-root (fs/cwd))

(def clojuredart
  {:name "clojuredart"
   :root (str proj-root "/data/clojuredart-repos")
   :extensions #{"clj" "cljc" "cljd"
                 "edn"}})

(def cljd-repos-list
  (str proj-root "/data/clojuredart-repos-list.txt"))

(def cljd-zips-list
  (str proj-root "/data/clojuredart-zips-list.txt"))

(def cljd-repos-root
  (clojuredart :root))

