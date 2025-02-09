(ns bb.new-post
  (:require
   [babashka.cli :as cli]
   [babashka.fs :as fs]
   [bblgum.core :as b]
   [clj-commons.ansi :as ansi]
   [clojure.edn :as edn]
   [clojure.string :as str]))

(def post-keys
  [:page/title
   :blog-post/author
   :blog-post/created-at
   :blog-post/tags
   :blog-post/header-image
   :open-graph/image
   :open-graph/description
   :blog-post/preview
   :page/body])

(defn post-path [slug]
  (fs/path "content" "posts" (format "%s.md" slug)))

(defn post-exists?
  "Given a post slug, verify if it exists in the filesystem."
  [slug]
  (fs/exists? (post-path slug)))

(defn load-edn-file
  "Given a path to an edn file, cooerce it to a java File and load it."
  [file]
  (->> file
       fs/file
       slurp
       edn/read-string))

(defn slugify
  "Convert a given string to a valid slug."
  [s]
  (-> s
      str/lower-case
      (str/replace #"\s" "-")))

(defn creation-date []
  (let [today (java.time.LocalDate/now)]
    (.format today (java.time.format.DateTimeFormatter/ofPattern "yyyy-MM-dd"))))

(defn list-authors
  "List all the defined authors in content/authors."
  []
  (let [author-path (fs/path "content" "authors")]
    (when (and (fs/exists? author-path)
               (seq (fs/list-dir author-path)))
      (when-let [authors (seq (fs/list-dir author-path))]
        (map load-edn-file authors)))))

(defn get-value
  "General function to handle whether to prompt for a value or not."
  [{:keys [result-fn gum-args key name value-fn]
    :or {value-fn identity}} opts]
  (when-let [value
             (or
              (key opts)
              (-> (apply b/gum gum-args)
                  :result
                  result-fn))]
    (println (ansi/compose
              [:green (format "%s: " name)
               [:white.bold (str (value-fn value))]]))
    value))

(defn get-slug
  "Get the slug from a map of options, otherwise prompt for one."
  [opts]
  (get-value
   {:result-fn #(-> % first slugify)
    :gum-args '(:input :placeholder "Write a short slug (will be converted for you)" :header "Enter a slug")
    :key :slug
    :name "Slug"}
   opts))

(defn get-title
  "Get the title from a map of options, otherwise prompt for one."
  [opts]
  (get-value
   {:result-fn first
    :gum-args '(:input :placeholder "What is your post about?" :header "Enter a title")
    :key :title
    :name "Title"}
   opts))

(defn get-author
  "Get the author from a map of options, otherwise prompt for one."
  [opts]
  (let [authors (list-authors)]
    (get-value
     {:result-fn #(-> (filter (fn [author] (= (first %) (:person/full-name author))) authors) first :person/id)
      :gum-args `(:choose ~(map :person/full-name authors) :header "Select an author")
      :key :author
      :name "Author"
      :value-fn (fn [author]
                  (->> authors
                       (filter #(= author (:person/id %)))
                       first
                       :person/full-name))}
     opts)))

(defn get-tags
  "Get the tags from a map of options, otherwise prompt for them."
  [opts]
  (get-value
   {:result-fn #(map (comp keyword slugify) %)
    :gum-args '(:write :header "Enter tags to use (one per line, result will be slugified)")
    :key :tags
    :name "Tags"
    :value-fn #(str/join ", " (map name %))}
   opts))

(defn get-description
  "Get the description from a map of options, otherwise prompt for it."
  [opts]
  (get-value
   {:result-fn first
    :gum-args '(:write :header "Enter text/markdown description to be used as a sub-title")
    :key :description
    :name "Description"}
   opts))

(defn get-preview
  "Get the description from a map of options, otherwise prompt for it."
  [opts]
  (get-value
   {:result-fn #(str/join "\n" %)
    :gum-args '(:write :header "Enter preview text/markdown to be displayed")
    :key :preview
    :name "Preview"}
   opts))

(defn build-post
  "Given a set of options, build a page or prompt for as many values as needed."
  [opts slug]
  {:page/title (get-title opts)
   :blog-post/author {:person/id (get-author opts)}
   :blog-post/created-at (creation-date)
   :blog-post/tags (pr-str (vec (get-tags opts)))
   :blog-post/header-image (format "/header/images/%s/preview.png" slug)
   :open-graph/image (format "/opengraph/%s.png" slug)
   :open-graph/description (get-description opts)
   :blog-post/preview (str "\n" (get-preview opts))
   :page/body "\n\nDelete me and start writing!"})

(defn serialize-map [m ordered-keys]
  (let [serialize-kv (fn [k v] (str k " " v "\n"))
        serialized-pairs (map (fn [k] (serialize-kv k (get m k))) ordered-keys)]
    (apply str serialized-pairs)))

(def cli-options
  {:help {:desc "Show this help"
          :alias :h
          :coerce :boolean}
   :title {:desc "Title of the post"
           :coerce :string}
   :author {:desc (format "Keyword of the author to use. Valid examples: %s" (str/join ", " (map :person/id (list-authors))))
            :coerce :keyword}
   :tags {:desc "A list of keywords to tag the post with"
          :coerce :vector}
   :description {:desc "The sub-title of the post"
                 :coerce :string}
   :preview {:desc "Preview text from the post used in the OG image also"
             :coerce :string}})

(defn write-post []
  (let [opts (cli/parse-opts *command-line-args* {:spec cli-options})]
    (if (:help opts)
      (do
        (println "new-post -- Generate a new post")
        (println)
        (println (cli/format-opts {:spec cli-options})))
      (let [slug (get-slug opts)]
        (if (post-exists? slug)
          (println (ansi/compose [:red.bold "Post already exists."]))
          (let [post (-> opts
                         (build-post slug)
                         (serialize-map post-keys))]
            (if (zero? (:status (b/gum :confirm ["Are you sure you want to create a post with the above fields?"])))
              (do
                (spit (str (post-path slug)) post)
                (fs/create-dir (fs/path "resources" "public" "images" slug))
                (println
                 (ansi/compose
                  [:green "\nCreated "
                   [:white.bold (str (post-path slug))]
                   " and "
                   [:white.bold (str (fs/path "resources" "public" "images" slug))]])))
              (println (ansi/compose [:red.bold "\nAborted post"])))))))))
