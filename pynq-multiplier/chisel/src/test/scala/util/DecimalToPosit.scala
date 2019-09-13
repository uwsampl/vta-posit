package unittest.util

import scala.io.Source

class DecimalToPosit() {
  var positList = scala.collection.mutable.Map[Double, Int]()// map of decimals -> posits
  val filename = "src/test/resources/posit8_1_Table.csv"
  for (line <- Source.fromFile(filename).getLines.drop(1)) {
    val cols = line.split(",").map(_.trim)
    //println(s"${cols(0)}|${cols(1)}")
    if (cols(0) == "Double.NaN") {
      positList += (Double.NaN -> cols(1).toInt)
    } else {
      positList += (cols(0).toDouble -> cols(1).toInt)
    }
  } 
  val decimalList = for ((k,v) <- positList) yield (v, k)// map of posits -> decimals
  
  /** Returns the Posit associated with the input Double, or NaN */
  def singlePosit(x: Double): Int = {
    positList.getOrElse(x, 128)
  }
  /** Returns an Array including all Posits as Ints */
  def arrayOfPosits(): Array[Int] = {
    positList.values.toArray
  }
  /** Returns true if the Int input is a valid sized Posit, false otherwise */
  def validPosit(inQuestion: Int): Boolean = {
    positList.values.toList.contains(inQuestion)
  }
  
  /** Returns the Double associated with the input Posit (as an Int) */
  def decimalOf(posit: Int): Double = {
    decimalList.getOrElse(posit, 0.0)
  }
  
  /** Returns true if the input Double is represented by a Posit, false otherwise */
  def validDouble(inQuestion: Double): Boolean = {
    positList.contains(inQuestion)
  }

}
