(ns gen-github-clojuredart-zips-list
  (:require [babashka.fs :as fs]
            [babashka.process :as proc]
            [clojure.java.io :as cji]
            [clojure.string :as cs]
            [conf :as cnf]))

(defn extract-head-branch
  [text]
  (loop [lines (cs/split text #"\n")]
    (if-let [line (first lines)]
      (if-let [[_ default-branch]
               (re-matches #"^\s*HEAD\s+branch:\s+(.*)$" line)]
        default-branch
        (recur (rest lines)))
      nil)))

(comment

  (extract-head-branch "* remote https://github.com/sogaiu/ts-clojure
  Fetch URL: https://github.com/sogaiu/ts-clojure
  Push  URL: https://github.com/sogaiu/ts-clojure
  HEAD branch: default
")
  ;; =>
  "default"

  )

;; input: list of repository urls
;;
;; output: list of zip urls for commits at tips of default branches
(defn -main
  [& _args]
  (when (fs/exists? cnf/cljd-repos-list)
    (with-open [rdr (cji/reader cnf/cljd-repos-list)]
      (doseq [line (line-seq rdr)]
        (when (uri? (java.net.URI. line))
          (let [url line
                temp-dir (fs/create-temp-dir)
                _ (fs/delete-on-exit temp-dir)
                ;; (most?) git commands need to be run from a git repo
                p (proc/process {:dir (fs/file temp-dir)} "git init")
                exit-code (:exit @p)]
            (when-not (zero? exit-code)
              (println "Problem initializing temp repository:" exit-code)
              (System/exit 1))
            ;; find default branch
            (let [p (proc/process {:dir (fs/file temp-dir)
                                   :out :string}
                                  (str "git remote show " url))
                  exit-code (:exit @p)]
              (when-not (zero? exit-code)
                (println "Problem with git remote show:" exit-code)
                (System/exit 1))
              (let [response (:out @p)
                    default-branch (extract-head-branch response)]
                (when-not default-branch
                  (println "No default branch found for:" url)
                  (System/exit 1))
                ;; find commit at tip
                (let [p (proc/process 
                         {:dir (fs/file temp-dir)
                          :out :string}
                         (str "git ls-remote " url " " default-branch))
                      exit-code (:exit @p)]
                  (when-not (zero? exit-code)
                    (println "Problem with git ls-remote:" url " :" 
                             default-branch)
                    (System/exit 1))
                  (let [response (cs/trim (:out @p))]
                    (when-let [[_ sha] 
                               (re-matches #"^([0-9a-f]+).*$" response)]
                      (let [zip-url (str url "/archive/" sha ".zip")]
                        (spit cnf/cljd-zips-list
                              (str zip-url "\n")
                              :append true)))))))))))))

