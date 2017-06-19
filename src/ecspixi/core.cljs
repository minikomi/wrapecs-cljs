(ns ecspixi.core
  (:require [cljsjs.pixi]
            [reagent.core :as r]
            [oops.core :refer [oget oset!+ oset!]]
            [ecs.EntityManager :as EM]))

(enable-console-print!)

;; constants

(def P js/PIXI)
(def MAX_BUNNIES 10000)
(def NEW_BUNNIES 100)
(def W (.. js/window -document -body -clientWidth))
(def H (.. js/window -document -body -clientHeight))

;; util

(defn vec->str-arr [^:not-native coll]
  (let [arr (array)]
    (doseq [v coll]
      (.push arr (name v)))
    arr))

(defn get-component [entity c-name]
  (.get entity (name c-name)))

(defn add-component [entity c-name c-data]
  (.addComponent entity (name c-name) c-data))

(defn query-components [em cs]
  (.queryComponents em (vec->str-arr cs)))

(defn get-global [em kw]
  (get (.-globals em) kw))

(defn clamp [v l h]
  (min h (max l v)))

;; components

(defprotocol IVelocity
  (velocity-set [v dx' dy'])
  (bounce-x [_])
  (bounce-y [_])
  (gravity [_]))

(deftype Velocity [^:mutable dx ^:mutable dy]
  IFn
  (-invoke [this kw]
    (case kw
      :dx dx
      :dy dy
      nil))
  IVelocity
  (velocity-set [v dx' dy']
    (set! dx dx')
    (set! dy dy'))
  (bounce-x [_] (set! dx (- dx)))
  (bounce-y [_] (set! dy (- dy)))
  (gravity [_] (set! dy (inc dy))))

(defprotocol IDrawable
  (position-set [_ x y]))

(deftype Drawable [^:mutable sprite]
  IFn
  (-invoke [this kw]
    (case kw
      :x (.-x (.-position sprite))
      :y (.-y (.-position sprite))
      :sprite sprite
      nil))
  IDrawable
  (position-set [_ x y]
    (.set (.-position sprite) x y)))

;; sprite

(def bunny-texture (.fromImage P.Texture "bunnys.png"))

(def textures
  (mapv (fn [y]
          (P.Texture.
           (.-baseTexture bunny-texture)
           (P.Rectangle. 2 y 26 37)))
        [47 86 125 164 2]))

(defn get-sprite []
  (P.Sprite. (rand-nth textures)))

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
                   (Velocity. (- (rand-int 20) 10)
                              (- (+ 10 (rand-int 5)))))))

;; systems

(defn bounce-update [em]
  (doseq [e (query-components em [:drawable :velocity])]
    (let [x ((get-component e :drawable) :x)
          y ((get-component e :drawable) :y)
          vel (get-component e :velocity)]
      (when (or (>= 0 x) (<= W x))
        (bounce-x vel))
      (if (or (>= 0 y) (<= H y))
        (bounce-y vel)
        (gravity vel)))))

(defn move-update [em]
  (doseq [e (query-components em [:drawable :velocity])]
    (let [drw (get-component e :drawable)
          vel (get-component e :velocity)]
      (position-set drw
                    (+ (drw :x) (* 0.5 (vel :dx)))
                    (+ (drw :y) (* 0.5 (vel :dy)))))))

;; main loop

(def event-bus (volatile! []))

(defn event!
  ([event-type]
   (vswap! event-bus conj [event-type nil]))
  ([event-type data]
   (vswap! event-bus conj [event-type data])))

(defn init-events [stage em]
  (set! (.-interactive stage) true)
  (set! (.-hitArea stage)
        (P.Rectangle. 0 0 W H))
  (.on stage "mousemove"
       (fn [ev]
         (event! :mouse-move {:x (.. ev -data -global -x)
                              :y (.. ev -data -global -y)})))
  (.on stage "mousedown"
       (fn [ev]
         (event! :mouse-down {:x (.. ev -data -global -x)
                              :y (.. ev -data -global -y)})))
  (.on stage "mouseup"
       (fn [ev] (event! :mouse-up))))

(def mouse-pressed (volatile! false))
(def mouse-position (volatile! nil))
(def event-handlers
  {:mouse-down
   (fn mouse-down-handler [_ data]
     (vreset! mouse-pressed true)
     (vreset! mouse-position data))
   :mouse-up
   (fn mouse-up-handler [_ data]
     (vreset! mouse-pressed false))
   :mouse-move
   (fn mouse-move-hanlder [_ data]
     (vreset! mouse-position data))})

(defn maybe-add-bunnies [em]
  (when @mouse-pressed
    (let [stage (get-global em :stage)]
      (dotimes [_ NEW_BUNNIES]
        (make-bunny em stage (:x @mouse-position) (:y @mouse-position)))
      (let [es (query-components em [:drawable :velocity])]
        (when (< MAX_BUNNIES (count es))
          (dotimes [n (- (count es) MAX_BUNNIES)]
            (let [e (get es n)]
              (.removeChild stage ((get-component e :drawable) :sprite))
              (.remove e))))))))

(defn process-events [em]
  (let [stage (get-global em :stage)
        current-events @event-bus]
    (doseq [[ev-type data] @event-bus]
      (when-let [h (event-handlers ev-type)]
        (h em data)))
    (vreset! event-bus [])))

(defn render-scene [em]
  (let [renderer (get-global em :renderer)
        stage (get-global em :stage)]
    (.render renderer stage)))

(defn game []
  (let [dom-node (atom false)
        mouse-state (atom {:mouse-pressed false})]
    (r/create-class
     {:display-name "game"
      :component-did-mount
      (fn [this]
        (reset! dom-node (r/dom-node this))
        (let [renderer (.autoDetectRenderer P W H)
              stage (P.Container.)
              em (EM/Manager. {:renderer renderer
                               :stage stage})
              loop-fn (fn loop []
                        (when @dom-node
                          (js/requestAnimationFrame loop))
                        (doto em
                          process-events
                          maybe-add-bunnies
                          bounce-update
                          move-update
                          render-scene))]
          (init-events stage em)
          (dotimes [x MAX_BUNNIES]
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
