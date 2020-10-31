#include <jni.h>
#include <string>
#include <opencv2/opencv.hpp>

using namespace cv;

extern "C"
JNIEXPORT void JNICALL
Java_com_example_ihyelan_opencvuse_MainActivity_ConvertRGBtoGray(JNIEnv *env, jobject instance,
                                                                 jlong matAddrInput,
                                                                 jlong matAddrResult) {
    // 입력 RGBA 이미지를 GRAY 이미지로 변환
    Mat &matInput = *(Mat *)matAddrInput;
    Mat &matResult = *(Mat *)matAddrResult;
    cvtColor(matInput, matResult, COLOR_RGBA2GRAY);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_ihyelan_opencvuse_MainActivity_ConvertRGBtoSepia(JNIEnv *env, jobject instance,
                                                                 jlong matAddrInput,
                                                                 jlong matAddrResult) {
    // 입력 RGBA 이미지를 Sepia 이미지로 변환
    Mat &matInput = *(Mat *)matAddrInput;
    Mat &matResult = *(Mat *)matAddrResult;

    cv::Mat kern = (cv::Mat_<float>(4,4) <<  0.189, 0.769, 0.393, 0,
            0.168, 0.686, 0.349, 0,
            0.131, 0.534, 0.272, 0,
            0, 0, 0, 1);

    transform(matInput, matResult, kern);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_ihyelan_opencvuse_MainActivity_ConvertRGBtoRGB(JNIEnv *env, jobject instance,
                                                                 jlong matAddrInput,
                                                                 jlong matAddrResult) {
    Mat &matInput = *(Mat *)matAddrInput;
    Mat &matResult = *(Mat *)matAddrResult;
    cvtColor(matInput, matResult, COLOR_RGBA2RGB);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_ihyelan_opencvuse_MainActivity_ConvertRGBtoSharpen(JNIEnv *env, jobject instance,
                                                                    jlong matAddrInput,
                                                                    jlong matAddrResult) {
    // 입력 RGBA 이미지를 Sepia 이미지로 변환
    Mat &matInput = *(Mat *)matAddrInput;
    Mat &matResult = *(Mat *)matAddrResult;

    cv::Mat kern = (cv::Mat_<float>(4,4) <<  -1, -1, -1, 0,
            -1, 9, -1, 0,
            -1, -1, -1, 0,
            0, 0, 0, 1);

    filter2D(matInput, matResult, -1, kern);
}