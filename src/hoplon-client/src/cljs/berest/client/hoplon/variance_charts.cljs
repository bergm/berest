(ns berest.client.hoplon.variance-charts
  (:refer-clojure :exclude [range repeat]))

(defn- make-elem-ctor [tag]
  (fn [& args]
    (apply (.createElement js/document tag) args)))

(def chart (make-elem-ctor "chart"))

;geometries
(def point (make-elem-ctor "point"))
(def dot (make-elem-ctor "dot"))
(def range (make-elem-ctor "range"))
(def bar (make-elem-ctor "bar"))
(def boxplot (make-elem-ctor "boxplot"))
(def line (make-elem-ctor "line"))

;non-visible elements
(def groups (make-elem-ctor "groups"))
(def repeat (make-elem-ctor "repeat"))
(def group (make-elem-ctor "group"))
(def datum (make-elem-ctor "datum"))
(def csv (make-elem-ctor "csv"))
(def json (make-elem-ctor "json"))

;guides & annotations
(def guide-x (make-elem-ctor "guide-x"))
(def guide-y (make-elem-ctor "guide-y"))
(def annotation (make-elem-ctor "annotation"))

;debugging
(def debug-table (make-elem-ctor "debug-table"))

(defn set-data
  "sets 'data property on angular scope of element with given id
  using $apply directive to have the change propagate to view"
  [element-id clj-value]
  (let [scope (.. js/angular (element (js/$ (str "#" element-id))) scope)]
    (.$apply scope #(aset % "data" (clj->js clj-value)))))

