(ns quiz-server.question
  (:require [clojure.string :as str])
  (:gen-class))

(require '[cemerick.url :refer (url url-encode)])

(def domains
  ["www.youtube.com"
   "my-cats.philipp-horn.de"
   "staging.wendero.com"
   "playbooks.wendero.de"
   "computer-science.mit.edu"
   "my-new-songs.britney.com"
   "news.faz.net"
   "amazing-deals.walmart.com"])
   

(defn domain-gen []
  (let [rand-domain (rand-nth domains)
        [subdomain middle tld] (str/split rand-domain #"\.")

        components {:sub subdomain
                    :tld tld}

        rnd-component (rand-nth (keys components))

        text (str/join " " ["Was ist die Rolle von"
                            (if (= rnd-component :sub)
                              subdomain
                              tld)
                            "in"
                            rand-domain])

        new-question {:text text
                      :correct-answer (if (= rnd-component :sub)
                                        0
                                        1)
                      :choices {0 "subdomain"
                                1 "top-level domain"
                                2 "weder noch"}}]

    (println new-question)

    new-question))



(def urls
  ["https://wendero.egnyte.com/navigate/file/2ce67460-c4bb-468b-a2b9-deba4d4efd70"
   "https://immobilien-check.techem.de/m/marktwertermittlung/"
   "https://www.heise.de/preisvergleich/"
   "https://web.telegram.org"
   "https://www.sistrix.com/ask-sistrix/"
   "https://www.metalevel.at/prolog/concepts"
   "https://wenderoteam.slack.com/messages/C7HDK8S8K/"
   "https://github.com/rtfeldman/elm-css"
   "ftp://csp.com:8100/zk"
   "http://www.faz.net/aktuell/finanzen/"
   "http://www.spiegel.de/sport/fussball"
   "http://techem-immocheck.staging.wendero.com/l/marktwertermittlung"])

(defn url-gen-question [raw-url]
  (let
      [parsed-url (url raw-url)
       components [:protocol :host :path]
       components (if (= -1 (:port parsed-url))
                    components
                    (concat components [:port]))
       rnd-component (rand-nth components)
       text (str/join " " ["Was ist"
                       (rnd-component parsed-url)
                       "in dieser URL:"
                       raw-url])
                       
       correct-answer (.indexOf components rnd-component)

       new-question {:text text
                     :correct-answer correct-answer
                     :choices {0 "Protokoll"
                               1 "Hostname"
                               2 "Pfad"
                               3 "Port"}}]

    new-question))

(def predefined-questions
  [{:correct-answer 3
    :text "Welcher der Ausdrücke HTTPS, WEBCAL oder SMTP ist ein Protokoll?"
    :choices {0 "HTTPS"
              1 "WEBCAL"
              2 "SMTP"
              3 "Alle der genannten"}}
   {:correct-answer 1
    :text "Wie lautet die Top-Level-Domain von wendero?"
    :choices {0 ".berlin"
              1 ".com"
              2 ".net"}}
   {:text "Was ist www in www.youtube.com?"
    :correct-answer 0
    :choices {0 "subdomain"
              1 "tld"
              2 "port"
              3 "Protokoll"}}
   {:text "Welche Rolle hat .com in example.com?"
    :correct-answer 1
    :choices {0 "Port"
              1 "Top-level domain"
              2 "Hostname"
              3 "Domain"}}
   {:correct-answer 2
    :text "Welche der folgenden URLs verfügt über einen Parameter?"
    :choices {0 "https://de.wikipedia.org/wiki/Internetprotokollfamilie#Beispiel"
              1 "https://wendero.egnyte.com/app/index.do#storage/files/1/Shared/01-project-operations/01-active-projects"
              2 "https://www.techem.de/?source=google-search"}}])


(defn url-gen []
  (let
      [some-url (rand-nth urls)
       question (url-gen-question some-url)]
    question))

(defn choose-predefined []
  (let [rand-question (rand-nth predefined-questions)]
    rand-question))
        

(def generators [url-gen choose-predefined domain-gen])

(defn gen []
  (let [generator (rand-nth generators)]
    (generator)))
