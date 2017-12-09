(ns quiz-server.handlers
  (:gen-class)
  (:require [clojure.data [json :as json]])
  (:require [clojure.tools.logging :as log])
  (:require [quiz-server [question :as question]])
  (:require [quiz-server [user :as user]])
  (:require [org.httpkit.server
             :refer [send!]]))



(defn serialize-state [state]
  (let [current-question (:current-question state)
        players (filter #(true? (:playing %)) (vals (:users state)))]
        

    (json/write-str
     {:users (map (fn [u]
                    {:name (:name u)
                     :submitted (not (nil? (:selected-answer u)))
                     :playing (:playing u)
                     :score (:score u)})
                  players)
      
      :current-question {:text (:text current-question)
                         :choices (map (fn [[idx text]] [idx text]) (:choices current-question))}})))


(defn connect! [state channel]
  (log/info "got a new client")
  (swap! state assoc-in
         [:users channel]
         user/new)
  (doseq [channel (keys (get-in @state [:users]))]
    (send! channel (serialize-state @state))))


(defn disconnect! [state channel]
  (log/info "client left")
  (swap! state update-in [:users] dissoc channel)
  (doseq [channel (keys (get-in @state [:users]))]
    (send! channel (serialize-state @state))))


(defmulti client-msg (fn [state channel msg] (msg "msg-type")))


(defmethod client-msg "set-name" [state channel msg]
  (let [new-name (msg "new-name")]
    (swap! state assoc-in [:users channel :name] new-name)
    (doseq [channel (keys (get-in @state [:users]))]
      (send! channel (serialize-state @state)))))

(defmethod client-msg "join" [state channel msg]
  (let [name (msg "name")]
    (swap! state update-in [:users channel]
           (fn [user]
             (-> user
                 (assoc-in [:playing] true)
                 (assoc-in [:name] name))))
    (doseq [channel (keys (get-in @state [:users]))]
          (send! channel (serialize-state @state)))))
   
(defn map-kv [coll f]
  (reduce-kv (fn [m k v] (assoc m k (f v))) (empty coll) coll))

                                     


(defn submit-answer [state channel selected-answer]
  "Update the tally based on the submission"
  (let
      [correct-answer (get-in state [:current-question :correct-answer])

       state (assoc-in state [:users channel :selected-answer] selected-answer)

       users (vals (:users state))

       players (filter #(true? (:playing %)) (vals (:users state)))

       everybody-submitted (every? user/has-selected-answer players)]

    (if everybody-submitted
      (let
          [state (update-in
                  state
                  [:users]
                  map-kv
                  (partial user/bump-if-correct correct-answer))
          
           state (assoc-in
                  state
                  [:current-question]
                  (question/gen))

           state (update-in
                  state
                  [:users]
                  map-kv
                  user/reset-answer)]

        (doseq [channel (keys (get-in state [:users]))]
          (send! channel (serialize-state state)))

        state)
      (do
        (doseq [channel (keys (get-in state [:users]))]
          (send! channel (serialize-state state)))
        state))))

(defmethod client-msg "submit-answer" [state channel msg]
  (let [selected-answer (msg "selected-answer")]
    (swap! state submit-answer channel selected-answer)))


(defmethod client-msg :default [state channel msg]
  (log/warn "got a message with an unknown type"))


(defn handle-msg [state channel raw-msg]
  "Handles a raw message from a client"
  (let [msg (json/read-str raw-msg)]
    (client-msg state channel msg)))

