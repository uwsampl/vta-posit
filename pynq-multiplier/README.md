# Posit Multiplier

A posit operator written in Chisel, based on Verilog files from https://github.com/facebookresearch/deepfloat.git 
The chisel can be found in [`chisel/src/main/scala/posit`](./pynq-multiplier/chisel/src/main/scala/posit).
The multiply operation from packed posit inputs to a packed posit output includes four major stages :

- **Decode**: First the packed posits are converted to unpacked posits, which include an exponent, fraction, and bits for the the sign of the posit, whether it’s zero, and whether it’s infinity. If two’s complement is used in the Packed Posits, they are converted to sign-magnitude before being decoded.

- **Multiply**: Multiplication happens with the unpacked posits, similar to floating point multiplication. If overflow happens, the result is set to the max posit value (not infinity). If underflow occurs, the result is set to the minimum posit value (not zero). 

- **RoundToNearestEven**: Rounding of the fraction and exponent occur if necessary for the result to be a valid posit.

- **Encoding**: The result is converted back to packed form, and two’s complement if necessary.

(The decode, round, and encode stages are not specific to multiplication.)

A unit test is located in [`chisel/src/test/scala`](./pynq-multiplier/chisel/src/test/scala/PositMultiplyTester.scala). To run it, navigate to `pynq-multiplier/chisel`
and run `make test`. It tests several different cases of multiplication, 
as well as printing results from random inputs. Currently, the chisel test cannot properly simulate the behavior of the posit rounding, 
so it can't correctly check the results from random trials (only getting within plus or minus one posit of the correct answer).

Currently, registers exist between the decode and multiply stages, and within the multiplication to create a 3 stage pipeline, capable of running at ~133 MHz.

To Do:
- Incorporate the conversion of two’s complement to sign magnitude directly inside the decode and encode stages.
- Remove any logic inside the rounding modules for rounding up from zero. This is leftover from the adapted design that originally allowed underflow to 0, which was not correct posit behavior and is no longer implemented.
- Add scala documentation comments to classes missing them.
- Enable testing outside of chisel, to be able to accurately simulate the posit multiplication and rounding, and allow easier testing of random inputs and multiplication with posits that have different (n, es) values.
- Investigate what to do with decoding, encoding, and rounding once more operators are introduced (as they don't necessarily 
need to be connected to a single specific operation).
