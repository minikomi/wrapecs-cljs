(ns ecspixi.systems
  (:require [ecspixi.util :as u]
            [ecspixi.events :as e]
            [ecspixi.entities :as ent]
            [ecspixi.constants :refer [MAX_BUNNIES NEW_BUNNIES W H]]))

(defn process-events [em]
  (let [stage (u/get-global em :stage)
        current-events @e/event-bus]
    (doseq [[ev-type data] @e/event-bus]
      (when-let [h (e/event-handlers ev-type)]
        (h em data)))
    (vreset! e/event-bus [])))

(defn maybe-add-bunnies [em]
  (when @e/mouse-pressed
    (let [stage (u/get-global em :stage)]
      ;; add new bunnies
      (dotimes [_ NEW_BUNNIES]
        (ent/make-bunny em stage (:x @e/mouse-position) (:y @e/mouse-position)))
      ;; take the number down to MAX_BUNNIES
      (let [es (u/query-components em [:drawable :velocity])]
        (when (< MAX_BUNNIES (count es))
          (dotimes [n (- (count es) MAX_BUNNIES)]
            (let [e (get es n)]
              (.removeChild stage ((u/get-component e :drawable) :sprite))
              (.remove e))))))))

(defn bounce-update [em]
  (doseq [e (u/query-components em [:drawable :velocity])]
    (let [x ((u/get-component e :drawable) :x)
          y ((u/get-component e :drawable) :y)
          vel (u/get-component e :velocity)]
      (when (or (>= 0 x) (<= W x))
        (.bounce-x vel))
      (if (or (>= 0 y) (<= H y))
        (.bounce-y vel)
        (.gravity vel)))))

(defn move-update [em]
  (doseq [e (u/query-components em [:drawable :velocity])]
    (let [drw (u/get-component e :drawable)
          vel (u/get-component e :velocity)]
      (.position-set drw
                     (+ (drw :x) (* 0.5 (vel :dx)))
                     (+ (drw :y) (* 0.5 (vel :dy)))))))

(defn render-scene [em]
  (let [renderer (u/get-global em :renderer)
        stage (u/get-global em :stage)]
    (.render renderer stage)))

(defn run-systems [em]
  (doto em
   process-events
   maybe-add-bunnies
   bounce-update
   move-update
   render-scene))
