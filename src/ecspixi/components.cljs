(ns ecspixi.components)

(deftype Velocity [^:mutable dx ^:mutable dy]
  IFn
  (-invoke [this kw]
    (case kw
      :dx dx
      :dy dy
      nil))
  Object
  (velocity-set [v dx' dy']
    (set! dx dx')
    (set! dy dy'))
  (bounce-x [_] (set! dx (- dx)))
  (bounce-y [_] (set! dy (- dy)))
  (gravity [_] (set! dy (inc dy))))

(deftype Drawable [^:mutable sprite]
  IFn
  (-invoke [this kw]
    (case kw
      :x (.-x (.-position sprite))
      :y (.-y (.-position sprite))
      :sprite sprite
      nil))
  Object
  (position-set [_ x y]
    (.set (.-position sprite) x y)))
