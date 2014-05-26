(ns de.zalf.berest.client.hoplon.util)

(defn cell-update-in
  [global-cell path-to-substructure]
  (fn [path func & args]
    (apply update-in global-cell (vec (concat path-to-substructure path)) func args)))

(defn round [value & {:keys [digits] :or {digits 0}}]
  (let [factor (.pow js/Math 10 digits)]
    (-> value
        (* ,,, factor)
        (#(.round js/Math %) ,,,)
        (/ ,,, factor))))


(defn js-date-time->date-str [date]
  (some-> date .toJSON (.split ,,, "T") first))

(defn is-leap-year [year]
  (= 0 (rem (- 2012 year) 4)))


(defn indexed [col]
  (->> col
       (interleave (range) ,,,)
       (partition 2 ,,,)))

(defn val-event [event]
  (-> event .-target .-value))

