#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
File name: dbnrisk_wrapper.py
Creation date: 15-03-2017 11:32:35
Last modified: 12-05-2017 13:23:43
@author: 212359868  
"""

import numpy as np
import matplotlib.pyplot as plt
import pandas as pd

import traceback
import logging
import json
import os
import copy
import time
import sys
import cPickle as pickle
import decimal
import datetime
from dateutil.parser import parse
import pytz

from genDBNinput import write_expert_knowledge,write_prior_data,write_observation_data
from gedbn.Preprocessing import writeInputJson
from gedbn.Postprocessing import plotUpdatedBHM
from gedbn.runDBN import run
from gedbn.Postprocessing import postprocessing_PF

from flask import Flask, request, Response, jsonify

#%%%
def RunDBNRisk(ubl_input,log_instance):
    work_dir = ubl_input['workDir'] 
    if ubl_input['analyticSettings']['DBNSetup']['WorkDir']!=work_dir:
        ubl_input['analyticSettings']['DBNSetup']['WorkDir'] = work_dir + '/'
        ubl_input['analyticSettings']['DBNSetup']['ParticleFilterOptions']['BackendFname'] = work_dir + '/Results/'
    #model_details = copy.deepcopy(ubl_input)
    model_details = {}
    model_details['modelStatus'] = ""        

    # Generate input json to DBN
    log_instance.info("Reading user input ...")
    #dbn_input_file = os.path.join(work_dir,ubl_input['analyticSettings']['dbnInputFile'])
    #dbn_input_object = writeInputJson.DBN_Input(dbn_input_file)
    dbn_input = ubl_input['analyticSettings']#bn_input_object.dbn_input
    pickle.dump([dbn_input], open( os.path.join(work_dir,"dbn_input.pkl"), "wb" ) )  
    log_instance.info("Input json file generated.")
    
    # Run DBN to update the parabola BHM model
    t0 = time.time()
    
    DBN_setup_file = os.path.join(work_dir,'dbn_input.pkl')
    time_step_total = dbn_input['DBNSetup']['TrackingTimeSteps']
    n_steps = time_step_total
    log_instance.info("Total time steps = %d"%(time_step_total))
    
    if 'TrackingBatchNumbers' in dbn_input['DBNSetup'].keys():
        batch_step_total = dbn_input['DBNSetup']['TrackingBatchNumbers']
        n_steps = batch_step_total
        log_instance.info("Total batch steps = %d"%(batch_step_total))
    
    number_samples = dbn_input['DBNSetup']['NumberOfSamples']
    log_instance.info("Total number of particles = %d"%(number_samples))

    pfdbn = run(DBN_setup_file, n_steps, number_samples, log_instance)

    dbn_model_results = pickle.load(open(os.path.join(work_dir,'Results/dbn_model_latest.pkl'),'r'))
    nodeIndices = [x in dbn_model_results.keys() or x+'_outputs' in dbn_model_results.keys() for x in ubl_input['analyticSettings']['Nodes'].keys()]
    results = {}
    for ind, node in enumerate(ubl_input['analyticSettings']['Nodes'].keys()):
        results[node] = {}
        if nodeIndices[ind]:
            if node in dbn_model_results.keys():
                nodename = node
            elif node+'_outputs' in dbn_model_results.keys():
                nodename = node+'_outputs'

            results[node]['mean'] = np.mean(dbn_model_results[nodename][0])
            results[node]['std'] = np.std(dbn_model_results[nodename][0])

    model_details['results'] = results

    model_details['modelStatus'] = 'Success'
    model_details['statusMessage'] = 'DBN risk assessment completed successfully.'

    return model_details, pfdbn

class StreamToLogger(object):
    """
    Fake file-like stream object that redirects writes to a logger instance.
    """
    def __init__(self, logger, log_level=logging.INFO):
        self.logger = logger
        self.log_level = log_level
        self.linebuf = ''
 
    def write(self, buf):
        for line in buf.rstrip().splitlines():
            self.logger.log(self.log_level, line.rstrip())

def printUsage():
    print ""
    print "Usage:"
    print sys.argv[0] + " <path_to_input.json>"
    print ""
    
def as_float(obj):
    """Checks each dict passed to this function if it contains the key "value"
    Args:
        obj (dict): The object to decode

    Returns:
        dict: The new dictionary with changes if necessary
    """
    if "value" in obj:
        obj["value"] = float(obj["value"])
    return obj

class Decoder(json.JSONDecoder):
     def decode(self, s):
         result = super(Decoder, self).decode(s)
         return self._decode(result)
     def _decode(self, o):
         if isinstance(o, str) or isinstance(o, unicode):
             try:
                 return int(o)
             except ValueError:
                 try:
                     return float(o)
                 except ValueError:
                     return str(o)
         elif isinstance(o, dict):
             return {str(k): self._decode(v) for k, v in o.items()}
         elif isinstance(o, list):
             return [self._decode(v) for v in o]
         else:
             return o
         
#%%%
def addBatchIndex(ubl_input):
    ubl_input['analyticSettings']['DBNSetup']['BatchIndexName'] = 'batch_index'
    nodeKeys = ubl_input['analyticSettings']['Nodes'].keys()
    
    indicesTimeNodes = [i for i, s in enumerate(nodeKeys) \
                      if ubl_input['analyticSettings']['Nodes'][s]['Type'] == 'Stochastic_Transient' and \
                      ubl_input['analyticSettings']['Nodes'][s]['Tag'][0].lower()=='operating-data']
    if indicesTimeNodes is not None:
        nSteps = len(ubl_input['analyticSettings']['Nodes'][nodeKeys[indicesTimeNodes[0]]]['ObservationData'])
        
        ubl_input['analyticSettings']['DBNSetup']['TrackingBatchNumbers'] = nSteps
    
        ubl_input['analyticSettings']['Nodes']['batch_index'] = {'Children':[],'Distribution':'','DistributionParameters':{},'Parents':[],'Type':"Constant",'Value':[0L],'Tag':''}
        
        dummyObjList = []
        
        if nSteps>1 and ubl_input['taskName']!='predict':
            for i in range(nSteps):
                dummyObj = {}
                dummyObj['TimeIndexRange'] = [i,i+1]
                dummyObjList.append(dummyObj)
        else:
            for i in range(nSteps+1):
                dummyObj = {}
                if i==0:
                    dummyObj['TimeIndexRange'] = [i,i+1]
                else:
                    dummyObj['TimeIndexRange'] = [i-1,i]    
                dummyObjList.append(dummyObj)
            
        ubl_input['analyticSettings']['Nodes']['batch_index']['ObservationData'] = dummyObjList
    
    return ubl_input
#%%%%
def runDBN(ubl_input,log_instance):
    model_name = ubl_input['modelName']
    task_name = ubl_input['taskName']
    model_run_loc = ubl_input['workDir']
    logFileName = model_name + '.log'
    if ('logFile' in ubl_input.keys()):
        logFileName=ubl_input['logFile']
    logFilePath = os.path.join(model_run_loc,logFileName)
    logging.basicConfig(format='%(message)s',filename=logFilePath,filemode='w',level=logging.INFO)
    #sl = StreamToLogger(log_instance, logging.INFO)
    #sys.stdout = sl
    
    line = '==============================================================================='

    log_instance.info(line)
    log_instance.info('Local time: %s'%(time.strftime("%a %b %d %H:%M:%S %Y")))
    log_instance.info('DBN risk assessment %s has started.'%task_name)


    sys.path.append(ubl_input['workDir'])
    
    print 'inside runDBN'
    
    model_details, pfdbn = RunDBNRisk(ubl_input,log_instance)
    
    print 'Ran runDBNRisk'
    
    # work_dir = ubl_input['workDir']
    # json_file_name = 'Results/node_statistics.json'
    # node_stats = json.load(open(os.path.join(work_dir,json_file_name),'rb'))
    #
    # model_details['results'] = {}
    # model_details['results']['nodeStatistics'] = node_stats
    
    modelFilesList = []
    modelFilesTypeList = []
    
    
    model_details['modelFilesList'] = modelFilesList
    model_details['modelFilesTypeList'] = modelFilesTypeList
    
    return model_details, pfdbn
#%%%%
# Create Flask application object
app = Flask(__name__)

@app.route('/hello',methods=['POST'])
def hello():
    print "Welcome to ASKE Execution Bot! Let's do some queries!"
    return json.dumps({"modelStatus": "Success", "statusMessage": "Welcome to ASKE Execution Bot! Let's do some queries!"})

@app.route('/process',methods = ['POST'])
def process():
    # Read input json data
    try:
        log_instance = logging.getLogger('root')
    except Exception:
        error_message = traceback.format_exc()
        output_json = {}
        output_json['modelStatus'] = 'Failure'
        output_json['statusMessage'] = error_message + ' Issues with folders for model execution, exiting...'
        output_response = Response(output_json, status=500,
                                   mimetype='application/json')
        return output_response
    try:
        in_json = request.json
        #print in_json
        ubl_input = json.loads(in_json,cls=Decoder)

    except Exception:
        #Error in reading in the json
        error_message = traceback.format_exc()

        output_json = {}
        output_json['modelStatus'] = 'Failure'
        output_json['statusMessage'] = error_message + ' Issues with the input json, exiting...'
        output_response = Response(output_json, status=500,
                                   mimetype='application/json')
        return output_response
    try:
        model_details, pfdbn = runDBN(ubl_input,log_instance)
        #print output_dict
        # Return data in json format
        print 'Completed dbn run, extracted info'
        output_json = json.dumps(model_details)
        task_name = ubl_input['taskName']
        print output_json
        output_response = Response(output_json, status = 200,
                                   mimetype = 'application/json')
        
        #log_instance.info("Successfully wrote %s"%(model_details_json_filename))
        log_instance.info('Local time: %s'%(time.strftime("%a %b %d %H:%M:%S %Y")))
        log_instance.info('DBN risk assessment %s has finished.'%task_name)
        logging.shutdown()
        return output_response
    except Exception:
        error_message = traceback.format_exc()
        log_instance.error(error_message)
        work_dir = ubl_input['workDir']
        model_details_json_filename = os.path.join(work_dir,'ModelDetails.json')
        
        try:
            if os.path.exists(model_details_json_filename):
                model_details = json.load(open(model_details_json_filename,'rb'))
            elif os.path.exists(os.path.join(work_dir,'ModelDetails-input.json')):
                model_details = json.load(open(os.path.join(work_dir,'ModelDetails-input.json'),'rb'))

            else:
                model_details = {}
            model_details['modelStatus'] = "Failure"
            model_details['statusMessage'] = error_message
            
        except Exception:
            pass
        output_json = json.dumps(model_details)
        output_response = Response(output_json, status=500,
                                   mimetype='application/json')
        logging.shutdown()
        return output_response
        
        # model_details['analyticSettings'] = ubl_input['analyticSettings']
        #
        # model_details_json_file = open(model_details_json_filename, 'w')
        # json.dump(model_details, model_details_json_file, sort_keys=True)
        # model_details_json_file.close()


        
    
#%%%
if __name__=='__main__':
    
    try:
        line = '==============================================================================='
        
        # Adding capability to read json string from stdin
        if sys.argv[1][0] == '{':
            ubl_input = json.loads(sys.argv[1])
            ubl_input = ubl_input['analyticConfig']
        else:
            if (len(sys.argv)<2):
                print line
                print "Please specify path to input json!"
                printUsage()
                exit(1)
            inputfile_json = sys.argv[1];
            if (not os.path.isfile(inputfile_json)):
                print line
                print "Specified input json file does not exist: " + inputfile_json
                exit(1)
            json_file = open(inputfile_json, 'r')
            ubl_input = json.load(json_file,cls=Decoder)
            json_file.close()

        log_instance = logging.getLogger('root')
        
        model_name = ubl_input['modelName']
        task_name = ubl_input['taskName']
        model_run_loc = ubl_input['workDir']
        logFileName = model_name + '.log'
        if ('logFile' in ubl_input.keys()):
            logFileName=ubl_input['logFile']
        logFilePath = os.path.join(model_run_loc,logFileName)
        logging.basicConfig(format='%(message)s',filename=logFilePath,filemode='w',level=logging.INFO)
        #sl = StreamToLogger(log_instance, logging.INFO)
        #sys.stdout = sl
        
        line = '==============================================================================='
    
        log_instance.info(line)
        log_instance.info('Local time: %s'%(time.strftime("%a %b %d %H:%M:%S %Y")))
        log_instance.info('DBN risk assessment %s has started.'%task_name)


        sys.path.append(ubl_input['workDir'])
        
        model_details, pfdbn = RunDBNRisk(ubl_input,log_instance)

        

        model_details_json_filename = 'ModelDetails.json'    
        model_details_json_file = open(os.path.join(model_run_loc,model_details_json_filename), 'w')
        json.dump(model_details, model_details_json_file, sort_keys=True)
        model_details_json_file.close()
        log_instance.info("Successfully wrote %s"%(model_details_json_filename))
        log_instance.info('Local time: %s'%(time.strftime("%a %b %d %H:%M:%S %Y")))
        log_instance.info('DBN risk assessment %s has finished.'%task_name)
        
        #%%%

    except Exception:

        error_message = traceback.format_exc()
        log_instance.error(error_message)
        inputfile_json = sys.argv[1];
        json_file = open(inputfile_json, 'r')
        ubl_input = json.load(json_file)
        json_file.close()
        work_dir = ubl_input['workDir']
        model_details_json_filename = os.path.join(work_dir,'ModelDetails.json') 
        
        
        try:
            if os.path.exists(model_details_json_filename):
                model_details = json.load(open(model_details_json_filename,'rb'))
            elif os.path.exists(os.path.join(work_dir,'ModelDetails-input.json')):
                model_details = json.load(open(os.path.join(work_dir,'ModelDetails-input.json'),'rb'))

            else:
                model_details = {}
            model_details['modelStatus'] = "Failure"
            model_details['statusMessage'] = error_message
            
        except:
            pass
        
        model_details['analyticSettings'] = ubl_input['analyticSettings']
 
        model_details_json_file = open(model_details_json_filename, 'w')
        json.dump(model_details, model_details_json_file, sort_keys=True)
        model_details_json_file.close()

logging.shutdown()