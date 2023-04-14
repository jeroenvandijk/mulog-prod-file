(ns jeroenvandijk.mulog.publishers.prod-file
  (:require [com.brunobonacci.mulog.publisher :as pub]
            [com.brunobonacci.mulog.buffer :as rb]
            [com.brunobonacci.mulog.utils :as ut]
            [clojure.java.io :as io]
            [jeroenvandijk.mulog.publishers.prod-file.impl.rotor :refer [rotate-logs]]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;                     ----==| P R O D   F I L E |==----                      ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- str->byte-length [s]
  ;; Each char is 16 bit, string length = 2 x byte lenght
  (* 2 (long (.length s))))

(deftype ProdFilePublisher [config ^java.io.Writer *filewriter file-path buffer transform max-file-count max-byte-count]

  com.brunobonacci.mulog.publisher.PPublisher
  (agent-buffer [_]
    buffer)


  (publish-delay [_]
    50)


  (publish [_ buffer]
    (let [init-writer @*filewriter
          file (io/file file-path)
          init-file-size (.length file)
          ;; items are pairs [offset <item>]
          items (transform (map second (rb/items buffer)))]
      (reduce (fn [[fw file-size] item]
                (let [item-str (str (ut/edn-str item) \newline)
                      item-byte-count (str->byte-length item-str)
                      byte-count (+ file-size item-byte-count)
                      [fw byte-count :as ret]
                      (if (< byte-count max-byte-count)
                        [fw byte-count]
                        (do
                          (.flush fw)
                          (.close fw)

                          (rotate-logs file max-file-count)

                          (let [new-fw (io/writer file-path :append true)]
                            (reset! *filewriter new-fw)
                            [new-fw 0])))]

                  (.write fw item-str)
                  (.flush fw)
                  ret))
              [init-writer init-file-size]
              items))

    (rb/clear buffer))

  java.io.Closeable
  (close [_]
    (let [w @*filewriter]
      (.flush w)
      (.close w))))


(defn prod-file-publisher
  [{:keys [filename transform
           max-file-count
           max-byte-count] :as config
    :or {max-file-count 100
         max-byte-count (* 1024 1024)}}]
  {:pre [filename]}
  (when (or (string? filename) (instance? java.io.File filename))
    (io/make-parents filename))
  (ProdFilePublisher.
   config
   (atom (io/writer filename :append true))
   (.getAbsolutePath (io/file filename))
   (rb/agent-buffer 10000)
   (or transform identity)
   max-file-count
   max-byte-count))


(defmethod pub/publisher-factory :prod-file
  [config]
  (prod-file-publisher config))


;; Scratch

(comment
  (require '[com.brunobonacci.mulog :as μ]
           '[com.brunobonacci.mulog.core :as μ.core])


  (def stop-pub (μ/start-publisher!
                 {:type :multi
                  :publishers
                  [{:type :console}
                   {:type :prod-file
                    :filename "tmp/mulog/app.log"
                    :max-file-count 20
                    :max-byte-count (* 100 1024) ;; 100kb
                    }]}))


   (doseq [batch (partition 100 (range 20000000))]
    (doseq [i batch]
      (μ/log ::hello :to "New World!")
      ;; Add sleep so we can interrupt
      (Thread/sleep 10)))

   (stop-pub))
