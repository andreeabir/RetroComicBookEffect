import random

import cv2
import numpy as np
from PIL import Image, ImageDraw, ImageStat, ImageFont
from sklearn.cluster import MiniBatchKMeans


class ComicBookEffect(object):
    def __init__(self, sample=8, scale=1, percentage=50, angles=[0, 15, 30, 45], contrast=False, quantization=True,
                 grain=False, alpha_blending=0.3, add_comic_text_on_image=True):
        self.sample = sample
        self.angles = angles
        self.scale = scale
        self.percentage = percentage
        self.quantization = quantization
        self.contrast = contrast
        self.grain = grain
        self.alpha = alpha_blending
        self.add_comic_text_on_image = add_comic_text_on_image
        self.comic_texts = open('./comic_texts.txt').readline().split(';')

    def process(self, input_path, out_path=None):
        img = Image.open(input_path)
        original = np.array(img).copy()

        step1_img = img.copy()
        if self.contrast:
            step1_img = self.apply_contrast(img)

        step2_img = step1_img.copy()
        if self.quantization:
            step2_img = self.quantization_image(step1_img, num_clusters=30)

        step3_img = step2_img.copy()
        if self.grain:
            step3_img = self.apply_grain(step2_img)

        cmyk = self.gcr(step3_img)
        dots = self.halftone(step3_img, cmyk)
        step4_img = Image.merge('CMYK', dots)
        step4_img = step4_img.convert('RGB')
        step4_img = cv2.cvtColor(np.array(step4_img), cv2.COLOR_RGB2BGR)
        original = cv2.cvtColor(np.array(step3_img), cv2.COLOR_RGB2BGR)

        out = step4_img * self.alpha + (1 - self.alpha) * original
        out = cv2.cvtColor(out.astype('float32'), cv2.COLOR_BGR2RGB)

        if out_path is not None:
            cv2.imwrite(out_path, out)

        if self.add_comic_text_on_image is True:
            out = self.add_comic_text(out)

        return step1_img, step2_img, step3_img, out

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
                    diameter = (mean / 255) ** 0.5 * 0.7
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

    def add_comic_text(self, image):
        h, w, c = image.shape

        margin_size = 3
        h_size = min(int(h / 6), 100)
        w_size = min(int(w / 3), 200)

        image[0:h_size, 0:w_size, :] = 30
        image[margin_size:h_size - margin_size, margin_size:w_size - margin_size, :] = 80
        image = image.astype(np.uint8)

        img = Image.fromarray(image)
        draw = ImageDraw.Draw(img)
        font = ImageFont.truetype("./font/comicbook.ttf", 25)

        comic_text = self.comic_texts[random.randint(0, len(self.comic_texts) - 1)]
        draw.text((margin_size + 5, margin_size + 5), comic_text, (0, 0, 255), font=font)
        return img

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
    effect.process("../examples/img.jpg", "../examples/comic_effect_film-grain2.jpg")
