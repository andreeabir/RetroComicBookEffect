import base64

import ast
import json

key_imgbb = '05fc35834a5f79d64dc92052069a42c7'
with open("Evrik.jpeg", "rb") as file:
    url = "https://api.imgbb.com/1/upload"
    payload = {
        "key": key_imgbb,
        "image": base64.b64encode(file.read()),
    }
    res = requests.post(url, payload)
    res_str = res.content.decode('UTF-8')
    result = json.loads(res.content.decode('utf-8'))
    print(result['data']['display_url'])