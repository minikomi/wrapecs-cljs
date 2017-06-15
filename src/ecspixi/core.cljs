(ns ecspixi.core
  (:require [cljsjs.pixi]
            [reagent.core :as r]
            [goog.object :as o]
            [ecs.EntityManager :as EM]))

(enable-console-print!)

(def P js/PIXI)

(defn shallow-clj->arr [coll]
  (let [arr (array)]
    (doseq [v coll]
      (.push arr v))
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

(defn component-set [e c-name k v]
  (o/set (o/get e (name c-name)) (name k) v))

(deftype Velocity [^:mutable dx ^:mutable dy])

(defn make-bunny [em stage x y]
  (let [bunny (.createEntity em)
        sprite (get-sprite)]
    (.addChild stage sprite)
    (.set (.-position sprite) x y)
    (.addComponent bunny "drawable" sprite)
    (.addComponent bunny "velocity"
                   (Velocity. (- (rand-int 20) 10)
                              (- (rand-int 20) 10)))))

(defn query-components [em cs]
  (.queryComponents em (shallow-clj->arr cs)))

(defn rev-x [v]
  (set! (.-dx v) (- (.-dx v))))

(defn rev-y [v]
  (set! (.-dy v) (- (.-dy v))))

(defn bounce-update [em]
  (doseq [e (query-components em ["drawable" "velocity"])]
    (let [spr (.get e "drawable")
          pos (.-position spr)
          x (.-x pos)
          y (.-y pos)
          vel (.get e "velocity")]
      (when (or (>= 0 x) (<= W x))
        (rev-x vel))
      (if (or (>= 0 y) (<= H y))
        (rev-y vel)
        (set! (.-dy vel)
              (inc (.-dy vel)))))))

(defn clamp [v l h]
  (min h (max l v)))

(defn move-update [em]
  (doseq [e (query-components em ["drawable" "velocity"])]
    (let [pos (.-position (.get e "drawable"))
          vel (.get e "velocity")]
      (.set pos
            (clamp (+ (.-x pos) (.-dx vel))
                   0 W)
            (clamp (+ (.-y pos) (.-dy vel))
                   0 H)))))

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
