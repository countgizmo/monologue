(ns monologue.core
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [clojure.tools.cli :refer [parse-opts]]
            [hiccup.core :refer [html]])
  (:import [java.time LocalDate]
           [java.time.format DateTimeFormatter])
  (:gen-class))

(def input-fmt (DateTimeFormatter/ofPattern "yyyyMMdd"))
(def output-fmt (DateTimeFormatter/ofPattern "dd MMM, yyyy"))

(defn today-date
  "Returns today's date in file-name friendly format.
  The date is local date."
  []
  (.format (LocalDate/now) input-fmt))

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

(defn thought-title->date
  [title]
  (-> (string/split title #".txt")
      first
      (LocalDate/parse input-fmt)))

(defn ->post
  [[date content-file]]
  [:div
   {:class "post"}
   [:h3 (.format date output-fmt)]
   [:div (content-file->html-str content-file)]])

(defn ->archive-entry
  [file-name]
  [:div
   {:class "archive-entry"}
   [:a {:href file-name} file-name]])

(defn generate-page-with-posts!
  [posts page-file-name post-fn]
  (io/make-parents page-file-name)
  (with-open [index-file (io/writer page-file-name :encoding "UTF-8")]
    (doseq [line (read-template)]
      (if (string/includes? line "CONTENT")
        (.write index-file
                (html [:div
                       (for [post posts]
                         (post-fn post))]))
        (.write index-file line)))
    (.flush index-file)))

(defn generate-index-page
  "Takes 10 latest posts and creates an HTML page of them."
  [posts site]
  (let [latest          (take 10 (reverse posts))
        index-file-name (str site "/index.html")]
    (generate-page-with-posts! latest index-file-name ->post)))

(defn generate-archive-page
  [posts site]
  (let [archive-file-name  (str site "/archive.html")
        posts-by-year      (group-by #(.getYear (key %)) posts)]
    (doseq [[year posts] posts-by-year]
      (generate-page-with-posts! posts (str site "/" year ".html") ->post))
    (generate-page-with-posts! (map #(str % ".html")
                                   (sort #(compare %2 %1)
                                         (keys posts-by-year)))
                              archive-file-name
                              ->archive-entry)))

(defn copy-images!
  [src dest]
  (doseq [f (.listFiles (io/file src))]
    (let [file-name  (.getName f)
          img-output (str dest "/" file-name)]
      (when (.isFile f)
        (io/make-parents img-output)
        (io/copy (io/file (str src "/" file-name))
                 (io/file img-output))))))

(defn generate-site
  "Generates the website by using the template.html found in the resources
  and the thoughts found in the thoughts location.
  Two html pages are created: index.html and archive.html
  If there's an img directory in the thoughts location, that directory is moved
  to the site direcoty."
  [{:keys [thoughts site]}]
  (let [posts             (->> (.listFiles (io/file thoughts))
                               (filter #(and (.isFile %)
                                             (string/ends-with? (.getName %)
                                                                ".txt")))
                               (map (fn [f]
                                      {(thought-title->date (.getName f)) f}))
                               (into (sorted-map)))]
    (generate-index-page posts site)
    (generate-archive-page posts site)
    (copy-images! (str thoughts "/img") (str site "/img"))))

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

  (parse-args ["generate" "--thoughts=blah"])


  (generate-site {:thoughts "../countgizmo.github.io/thoughts"
                  :site "../countgizmo.github.io/site"}))


