(ns fedex-analyser.core
  (:require [clj-slack.conversations] 
            [cheshire.core :as cc]
            [environ.core :refer [env]])
  (:gen-class))

(defn sum-reactions [reaction-list]
  (if reaction-list
    (reduce + (map :count reaction-list)) 
    0))

(defn conversation-stream
  "Returns a lazy stream of conversation messages, when a page is consumed, the next page will be fetched"
  [connection channel-id cursor]
  (lazy-seq
   (println "Fetching history for channel " channel-id " cursor " cursor)
   (let [response (clj-slack.conversations/history connection channel-id (if cursor {:cursor cursor} {}))]
     (assert (:ok response) (:error response))
     (concat (:messages response)
             (when-let [next-cursor (get-in response [:response_metadata :next_cursor])]
               (conversation-stream connection channel-id next-cursor))))))

(defn extract-attachment-data 
  "Extracts the useful data out of the unstructured attachment data"
  [message]
  (when-let [attachments (:attachments message)] 
    (let [projected (select-keys (last attachments) [:author_name :title :fallback :original_url :app_unfurl_url])]
      (assoc projected 
             :title (or (:title projected) (:fallback projected))
             :url (or (:original_url projected) (:app_unfurl_url projected))))))
  

(defn -main
  "Crawl the Fedex channel and get the links with the highest reaction count"
  [& _]
  (let [connection {:api-url "https://slack.com/api" :token (env :slack-api-token)}
        response (clj-slack.conversations/list connection)
         _ (assert (:ok response) (:error response))
        channels (:channels response)
        fedex-convo (first (filter #(= (:name %) "fedex") channels))
        channel-id (:id fedex-convo)
        messages (conversation-stream connection channel-id nil) 
        with-links (map #(merge % (extract-attachment-data %)) messages)
        projected (map #(select-keys % [:author_name :title :url :reactions]) with-links)
        cleaned (filter :url projected)
        aggregrated (map #(update-in % [:reactions] sum-reactions) cleaned)
        sorted (reverse (sort-by :reactions (reverse aggregrated))) ; Reverse to put newer links at the top
        json (cc/generate-string sorted {:pretty true})]
    (spit "results.json" json)))
