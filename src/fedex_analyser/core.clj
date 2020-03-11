(ns fedex-analyser.core
  (:require [clj-slack.conversations] 
            [cheshire.core :as cc]
            [environ.core :refer [env]])
  (:gen-class))

(defn sum-reactions [reaction-list]
  (if reaction-list
    (reduce + (map :count reaction-list)) 
    0))

(defn conversation-stream [connection channel-id cursor]
    (lazy-seq
     (println "Fetching history for channel " channel-id " cursor " cursor)
     (let [history (clj-slack.conversations/history connection channel-id (if cursor {:cursor cursor} {}))]
       (concat (:messages history)
               (when-let [next-cursor (get-in history [:response_metadata :next_cursor])]
                 (conversation-stream connection channel-id next-cursor))))))

(defn extract-attachment-data [message]
  (when-let [attachments (:attachments message)] 
    (let [link-attachments (filter :original_url attachments)]
      (select-keys (last link-attachments) [:author_name :fallback :original_url]))))
  

(defn -main
  "Crawl the Fedex channel and get the links with the highest reaction count"
  [& _]
  (let [connection {:api-url "https://slack.com/api" :token (env :slack-api-token)}
        channels (:channels  (clj-slack.conversations/list connection))
        fedex-convo (first (filter #(= (:name %) "fedex") channels))
        channel-id (:id fedex-convo)
        messages (conversation-stream connection channel-id nil)
        with-links (map #(merge % (extract-attachment-data %)) messages)
        cleaned (filter #(and (:original_url %)) with-links)
        projected (map #(select-keys % [:author_name :fallback :original_url :reactions]) cleaned)
        aggregrated (map #(assoc % :reactions (sum-reactions (:reactions %))) projected)
        sorted (reverse (sort-by :reactions (reverse aggregrated))) ; Reverse to put newer links at the top
        json (cc/generate-string sorted {:pretty true})]
    (spit "results.json" json)))
