(ns jeroenvandijk.mulog.publishers.prod-file.impl.rotor
  (:require [clojure.java.io :as io])
  (:import [java.io File FilenameFilter]))
;; https://github.com/ptaoussanis/timbre/blob/3ace30385336a29fd6afdbe6b74f756f8253e074/src/taoensso/timbre/appenders/community/rotor.clj#L12-L49

(defn- ^FilenameFilter file-filter
  "Returns a Java FilenameFilter instance which only matches
  files with the given `basename`."
  [basename]
  (reify FilenameFilter
    (accept [_ _ name]
      (.startsWith name basename))))

(defn- matching-files
  "Returns a seq of files with the given `basepath` in the
  same directory."
  [basepath]
  (let [f (-> basepath io/file (.getAbsoluteFile))]
    (-> (.getParentFile f)
        (.listFiles (file-filter (.getName f)))
        seq)))

(defn rotate-logs
  "Performs log file rotation for the given files matching `basepath`
  and up to a maximum of `max-count`. Historical versions are suffixed
  with a 3-digit index, e.g.
      logs/app.log     ; current log file
      logs/app.log.001 ; most recent log file
      logs/app.log.002 ; second most recent log file etc.
  If the max number of files has been reached, the oldest one
  will be deleted. In future, there will be a suffix fn to customize
  the naming of archived logs."
  [basepath max-count]
  (let [abs-path (-> basepath io/file (.getAbsolutePath))
        logs     (-> basepath matching-files sort)
        [logs-to-rotate logs-to-delete] (split-at max-count logs)]

    (doseq [log-to-delete logs-to-delete]
      (io/delete-file log-to-delete))

    (doseq [[^File log-file n] (reverse (map vector logs-to-rotate (iterate inc 1)))]
      (.renameTo log-file (io/file (format "%s.%03d" abs-path n))))))
