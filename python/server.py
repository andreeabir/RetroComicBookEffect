
import base64
import json

from flask import Flask, request, Response
from comic_book_effect import ComicBookEffect
import requests

app = Flask(__name__)


def upload_on_imgbb(file_name):
    key_imgbb = '05fc35834a5f79d64dc92052069a42c7'
    with open(file_name, "rb") as file:
        url = "https://api.imgbb.com/1/upload"
        payload = {
            "key": key_imgbb,
            "image": base64.b64encode(file.read()),
        }
        res = requests.post(url, payload)
        result = json.loads(res.content.decode('utf-8'))
        return result['data']['display_url']


@app.route('/', methods=['POST'])
def test():
    print('here')
    imgdata = base64.b64decode(request.values.get('image'))
    print('receive')
    image_name = "img.jpeg"
    with open(image_name, 'wb') as f:
        f.write(imgdata)

    comicBookEffect = ComicBookEffect(alpha_blending=0.3)
    step1, step2, step3, step4 = comicBookEffect.process(image_name)
    step1.save('step1.png')
    step2.save('step2.png')
    step3.save('step3.png')
    step4.save('step4.png')

    url_step1 = upload_on_imgbb('step1.png')
    url_step2 = upload_on_imgbb('step2.png')
    url_step3 = upload_on_imgbb('step3.png')
    url_step4 = upload_on_imgbb('step4.png')
    res = json.dumps([{'step1': url_step1, 'step2': url_step2, 'step3': url_step3, 'step4': url_step4}])
    print('send')
    return Response(res,
                    status=200,
                    mimetype="application/json"
                    )

app.run(host='192.168.0.183', port=65432)