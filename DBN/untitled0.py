# -*- coding: utf-8 -*-
"""
Created on Fri Feb  1 16:06:30 2019

@author: 200019210
"""

import requests
import json
import sys

inputfile_json = sys.argv[1]
json_file = open(inputfile_json, 'r').read()
#ubl_input = json.load(json_file,cls=Decoder)
#json_file.close()

#%%
#r = requests.post('http://3.1.46.19:5000/process', json=json_file)
#r = requests.post('http://3.202.38.14:5000/process', json=json_file)
r = requests.post('http://3.202.38.14:5000/process')
print r.json()

