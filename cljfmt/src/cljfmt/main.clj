(ns cljfmt.main
  "Functionality to apply formatting to a given project."
  (:require [cljfmt.config :as config]
            [cljfmt.tool :as tool]
            [clojure.java.io :as io]
            [clojure.tools.cli :as cli])
  (:gen-class))

(defn- cli-options [defaults]
  [[nil "--help"]
   [nil "--[no-]parallel"
    :id :parallel?
    :default (:parallel? defaults)]
   [nil "--project-root PROJECT_ROOT"
    :default (:project-root defaults)]
   [nil "--file-pattern FILE_PATTERN"
    :default (:file-pattern defaults)
    :parse-fn re-pattern]
   [nil "--indents INDENTS_PATH"
    :parse-fn config/read-config]
   [nil "--alias-map ALIAS_MAP_PATH"
    :parse-fn config/read-config]
   [nil "--[no-]ansi"
    :default (:ansi? defaults)
    :id :ansi?]
   [nil "--[no-]indentation"
    :default (:indentation? defaults)
    :id :indentation?]
   [nil "--[no-]remove-multiple-non-indenting-spaces"
    :default (:remove-multiple-non-indenting-spaces? defaults)
    :id :remove-multiple-non-indenting-spaces?]
   [nil "--[no-]remove-surrounding-whitespace"
    :default (:remove-surrounding-whitespace? defaults)
    :id :remove-surrounding-whitespace?]
   [nil "--[no-]remove-trailing-whitespace"
    :default (:remove-trailing-whitespace? defaults)
    :id :remove-trailing-whitespace?]
   [nil "--[no-]insert-missing-whitespace"
    :default (:insert-missing-whitespace? defaults)
    :id :insert-missing-whitespace?]
   [nil "--[no-]remove-consecutive-blank-lines"
    :default (:remove-consecutive-blank-lines? defaults)
    :id :remove-consecutive-blank-lines?]
   [nil "--[no-]split-keypairs-over-multiple-lines"
    :default (:split-keypairs-over-multiple-lines? defaults)
    :id :split-keypairs-over-multiple-lines?]
   [nil "--[no-]sort-ns-references"
    :default (:sort-ns-references? defaults)
    :id :sort-ns-references?]])

(defn- abort [& msg]
  (binding [*out* *err*]
    (when (seq msg)
      (apply println msg))
    (System/exit 1)))

(defn- file-exists? [path]
  (.exists (io/as-file path)))

(defn- abort-if-files-missing [paths]
  (when-some [missing (some (complement file-exists?) paths)]
    (abort "No such file:" (str missing))))

(def ^:dynamic *command*
  "clojure -M -m cljfmt.main")

(defn -main [& args]
  (let [base-opts     (config/load-config)
        parsed-opts   (cli/parse-opts args (cli-options base-opts))
        [cmd & paths] (:arguments parsed-opts)
        options       (-> (config/merge-configs base-opts (:options parsed-opts))
                          (update :paths into paths))]
    (if (:errors parsed-opts)
      (abort (:errors parsed-opts))
      (if (or (nil? cmd) (-> parsed-opts :options :help))
        (do (println "Usage:")
            (println (str \tab *command* " (check | fix) [PATHS...]"))
            (println "Options:")
            (println (:summary parsed-opts)))
        (let [cmdf (case cmd
                     "check" tool/check-no-config
                     "fix"   tool/fix-no-config
                     (abort "Unknown cljfmt command:" cmd))]
          (abort-if-files-missing paths)
          (cmdf options)
          (when (:parallel? options)
            (shutdown-agents)))))))
