(ns quiz-server.core
  (:gen-class)
  (:require [clojure.tools.logging :as log])
  (:require [clojure.data [json :as json]])
  (:require [quiz-server
             [handlers :as handlers]
             [question :as question]])
  (:require [org.httpkit.server :as server]))

(defn handler- [state request]
  (server/with-channel request channel
    (handlers/connect! state channel)
    (server/on-close channel
              (fn [status]
                (handlers/disconnect! state channel)))
    (server/on-receive channel (partial handlers/handle-msg state channel))))

(def init-state {:users {}
                 :current-question (question/gen)})

(defn -main
  "Run the quiz server"
  [& args]
  (let [default-port 3333
        actual-port (if (empty? args) default-port
                        (Integer/parseInt (first args)))
        state (atom init-state)]
    (log/info "running ws server on port" default-port)
    (server/run-server
     (partial handler- state)
     {:port actual-port})))
