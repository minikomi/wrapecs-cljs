(ns ecspixi.events
  (:require [ecspixi.constants :as C]))

(def event-bus (volatile! []))
(def mouse-pressed (volatile! false))
(def mouse-position (volatile! nil))

(defn event!
  ([event-type]
   (vswap! event-bus conj [event-type nil]))
  ([event-type data]
   (vswap! event-bus conj [event-type data])))

(defn init-events [stage em]
  (set! (.-interactive stage) true)
  (set! (.-hitArea stage)
        (C/P.Rectangle. 0 0 C/W C/H))
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

(def event-handlers
  {:mouse-down
   (fn mouse-down-handler [_ data]
     (vreset! mouse-position data)
     (vreset! mouse-pressed true))
   :mouse-up
   (fn mouse-up-handler [_ _]
     (vreset! mouse-pressed false))
   :mouse-move
   (fn mouse-move-hanlder [_ data]
     (vreset! mouse-position data))})
