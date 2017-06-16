(ns ecspixi.core
  (:require [cljsjs.pixi]
            [reagent.core :as r]
            [oops.core :as o]
            [ecs.EntityManager :as EM]))

(enable-console-print!)

(def P js/PIXI)

(defn shallow-clj->arr [coll]
  (let [arr (array)]
    (doseq [v coll]
      (.push arr v))
    arr))

(defn vec->str-arr [^:not-native coll]
  (let [arr (array)]
    (doseq [v coll]
      (.push arr (name v)))
    arr))

(def Drawable
  (let [drawable (fn [] #js{})]
    drawable))

(def Velocity
  (let [velocity (fn [] #js{})]
    velocity))

(def W (.. js/window -document -body -clientWidth))
(def H (.. js/window -document -body -clientHeight))

(defn get-sprite []
  (.fromImage P.Sprite "https://pixijs.github.io/examples/required/assets/basics/bunny.png"))

(defn get-component [entity c-name]
  (.get entity (name c-name)))

(defn component-set [e c-name ks v]
  (o/oset!+ (get-component e c-name) ks v))

(deftype Component [^:mutable properties])

(defn add-component [entity c-name c-data]
  (.addComponent entity (name c-name) c-data))

(defn make-bunny [em stage x y]
  (let [bunny (.createEntity em)
        sprite (get-sprite)]
    (.addChild stage sprite)
    (.set (.-position sprite) x y)
    (add-component bunny :drawable sprite)
    (add-component bunny :velocity
                   #js{:dx (- (rand-int 20) 10)
                       :dy (- (rand-int 20) 10)})))

(defn query-components [em cs]
  (.queryComponents em (shallow-clj->arr cs)))

(defn rev-x [v]
  (o/oset! v :dx (- (o/oget v :dx))))

(defn rev-y [v]
  (o/oset! v :dy (- (o/oget v :dy))))

(defn bounce-update [em]
  (doseq [e (query-components em ["drawable" "velocity"])]
    (let [x (o/oget (get-component e :drawable) :position :x)
          y (o/oget (get-component e :drawable) :position :y)
          vel (get-component e :velocity)]
      (when (or (>= 0 x) (<= W x))
        (rev-x vel))
      (if (or (>= 0 y) (<= H y))
        (rev-y vel)
        (o/oset! vel :dy (inc (o/oget vel :dy)))))))

(defn clamp [v l h]
  (min h (max l v)))

(defn set-position [drawable x y]
  (.set (.-position drawable) x y))

(defn set-component [e c-name path v]
  (.set e (name c-name) (vec->str-arr path) v))

(defn move-update [em]
  (doseq [e (query-components em ["drawable" "velocity"])]
    (let [x (o/oget (get-component e :drawable) :position :x)
          y (o/oget (get-component e :drawable) :position :y)
          dx (o/oget (get-component e :velocity) :dx)
          dy (o/oget (get-component e :velocity) :dy)]
      (set-component e :drawable [:position :x]
                     (+ x dx))
      (set-component e :drawable [:position :y]
                     (+ y dy)))))

(defn game []
  (let [dom-node (atom false)
        mouse-state (atom {:mousedown false})]
    (r/create-class
     {:display-name "game"
      :component-did-mount
      (fn [this]
        (reset! dom-node (r/dom-node this))
        (let [renderer (.autoDetectRenderer P W H)
              stage (P.Container.)
              em (EM/Manager.)
              loop-fn (fn loop []
                        (when @dom-node
                          (js/requestAnimationFrame loop))
                        (bounce-update em)
                        (move-update em)
                        (.render renderer stage))]
          (dotimes [x 10000]
            (make-bunny em stage (rand-int W) (+ 10 (rand-int (- H 10)))))
          (.appendChild @dom-node (.-view renderer))
          (loop-fn))) :component-will-unmount
      (fn [_]
        (reset! dom-node false))
      :reagent-render
      (fn []
        [:div {:id "game"}])})))

(defn init []
  (r/render [game]
            (.getElementById js/document "app")))
