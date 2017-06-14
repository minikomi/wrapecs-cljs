(ns ecspixi.core
  (:require [cljsjs.pixi]
            [reagent.core :as r]
            [goog.object :as gobj]
            makr))

(enable-console-print!)

(def P js/PIXI)

(defn shallow-clj->arr [coll]
  (let [arr (array)]
    (doseq [v coll]
      (.push arr v))
    arr))

(deftype Drawable [^:mutable sprite])

(deftype Velocity [^:mutable dx ^:mutable dy])

(def W (.. js/window -document -body -clientWidth))
(def H (.. js/window -document -body -clientHeight))

(defn get-sprite []
  (.fromImage P.Sprite "https://pixijs.github.io/examples/required/assets/basics/bunny.png"))

(defn make-bunny [em stage x y]
  (let [bunny (.create em)
        sprite (get-sprite)]

    (.set (.-position sprite) x y)
    (.addChild stage sprite)

    (.add bunny (Drawable. sprite))
    (.add bunny (Velocity. (rand-int 10) (rand-int 10)))))

(defn rev-x [v]
  (set! (.-dx v) (- (.-dx v))))

(defn rev-y [v]
  (set! (.-dy v) (- (.-dy v))))

(defn bounce-update [em]
  (doseq [e (.query em Drawable Velocity)]
    (let [pos (.-position (.-sprite (.get e Drawable)))
          vel (.get e Velocity)
          x (.-x pos)
          y (.-y pos)]

      (when (or (>= 0 x) (<= W x))
        (rev-x vel))
      (if (or (>= 0 y) (<= H y))
        (rev-y vel)
        (set! (.-dy vel)
              (inc (.-dy vel)))))))

(defn move-update [em]
  (doseq [e (.query em Drawable Velocity)]
    (let [pos (.-position (.-sprite (.get e Drawable)))
          vel (.get e Velocity)]
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
              em (makr Drawable Velocity)
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
