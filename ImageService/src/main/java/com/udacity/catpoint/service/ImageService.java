/*package com.udacity.catpoint.service;

public class Imageservice {
}*/
package com.udacity.catpoint.service;

import java.awt.image.BufferedImage;

public interface ImageService {
    boolean imageContainsCat(BufferedImage image, float confidenceThreshhold);
}