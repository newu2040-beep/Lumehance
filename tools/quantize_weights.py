#!/usr/bin/env python3
"""
Tools: Weight Quantizer for On-Device SIMD Kernels
Converts FP32 filter kernels into fixed-point INT8 arrays for zero-allocation
C++ and Kotlin SIMD execution.
"""

def quantize_fp32_to_int8(values, scale=127.0):
    return [int(min(127, max(-128, round(v * scale)))) for v in values]

if __name__ == "__main__":
    test_kernel = [0.05, 0.15, 0.60, 0.15, 0.05]
    quantized = quantize_fp32_to_int8(test_kernel)
    print(f"Quantized Kernel (INT8): {quantized}")
