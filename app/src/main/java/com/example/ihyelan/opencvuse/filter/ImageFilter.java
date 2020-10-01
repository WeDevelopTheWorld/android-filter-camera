package com.example.ihyelan.opencvuse.filter;

import org.opencv.core.Mat;

public interface ImageFilter {
    void doFilter(Mat input);
}
