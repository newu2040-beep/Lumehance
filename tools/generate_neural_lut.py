#!/usr/bin/env python3
"""
Tools: Neural LUT & Weight Synthesizer
Generates optimal tone-mapping curves and quantized bilateral filter coefficients
for on-device real-time enhancement pipelines.
"""

import math
import json
import os

def generate_dynamic_range_lut():
    lut = []
    for i in range(256):
        # S-curve dynamic range expansion
        norm = i / 255.0
        boosted = 1.0 / (1.0 + math.exp(-6.0 * (norm - 0.5)))
        # Blend with linear
        blended = (norm * 0.3) + (boosted * 0.7)
        val = int(min(255, max(0, round(blended * 255.0))))
        lut.append(val)
    return lut

def generate_bilateral_spatial_weights(radius=2, sigma_spatial=1.5):
    weights = []
    for dy in range(-radius, radius + 1):
        row = []
        for dx in range(-radius, radius + 1):
            dist_sq = dx * dx + dy * dy
            w = math.exp(-dist_sq / (2.0 * sigma_spatial * sigma_spatial))
            row.append(round(w, 4))
        weights.append(row)
    return weights

def main():
    os.makedirs("generated_assets", exist_ok=True)
    lut_data = {
        "engine_version": "2.4.0-native",
        "dynamic_range_lut": generate_dynamic_range_lut(),
        "bilateral_kernel_weights": generate_bilateral_spatial_weights(),
        "color_matrix_presets": {
            "vibrant": [1.15, 0.0, 0.0, 0.0, 5.0, 0.0, 1.15, 0.0, 0.0, 5.0, 0.0, 0.0, 1.15, 0.0, 5.0, 0.0, 0.0, 0.0, 1.0, 0.0],
            "portrait": [1.05, 0.0, 0.0, 0.0, 8.0, 0.0, 1.02, 0.0, 0.0, 4.0, 0.0, 0.0, 1.0, 0.0, 2.0, 0.0, 0.0, 0.0, 1.0, 0.0]
        }
    }
    
    with open("generated_assets/neural_lut_manifest.json", "w") as f:
        json.dump(lut_data, f, indent=2)
    print("Successfully generated offline neural LUT manifest.")

if __name__ == "__main__":
    main()
