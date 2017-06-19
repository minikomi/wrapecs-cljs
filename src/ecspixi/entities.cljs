(ns ecspixi.entities
  (:require [ecspixi.constants :as C]
            [ecspixi.util :as u]
            [ecspixi.components :as com]))

(def bunny-texture (.fromImage C/P.Texture "bunnys.png"))

(def textures
  (mapv (fn [y]
          (C/P.Texture.
           (.-baseTexture bunny-texture)
           (C/P.Rectangle. 2 y 26 37)))
        [47 86 125 164 2]))

(defn get-sprite []
  (C/P.Sprite. (rand-nth textures)))

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
