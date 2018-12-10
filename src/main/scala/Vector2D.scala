package main.scala


class Vector2D(_x: Double, _y: Double) {
  var x: Double = _x
  var y: Double = _y
}

object Vector2D {
  def add(a: Vector2D, b: Vector2D): Vector2D = {
    new Vector2D(a.x + b.x, a.y + b.y)
  }
}
