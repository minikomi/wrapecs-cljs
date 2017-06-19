(ns ecspixi.constants
  (:require [cljsjs.pixi]))

(def P js/PIXI)
(def MAX_BUNNIES 10000)
(def NEW_BUNNIES 100)
(def W (.. js/window -document -body -clientWidth))
(def H (.. js/window -document -body -clientHeight))
