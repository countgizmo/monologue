(ns monologue.core
  (:require [clojure.string :as string]
            [clojure.tools.cli :refer [parse-opts]])
  (:import [java.time LocalDate]
           [java.time.format DateTimeFormatter])
  (:gen-class))

(defn today-date
  "Returns today's date in file-name friendly format.
  The date is local date."
  []
  (let [fmt (DateTimeFormatter/ofPattern "yyyyMMdd")]
    (.format (LocalDate/now) fmt)))

(def supported-actions #{"add" "generate"})

(defn usage
  [summary]
  (->> ["Usage: monologue [options] action"
        ""
        "Options:"
        summary
        ""
        "Actions:"
        "  add          Create an empty txt file with name yyyyMMdd"
        "  generate     Generate website"]
       (string/join \newline)))

(defn error-msg
  [errors]
  (str "Couldn't run the command:\n \n"
       (string/join \newline errors)))

(def cli-options
  [["-t" "--thoughts THOUGHTS LOCATION" "Thoughts directory" :default "thoughts"]
   ["-s" "--site SITE LOCATION" "Site directory" :default "site"]
   ["-h" "--help" "Instructions"]])

(defn parse-args
  [args]
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)
        action                                     (first arguments)]
    (cond
      (:help options)                            {:message (usage summary)}
      errors                                     {:message (error-msg errors)}
      (contains? supported-actions action)       {:action action :options options}
      :else                                      {:message (usage summary)})))

(defn add-thought
  [opts])

(defn generate-site
  [opts])

(defn -main [& args]
  (let [{:keys [message action options]} (parse-args args)]
    (if message
      (do (println message)
          (System/exit 1))
      (case action
        "add"      (add-thought options)
        "generate" (generate-site options)))))

(comment

  (parse-args ["-tsomewhere" "-ssomewhereelse"  "add"])

  (parse-args ["generate" "--thoughts=blah" "--site=megalbah"])


  )
