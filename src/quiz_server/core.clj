(ns quiz-server.core
  (:gen-class)
  (:require [clojure.data [json :as json]])
  (:require [quiz-server [question :as question]])
  (:require [quiz-server [handlers :as handlers]])
  (:require [org.httpkit.server
             :refer [send!
                     with-channel
                     on-close
                     run-server
                     on-receive]]))


(defn handler- [state request]
  (with-channel request channel
    (handlers/connect! state channel)
    (on-close channel
              (fn [status]
                (handlers/disconnect! state channel)))
    (on-receive channel (partial handlers/handle-msg state channel))))

(def init-state {:users {}
                 :current-question (question/gen)})

(defn -main
  "Run the quiz server"
  [& args]
  (let [default-port 3333
        actual-port (if (empty? args) default-port
                        (Integer/parseInt (first args)))
        state (atom init-state)]
    (run-server
     (partial handler- state)
     {:port actual-port})))
