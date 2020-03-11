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

(defn get-link [message]
  (when-let [attachments (:attachments message)] 
    (let [link-attachments (filter :original_url attachments)]
      (:original_url (first link-attachments)))))
  

(defn -main
  "Crawl the Fedex channel and get the links with the highest reaction count"
  [& _]
  (let [connection {:api-url "https://slack.com/api" :token (env :slack-api-token)}
        channels (:channels  (clj-slack.conversations/list connection))
        fedex-convo (first (filter #(= (:name %) "fedex") channels))
        channel-id (:id fedex-convo)
        messages (conversation-stream connection channel-id nil)
        with-links (map #(assoc % :link (get-link %)) messages) 
        projected (map #(select-keys % [:link :reactions])  with-links)
        cleaned (filter #(and (:link %)) projected)
        aggregrated (map #(assoc % :reactions (sum-reactions (:reactions %))) cleaned)
        sorted (reverse (sort-by :reactions aggregrated)) 
        json (cc/generate-string sorted {:pretty true})]
    (spit "results.json" json)))
