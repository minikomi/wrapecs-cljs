(ns ecspixi.entities
  (:require [ecspixi.util :as u]
            [ecspixi.components :as com]))

(def bunny-texture (.fromImage js/PIXI.Texture "bunnys.png"))

(def textures
  (mapv (fn [y]
          (js/PIXI.Texture.
           (.-baseTexture bunny-texture)
           (js/PIXI.Rectangle. 2 y 26 37)))
        [47 86 125 164 2]))

(defn get-sprite []
  (js/PIXI.Sprite. (rand-nth textures)))

(defn make-bunny [em stage x y]
  (let [bunny (.createEntity em)
        sprite (get-sprite)]
    (.addChild stage sprite)
    (.set (.-position sprite) x y)
    (set! (.-rotation sprite) (- (rand) 0.5))
    (u/add-component bunny
                     :drawable
                     (com/Drawable. sprite))
    (u/add-component bunny
                     :velocity
                     (com/Velocity. (- (rand-int 20) 10)
                                    (- (+ 10 (rand-int 5)))))))
