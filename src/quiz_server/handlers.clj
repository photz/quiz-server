(ns quiz-server.handlers
  (:gen-class)
  (:require [clojure.data [json :as json]])
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
  (println @state)
  (println "connected")
  (swap! state assoc-in
         [:users channel]
         user/new)
  (doseq [channel (keys (get-in @state [:users]))]
    (send! channel (serialize-state @state))))


(defn disconnect! [state channel]
  (println @state)
  (println "disconnected")
  (swap! state update-in [:users] dissoc channel)
  (doseq [channel (keys (get-in @state [:users]))]
    (send! channel (serialize-state @state))))


(defmulti client-msg (fn [state channel msg] (msg "msg-type")))


(defmethod client-msg "set-name" [state channel msg]
  (let [new-name (msg "new-name")]
    (println "set the name to..." name)
    (swap! state assoc-in [:users channel :name] new-name)
    (doseq [channel (keys (get-in @state [:users]))]
      (send! channel (serialize-state @state)))))

(defmethod client-msg "join" [state channel msg]
  (let [name (msg "name")]
    (println "join and set name to " name)
    (println "------------------------------------------------")
    (println msg)
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

        (println "everybody submitted")
        (println state)

        (doseq [channel (keys (get-in state [:users]))]
          (send! channel (serialize-state state)))

        state)
      (do
        (println "some people have not submitted an answer")
        (doseq [channel (keys (get-in state [:users]))]
          (send! channel (serialize-state state)))
        state))))

(defmethod client-msg "submit-answer" [state channel msg]
  (let [selected-answer (msg "selected-answer")]
    (println "submit answer")
    (swap! state submit-answer channel selected-answer)))


(defmethod client-msg :default [state channel msg]
  (println "unrecognizable message from a client"))


(defn handle-msg [state channel raw-msg]
  "Handles a raw message from a client"
  (let [msg (json/read-str raw-msg)]
    (client-msg state channel msg)
    (println "-------------")
    (println state)))
