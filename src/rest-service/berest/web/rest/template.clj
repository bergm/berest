(ns berest.web.rest.template
  (:require [clojure.string :as cs]
            [hiccup.element :as he]
            [hiccup.form :as hf]
            [hiccup.page :as hp]
            [hiccup.def :as hd]
            [hiccup.util :as hu]
            [datomic.api :as d]
            [berest.datomic :as bd]
            [berest.web.rest.queries :as rq]
            [berest.web.rest.util :as util]
            [ring.util.response :as rur]))


(defn url->link-segments
  [url-like & [base]]
  (when url-like
    (let [dir? (= (last url-like) \/)
          url-like* (cs/split url-like #"/")
          urls (for [i (range 1 (inc (count url-like*)))]
                 (split-at i url-like*))]
      (as-> urls _
            (map (fn [[fst _]]
                   (let [url (cs/join "/" fst)
                         url* (str (if (empty? url) (or base "") url) "/")
                         display (str (last fst) "/")]
                     [:a {:href url*} display]))
                 _)
            (drop-last _ )
            (concat _ [(str (last url-like*) (if dir? "/" ""))])))))

#_(url->link-segments "/data/plot/aaaa/")

(defn standard-get-post-h3 [url]
  [:h2 (str "GET | POST ")
   (for [segment (url->link-segments url)]
     segment)])

(defn standard-get-layout*
  [url title description media-type-2-content]
  [:div
   [:h3 (str title " (GET " url ")")]
   [:p description]
   (for [[media-type content] media-type-2-content]
     [:h4 "media-type: " media-type]
     [:hr]
     content)])

(defn standard-get-layout [{:keys [url
                                   get-title description
                                   get-id-fn get-name-fn
                                   entities sub-entity-path
                                   leaf-sub-entities?]}]
  [:div
   [:h3 (str get-title " (GET " url ")")]
   [:h4 "media-type: text/html"]
   [:p description]
   [:hr]
   [:ul#farms
    (for [e entities]
      [:li [:a {:href (str (util/drop-path-segment url) "/"
                           (cs/join "/" sub-entity-path) "/"
                           (get-id-fn e) (if leaf-sub-entities? "" "/"))}
            (or (get-name-fn e) (get-id-fn e))]])]
   [:hr]
   [:h4 "media-type: application/edn"]
   [:code (pr-str (map get-id-fn entities))]
   [:hr]])


(defn standard-post-layout [{:keys [url
                                    post-title post-layout-fn]}]
  [:div
   [:h3 (str post-title " (POST " url ")")]
   [:form.form-horizontal {:role :form
                           :method :post
                           :action url}
    (post-layout-fn)]])
