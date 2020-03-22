from PIL import Image, ImageDraw, ImageStat
import cv2
from sklearn.cluster import MiniBatchKMeans
import numpy as np


class ComicBookEffect(object):
    def __init__(self, sample=8, scale=1, percentage=50, angles=[0,15,30,45], contrast=False, quantization=True,
                 grain=False, alpha_blending=0.4):
        self.sample = sample
        self.angles = angles
        self.scale = scale
        self.percentage = percentage
        self.quantization = quantization
        self.contrast = contrast
        self.grain = grain
        self.alpha = alpha_blending

    def process(self, input_path, out_path):
        img = Image.open(input_path)
        original = np.array(img).copy()

        if self.contrast:
            img = self.apply_contrast(img)

        if self.grain:
            img = self.apply_grain(img)

        if self.quantization:
            img = self.quantization_image(img, num_clusters=10)

        cmyk = self.gcr(img)
        dots = self.halftone(img, cmyk)
        img = Image.merge('CMYK', dots)
        img = img.convert('RGB')
        img = cv2.cvtColor(np.array(img), cv2.COLOR_RGB2BGR)
        original = cv2.cvtColor(original, cv2.COLOR_RGB2BGR)

        out = img * self.alpha + (1 - self.alpha) * original
        cv2.imwrite(out_path, out)

    def halftone(self, img, cmyk):
        cmyk = cmyk.split()
        dots = []

        for channel, angle in zip(cmyk, self.angles):
            channel = channel.rotate(angle, expand=1)
            size =channel.size[0] * self.scale, channel.size[1] * self.scale
            half_tone = Image.new('L', size)
            draw = ImageDraw.Draw(half_tone)

            for x in range(0, channel.size[0], self.sample):
                for y in range(0, channel.size[1], self.sample):
                    box = channel.crop((x, y, x + self.sample, y + self.sample))
                    mean = ImageStat.Stat(box).mean[0]
                    diameter = (mean / 255) ** 0.5
                    box_size = self.sample * self.scale
                    draw_diameter = diameter * box_size
                    box_x, box_y = (x * self.scale), (y * self.scale)
                    x1 = box_x + ((box_size - draw_diameter) / 2)
                    y1 = box_y + ((box_size - draw_diameter) / 2)
                    x2 = x1 + draw_diameter
                    y2 = y1 + draw_diameter

                    draw.ellipse([(x1, y1), (x2, y2)], fill=255)

            half_tone = half_tone.rotate(-angle, expand=1)
            width_half, height_half = half_tone.size

            # Top-left and bottom-right of the image to crop to:
            xx1 = (width_half - img.size[0] * self.scale) / 2
            yy1 = (height_half - img.size[1] * self.scale) / 2
            xx2 = xx1 + img.size[0] * self.scale
            yy2 = yy1 + img.size[1] * self.scale

            half_tone = half_tone.crop((xx1, yy1, xx2, yy2))
            dots.append(half_tone)
        return dots

    def apply_contrast(self, image):
        return image

    def apply_grain(self, image):
        return image

    def quantization_image(self, image, num_clusters=10):
        image = np.array(image)
        image = cv2.cvtColor(image, cv2.COLOR_RGB2BGR)
        (h, w) = image.shape[:2]
        image = cv2.cvtColor(image, cv2.COLOR_BGR2LAB)
        image = image.reshape((image.shape[0] * image.shape[1], 3))

        # apply k-means using the specified number of clusters and
        # then create the quantized image based on the predictions
        clt = MiniBatchKMeans(n_clusters=num_clusters)
        labels = clt.fit_predict(image)
        quant = clt.cluster_centers_.astype("uint8")[labels]
        quant = quant.reshape((h, w, 3))

        # convert from L*a*b* to RGB
        quant = cv2.cvtColor(quant, cv2.COLOR_LAB2BGR)
        quant = Image.fromarray(cv2.cvtColor(quant, cv2.COLOR_BGR2RGB))
        return quant

    def gcr(self, image):
        """
        Basic "Gray Component Replacement" function. Returns a CMYK image with
        percentage gray component removed from the CMY channels and put in the
        K channel, ie. for percentage=100, (41, 100, 255, 0) >> (0, 59, 214, 41)
        """
        cmyk_im = image.convert('CMYK')
        if not self.percentage:
            return cmyk_im
        cmyk_im = cmyk_im.split()
        cmyk = []
        for i in range(4):
            cmyk.append(cmyk_im[i].load())
        for x in range(image.size[0]):
            for y in range(image.size[1]):
                gray = min(cmyk[0][x,y], cmyk[1][x,y], cmyk[2][x,y]) * self.percentage / 100
                for i in range(3):
                    cmyk[i][x,y] = cmyk[i][x,y] - int(gray)
                cmyk[3][x,y] = int(gray)
        return Image.merge('CMYK', cmyk_im)


if __name__ == "__main__":
    effect = ComicBookEffect()
    effect.process("examples/film-grain.jpg", "examples/comic_effect_film-grain.jpg")