(ns de.zalf.berest.client.hoplon.util
  (:require [cljs-time.core :as cstc]
            [cljs-time.format :as cstf]
            [cljs-time.coerce :as cstcoe]))

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

(defn doy->cljs-time-date
  [doy & [year]]
  (cstc/plus (cstc/date-time (or year 2010) 1 1) (cstc/days (dec doy))))

(defn doy->js-date
  [doy & [year]]
  (cstcoe/to-date (doy->cljs-time-date doy year)))

(defn cljs-time-date->doy
  "get day of year (doy) from a js/Date"
  [date]
  (cstc/in-days (cstc/interval (cstc/date-time (cstc/year date) 1 1)
                               (cstc/plus date (cstc/days 1)))))

(defn js-date->doy
  "get day of year (doy) from a js/Date"
  [js-date]
  (cljs-time-date->doy (cstcoe/from-date js-date)))

(defn dmy-date->doy
  "get day of year (doy) either from a date with its constituents
  or from a js/Date"
  [day month & [year]]
  (cstc/in-days (cstc/interval (cstc/date-time (or year 2010) 1 1)
                               (cstc/plus (cstc/date-time (or year 2010) month day) (cstc/days 1)))))

(defn is-leap-year [year]
  (= 0 (rem (- 2012 year) 4)))

(def indexed (partial map-indexed vector))
#_(defn indexed [col]
  (->> col
       (interleave (range) ,,,)
       (partition 2 ,,,)))

(defn val-event [event]
  (-> event .-target .-value))

(def sum (partial reduce + 0))