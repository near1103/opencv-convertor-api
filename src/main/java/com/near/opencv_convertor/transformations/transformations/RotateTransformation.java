package com.near.opencv_convertor.transformations.transformations;

import com.near.opencv_convertor.transformations.ImageTransformation;
import com.near.opencv_convertor.transformations.TransformationParams;
import com.near.opencv_convertor.transformations.params.RotateParams;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

public class RotateTransformation implements ImageTransformation {

    @Override
    public Mat apply(Mat source, TransformationParams params) {
        RotateParams rotateParams = (RotateParams) params;
        double angle = rotateParams.angle();

        Point center = new Point(source.cols() / 2.0, source.rows() / 2.0);
        Mat rotationMatrix = Imgproc.getRotationMatrix2D(center, angle, 1.0);

        double absCos = Math.abs(rotationMatrix.get(0, 0)[0]);
        double absSin = Math.abs(rotationMatrix.get(0, 1)[0]);

        int newWidth = (int) Math.round(source.rows() * absSin + source.cols() * absCos);
        int newHeight = (int) Math.round(source.rows() * absCos + source.cols() * absSin);

        rotationMatrix.put(0, 2, rotationMatrix.get(0, 2)[0] + (newWidth / 2.0 - center.x));
        rotationMatrix.put(1, 2, rotationMatrix.get(1, 2)[0] + (newHeight / 2.0 - center.y));

        Mat result = new Mat();
        Imgproc.warpAffine(
                source,
                result,
                rotationMatrix,
                new Size(newWidth, newHeight),
                Imgproc.INTER_LINEAR,
                Core.BORDER_CONSTANT,
                new Scalar(0, 0, 0, 0)
        );

        rotationMatrix.release();
        return result;
    }
}