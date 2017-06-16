(ns ecspixi.core
  (:require [cljsjs.pixi]
            [reagent.core :as r]
            [oops.core :refer [oget oset!+ oset!]]
            [ecs.EntityManager :as EM]))

(enable-console-print!)

(def P js/PIXI)

(defn vec->str-arr [^:not-native coll]
  (let [arr (array)]
    (doseq [v coll]
      (.push arr (name v)))
    arr))

(def W (.. js/window -document -body -clientWidth))
(def H (.. js/window -document -body -clientHeight))

(def bunny-texture (.fromImage P.Texture "bunnys.png"))
(def textures
  (mapv (fn [y]
            (P.Texture.
              (.-baseTexture bunny-texture)
              (P.math.Rectangle. 2 y 26 37)))
        [47 86 125 164 2]))

(defn get-sprite []
  (P.Sprite. (rand-nth textures)))

(defn get-component [entity c-name]
  (.get entity (name c-name)))

(defn component-set [e c-name ks v]
  (oset!+ (get-component e c-name) ks v))

(defprotocol IVelocity
  (velocity-set [v dx' dy'])
  (bounce-x [_])
  (bounce-y [_])
  (gravity [_]))

(deftype Velocity [^:mutable dx ^:mutable dy]
  IVelocity
  (velocity-set [v dx' dy']
    (set! dx dx')
    (set! dy dy'))
  (bounce-x [_] (set! dx (- dx)))
  (bounce-y [_] (set! dy (- dy)))
  (gravity [_] (set! dy (inc dy))))

(defprotocol IDrawable
  (pos-x [_])
  (pos-y [_])
  (pos-set [_ x y]))

(deftype Drawable [^:mutable sprite]
  IDrawable
  (pos-x [_] (.. sprite -position -x))
  (pos-y [_] (.. sprite -position -y))
  (pos-set [_ x y] (.set (.-position sprite) x y)))

(defn add-component [entity c-name c-data]
  (.addComponent entity (name c-name) c-data))

(defn make-bunny [em stage x y]
  (let [bunny (.createEntity em)
        sprite (get-sprite)]
    (.addChild stage sprite)
    (.set (.-position sprite) x y)
    (set! (.-rotation sprite) (- (rand) 0.5))
    (add-component bunny
                   :drawable
                   (Drawable. sprite))
    (add-component bunny
                   :velocity
                   (Velocity. (- (rand-int 10) 5)
                              (- (rand-int 10) 5)))))

(defn query-components [em cs]
  (.queryComponents em (vec->str-arr cs)))

(defn rev-x [v]
  (oset! v :dx (- (oget v :dx))))

(defn rev-y [v]
  (oset! v :dy (- (oget v :dy))))

(defn bounce-update [em]
  (doseq [e (query-components em [:drawable :velocity])]
    (let [x (pos-x (get-component e :drawable))
          y (pos-y (get-component e :drawable))
          vel (get-component e :velocity)]
      (when (or (>= 0 x) (<= W x))
        (bounce-x vel))
      (if (or (>= 0 y) (<= H y))
        (bounce-y vel)
        (gravity vel)))))

(defn clamp [v l h]
  (min h (max l v)))

(defn move-update [em]
  (doseq [e (query-components em [:drawable :velocity])]
    (let [drw (get-component e :drawable)
          vel (get-component e :velocity)]
      (pos-set drw (+ (pos-x drw) (.-dx vel))
                   (+ (pos-y drw) (.-dy vel))))))

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
          (dotimes [x 20000]
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
