(ns ecspixi.core
  (:require [cljsjs.pixi]
            [reagent.core :as r]
            [goog.object :as gobj]
            tiny-ecs))

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

(defn make-bunny [em stage x y]
  (let [bunny (.createEntity em)
        sprite (get-sprite)]

    (.addComponent bunny Drawable)
    (.set (.-position sprite) x y)
    (.addChild stage sprite)
    (set! (.. bunny -drawable -sprite) sprite)

    (.addComponent bunny Velocity)
    (set! (.. bunny -velocity -dx) (- (rand-int 20) 10))
    (set! (.. bunny -velocity -dy) (- (rand-int 20) 10))))

(defn query-components [em cs]
  (.queryComponents em (shallow-clj->arr cs)))

(defn rev-x [v]
  (set! (.-dx v) (- (.-dx v))))

(defn rev-y [v]
  (set! (.-dy v) (- (.-dy v))))

(defn bounce-update [em]
  (doseq [e (query-components em [Drawable Velocity])]
    (let [pos (.. e -drawable -sprite -position)
          x (.-x pos)
          y (.-y pos)
          vel (.. e -velocity)]
      (when (or (>= 0 x) (<= W x))
        (rev-x vel))
      (if (or (>= 0 y) (<= H y))
        (rev-y vel)
        (set! (.-dy vel)
              (inc (.-dy vel)))))))

(defn move-update [em]
  (doseq [e (query-components em [Drawable Velocity])]
    (let [pos (.. e -drawable -sprite -position)
          vel (.. e -velocity)]
      (.set pos
            (+ (.-x pos) (.-dx vel))
            (+ (.-y pos) (.-dy vel))))))

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
              em (tiny-ecs/EntityManager.)
              loop-fn (fn loop []
                        (when @dom-node
                          (js/requestAnimationFrame loop))
                        (bounce-update em)
                        (move-update em)
                        (.render renderer stage))]
          (dotimes [x 10000]
            (make-bunny em stage (rand-int W) (+ 10 (rand-int (- H 10)))))
          (.appendChild @dom-node (.-view renderer))
          (loop-fn)))


      :component-will-unmount
      (fn [_]
        (reset! dom-node false))
      :reagent-render
      (fn []
        [:div {:id "game"}])})))

(defn init []
  (r/render [game]
            (.getElementById js/document "app")))
