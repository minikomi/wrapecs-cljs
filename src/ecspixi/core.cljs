(ns ecspixi.core
  (:require
   [cljsjs.pixi]
   [reagent.core :as r]
   [ecspixi.events :as e]
   [ecspixi.systems :as s]
   [ecspixi.entities :as ent]
   [ecspixi.util :as u]
   [ecspixi.constants :refer [W H MAX_BUNNIES]]
   [ecs.EntityManager :as EM]))

(enable-console-print!)

(defn game []
  (let [dom-node (atom false)
        mouse-state (atom {:mouse-pressed false})]
    (r/create-class
     {:display-name "game"
      :component-did-mount
      (fn [this]
        (reset! dom-node (r/dom-node this))
        (let [renderer (.autoDetectRenderer js/PIXI W H)
              stage (js/PIXI.Container.)
              em (EM/Manager. {:renderer renderer
                               :stage stage})
              loop-fn (fn loop []
                        (when @dom-node
                          (js/requestAnimationFrame loop))
                        (s/run-systems em))]
          ;; init
          (e/init-events stage em)
          (dotimes [x MAX_BUNNIES]
            (ent/make-bunny em stage (rand-int W) (+ 10 (rand-int (- H 10)))))
          (.appendChild @dom-node (.-view renderer))
          ;; big bang
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
