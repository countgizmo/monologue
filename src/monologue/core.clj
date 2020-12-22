(ns monologue.core
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [clojure.tools.cli :refer [parse-opts]]
            [hiccup.core :refer [html]])
  (:import [java.time LocalDate]
           [java.time.format DateTimeFormatter])
  (:gen-class))


(def date-fmt (DateTimeFormatter/ofPattern "yyyyMMdd"))
(def output-fmt (DateTimeFormatter/ofPattern "dd MMM, yyyy"))


(defn today-date
  "Returns today's date in file-name friendly format.
  The date is local date."
  []
  (.format (LocalDate/now) date-fmt))

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
  "Creates a txt file with today's date as the filename.
  If the parent directories don't exist - creates it."
  [{:keys [thoughts]}]
  (let [new-thought-file (str thoughts "/" (today-date) ".txt")]
    (io/make-parents new-thought-file)
    (spit new-thought-file "")
    new-thought-file))

(defn read-template
  []
  (line-seq (io/reader (io/resource "template.html"))))

(defn content-file->html-str
  [content-file]
  (for [line (line-seq (io/reader content-file))]
    (str line "<br />")))

(defn generate-index-page
  "Takes 10 latest posts and creates an HTML page of them."
  [posts site]
  (let [latest          (take 10 (reverse posts))
        index-file-name (str site "/index.html")
        format-title    (fn [title]
                          (as-> (string/split title #".txt") t
                            (first t)
                            (LocalDate/parse t date-fmt)
                            (.format t output-fmt)))]
    (io/make-parents index-file-name)
    (with-open [index-file (io/writer index-file-name :encoding "UTF-8")]
      (doseq [line (read-template)]
        (if (string/includes? line "CONTENT")
          (.write index-file
                  (html[:div
                        (for [[title content-file] latest]
                          [:div
                           [:h3 (format-title title)]
                           [:div (content-file->html-str content-file)]
                           [:hr]])]))
          (.write index-file line)))
      (.flush index-file))))

(defn generate-site
  "Generates the website by using the template.html found in the resources
  and the thoughts found in the thoughts location.
  Two html pages are created: index.html and archive.html
  If there's an img directory in the thoughts location, that directory is moved
  to the site direcoty."
  [{:keys [thoughts site]}]
  (let [posts             (->> (file-seq (io/file thoughts))
                               (filter #(.isFile %))
                               (map (fn [f] {(.getName f) f}))
                               (into (sorted-map)))
        archive-file-name (str site "/archive.html" )]
    (io/make-parents archive-file-name)
    (generate-index-page posts site)))

(defn -main [& args]
  (let [{:keys [message action options]} (parse-args args)]
    (if message
      (do (println message)
          (System/exit 1))
      (case action
        "add"      (->> (add-thought options)
                        (println "new thought: "))
        "generate" (generate-site options)))))

(comment

  (parse-args ["-tsomewhere" "-ssomewhereelse"  "add"])

  (parse-args ["generate" "--thoughts=blah" ])


  (generate-site {:thoughts "../countgizmo.github.io/thoughts"
                  :site "../countgizmo.github.io/site"})


  )
