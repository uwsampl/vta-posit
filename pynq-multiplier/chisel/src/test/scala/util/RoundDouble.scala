package unittest.util

import unittest.util
import scala.util.control.Breaks._

class RoundDouble() {
  val posits = new DecimalToPosit
  
  /** Rounds the input Double to the nearest Double represented by an (8,1) Posit 
   *
   *  This does not exactly work. 
   *  As the Rounding of Posits' fraction and exponent cannot be properly emulated
   *  by Doubles here, the result is only within +/- 1 of the output of the posit multiplier.
   */
  def roundToNearestEven(in: Double): Double = {
    var roundedResult: Double = 0.0
    var exactResult: Double = in
    var diffToLesser: Double  = 0.0
    var diffToGreater: Double = 0.0
    if (exactResult == Double.NaN) {
      roundedResult = exactResult
    } else if (exactResult >= 0) {
      if (exactResult >= 1) {
        breakable {
        for (i <- 64 to 127) {
          if (exactResult == posits.decimalOf(i) || ((exactResult > posits.decimalOf(i)) && i == 127)) {
            roundedResult = posits.decimalOf(i)
            break
          } else if (exactResult <  posits.decimalOf(i)) {
	    /*diffToLesser = exactResult - posits.decimalOf(i - 1)
            diffToGreater = posits.decimalOf(i) - exactResult
            if (diffToLesser > diffToGreater) {  
              roundedResult = posits.decimalOf(i)
              break
            } else if (diffToLesser < diffToGreater) {
	      roundedResult = posits.decimalOf(i-1)
              break
            } else {*/
              if (posits.decimalOf(i - 1).floor % 2 == posits.decimalOf(i).floor % 2) {
	        diffToLesser = exactResult - posits.decimalOf(i - 1)
                diffToGreater = posits.decimalOf(i) - exactResult
                if (diffToLesser > diffToGreater) {  
                  roundedResult = posits.decimalOf(i)
                  break
                } else if (diffToLesser < diffToGreater) {
                  roundedResult = posits.decimalOf(i-1)
                  break
                } else {
                   roundedResult = posits.decimalOf(i)
                   break
                }
              } else if (posits.decimalOf(i).floor % 2 == 0.0) {
                roundedResult = posits.decimalOf(i)
	        break
              } else {
	        roundedResult = posits.decimalOf(i - 1)
                break
              }
           // }
          }
        }
        }
      } else {
        breakable {
        for (i <- 1 to 64) { //TODO: Is there a special case for double between 0 and lowest posit?
          if (exactResult == posits.decimalOf(i)) {
            roundedResult = posits.decimalOf(i)
            break
          } else if (exactResult <  posits.decimalOf(i)) {
	    if (i == 1) {
              roundedResult = posits.decimalOf(i)
              break 
            } else {
              diffToLesser = exactResult - posits.decimalOf(i - 1)
              diffToGreater = posits.decimalOf(i) - exactResult
              if (diffToLesser > diffToGreater) {  
                roundedResult = posits.decimalOf(i)
                break
              } else if (diffToLesser < diffToGreater) {
	        roundedResult = posits.decimalOf(i-1)
                break
              } else {
	        if (posits.decimalOf(i - 1) == 0.0) {
	          roundedResult = posits.decimalOf(i)
                  break
                } else if (posits.decimalOf(i - 1) % 2 == posits.decimalOf(i) % 2) {
	          roundedResult = posits.decimalOf(i)
                  break
                } else if (posits.decimalOf(i) % 2 == 0.0) {
                  roundedResult = posits.decimalOf(i)
                  break
	        } else {
	          roundedResult = posits.decimalOf(i - 1)
                  break
                }
              }
            }
          }
        }
        }
      }
    } else {
      if (exactResult <= -1) {
        breakable {
          for (i <- 129 to 192) {
          if (exactResult == posits.decimalOf(i) || ((exactResult < posits.decimalOf(i)) && i == 129)) {
            roundedResult = posits.decimalOf(i)
            break
          } else if (exactResult <  posits.decimalOf(i)) {
	    diffToLesser = exactResult - posits.decimalOf(i - 1)
            diffToGreater = posits.decimalOf(i) - exactResult
            if (diffToLesser > diffToGreater) {
	      roundedResult = posits.decimalOf(i)
              break
            } else if (diffToLesser < diffToGreater) {
	      roundedResult = posits.decimalOf(i-1)
              break
            } else {
              if (posits.decimalOf(i - 1).floor % 2 == posits.decimalOf(i).floor  % 2) {
	        roundedResult = posits.decimalOf(i)
                break
              } else if (posits.decimalOf(i).floor  % 2 == 0.0) {
                roundedResult = posits.decimalOf(i)
	        break
              } else {
	        roundedResult = posits.decimalOf(i - 1)
                break
              }
            }
          }
        }
        }
      } else {
        breakable {
        for (i <- 192 until 256) {
          if (exactResult == posits.decimalOf(i)) {
            roundedResult = posits.decimalOf(i)
            break
          } else if (exactResult <  posits.decimalOf(i)) {
	    diffToLesser = exactResult - posits.decimalOf(i - 1)
            diffToGreater = posits.decimalOf(i) - exactResult
            if (diffToLesser > diffToGreater) {
	      roundedResult = posits.decimalOf(i)
              break
            } else if (diffToLesser < diffToGreater) {
	      roundedResult = posits.decimalOf(i-1)
              break
            } else {
              if (posits.decimalOf(i - 1).floor  % 2 == posits.decimalOf(i).floor  % 2) {
	        roundedResult = posits.decimalOf(i)
                break
              } else if (posits.decimalOf(i).floor % 2 == 0.0) {
                roundedResult = posits.decimalOf(i)
                break
              } else {
	        roundedResult = posits.decimalOf(i - 1)
                break
              }
            }
          } else if ((exactResult > posits.decimalOf(i)) && i == 255) {
              diffToLesser = exactResult - posits.decimalOf(i - 1)
              diffToGreater = 0.0 - exactResult
              if (diffToLesser > diffToGreater) {
	        roundedResult = posits.decimalOf(i)
                break
              } else if (diffToLesser < diffToGreater) {
	        roundedResult = posits.decimalOf(i-1)
                break
              } else {
                roundedResult = 0.0
                break
              }
          }
        }
      }
      }
    }  
    return roundedResult
  }
}
