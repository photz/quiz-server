(ns quiz-server.user
  (:gen-class))

(def new {:name "Anon"
          :score 0
          :playing false
          :selected-answer nil})

(defn has-selected-answer [user]
  "Returns true iff the user has selected an answer this round"
  (not (nil? (get-in user [:selected-answer]))))

(defn select-answer [user answer]
  "Select an answer if none has been selected"
  (if (has-selected-answer user)
    (assoc-in user [:selected-answer] answer)
    user))
  
(defn reset-answer [user]
  "Resets the user's selected answer"
  (assoc-in user [:selected-answer] nil))

(defn bump-score [user]
  "Increase this user's score by 1"
  (update-in user [:score] inc))

(defn bump-if-correct [correct-answer user]
  "Increase the user's score if their answer matches the correct one"
  (if (= correct-answer (:selected-answer user))
    (bump-score user)
    user))
