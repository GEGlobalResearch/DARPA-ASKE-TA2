#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
Wrapper module that integrates GE's DBN framework as the execution engine for KaPEESH as part of DARPA ASKE Topic Area 2.
We utilize Python's flask framework to design REST services that can be used through an API.

Written by Natarajan Chennimalai Kumar

General Electric Global Research Center

Mar 27, 2019
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

#from genDBNinput import write_expert_knowledge,write_prior_data,write_observation_data
from gedbn.Preprocessing import writeInputJson
from gedbn.Postprocessing import plotUpdatedBHM
from gedbn.runDBN import run
from gedbn.Postprocessing import postprocessing_PF

from flask import Flask, request, Response, jsonify

#%%%
def RunDBNExecute(aske_input,log_instance):
    """
    Primary DBN execution function. This function sets up the data structures to be used for the DBN execution
    and performs some level of postprocessing.

    :param aske_input: Python dict object containing the DBN execution set up information
    :param log_instance: A logging instance to keep track of the current status
    :return: model_details: Python dict object containing the details and outputs from the DBN execution
            pfdbn: DBN object contains the whole network, prior, posterior samples etc
    """
    work_dir = aske_input['workDir'] 
    if aske_input['analyticSettings']['DBNSetup']['WorkDir']!=work_dir:
        aske_input['analyticSettings']['DBNSetup']['WorkDir'] = work_dir + '/'
        aske_input['analyticSettings']['DBNSetup']['ParticleFilterOptions']['BackendFname'] = work_dir + '/Results/'
    #model_details = copy.deepcopy(aske_input)
    model_details = {}
    model_details['modelStatus'] = ""        

    # Generate input json to DBN
    log_instance.info("Reading user input ...")
    #dbn_input_file = os.path.join(work_dir,aske_input['analyticSettings']['dbnInputFile'])
    #dbn_input_object = writeInputJson.DBN_Input(dbn_input_file)
    dbn_input = aske_input['analyticSettings']#bn_input_object.dbn_input
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

    pfdbn = run(dbn_input, n_steps, number_samples, log_instance)

    if type(pfdbn) is int:
        dbn_model_results = pickle.load(open(os.path.join(work_dir,'Results/dbn_model_latest.pkl'),'r'))
        nodeIndices = [x in dbn_model_results.keys() or x+'_outputs' in dbn_model_results.keys() for x in aske_input['analyticSettings']['Nodes'].keys()]
        results = {}
        for ind, node in enumerate(aske_input['analyticSettings']['Nodes'].keys()):
            results[node] = {}
            if nodeIndices[ind]:
                if node in dbn_model_results.keys():
                    nodename = node
                elif node+'_outputs' in dbn_model_results.keys():
                    nodename = node+'_outputs'

                results[node]['mean'] = np.mean(dbn_model_results[nodename][0])
                results[node]['std'] = np.std(dbn_model_results[nodename][0])

        model_details['results'] = results
    else:
        results = {}
        for step in range(pfdbn.n_steps):
            samples = pfdbn.get_from_store(step)
            sample_mean = samples.mean().to_dict()
            sample_std = samples.std().to_dict()
            for ind, node in enumerate(aske_input['analyticSettings']['Nodes'].keys()):
                if step == 0 and (node in sample_mean.keys() or node+'_outputs' in sample_mean.keys()):
                    results[node] = {}
                    results[node]['mean'] = np.zeros(pfdbn.n_steps)
                    results[node]['std'] = np.zeros(pfdbn.n_steps)
                if node in sample_mean.keys():
                    results[node]['mean'][step] = sample_mean[node]
                    results[node]['std'][step] = sample_std[node]
                elif node+'_outputs' in sample_mean.keys():
                    results[node]['mean'][step] = sample_mean[node+'_outputs']
                    results[node]['std'][step] = sample_std[node+'_outputs']
                else:
                    pass
        for ind, node in enumerate(aske_input['analyticSettings']['Nodes'].keys()):
            if node in sample_mean.keys() or node+'_outputs' in sample_mean.keys():
                results[node]['mean'] = list(results[node]['mean'])
                results[node]['std'] = list(results[node]['std'])
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
def addBatchIndex(aske_input):
    aske_input['analyticSettings']['DBNSetup']['BatchIndexName'] = 'batch_index'
    nodeKeys = aske_input['analyticSettings']['Nodes'].keys()
    
    indicesTimeNodes = [i for i, s in enumerate(nodeKeys) \
                      if aske_input['analyticSettings']['Nodes'][s]['Type'] == 'Stochastic_Transient' and \
                      aske_input['analyticSettings']['Nodes'][s]['Tag'][0].lower()=='operating-data']
    if indicesTimeNodes is not None:
        nSteps = len(aske_input['analyticSettings']['Nodes'][nodeKeys[indicesTimeNodes[0]]]['ObservationData'])
        
        aske_input['analyticSettings']['DBNSetup']['TrackingBatchNumbers'] = nSteps
    
        aske_input['analyticSettings']['Nodes']['batch_index'] = {'Children':[],'Distribution':'','DistributionParameters':{},'Parents':[],'Type':"Constant",'Value':[0L],'Tag':''}
        
        dummyObjList = []
        
        if nSteps>1 and aske_input['taskName']!='predict':
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
            
        aske_input['analyticSettings']['Nodes']['batch_index']['ObservationData'] = dummyObjList
    
    return aske_input
#%%%
def updateNetworkHypothesis(aske_input,hypothesisData):

    for node in aske_input['analyticSettings']['Nodes'].keys():
        if not aske_input['analyticSettings']['Nodes'][node]['Parents'] and 'stochastic' in aske_input['analyticSettings']['Nodes'][node]['Type'].lower():
            # No parents, base nodes
            aske_input['analyticSettings']['Nodes'][node]['ObservationData'] = list(hypothesisData[node].values)
        if aske_input['analyticSettings']['Nodes'][node]['Type'].lower() == 'deterministic':
            if aske_input['analyticSettings']['Nodes'][node]['Children'] is None or not np.array(aske_input['analyticSettings']['Nodes'][node]['Children']).size:
                aske_input['analyticSettings']['Nodes'][node]['actuals'] = list(hypothesisData[node].values)

                aske_input['analyticSettings']['DBNSetup']['TrackingTimeSteps'] = len(hypothesisData[node].values)
    return aske_input

def postProcessHypothesis(model_details,aske_input):
    """
    This function takes the data after the DBN execution to analyze the hypothesis that the model can predict data

    Arguments:
    :param model_details: Python dict object containing current update of details
    :param aske_input: original ubl input dict object
    :return: model_details: updated model details and input json with outputs from the hypothesis testing including error in prediction of the output nodes and aggregated error measures for model comparison
    """
    model_details['results']['modelAccuracy'] = {}
    for node in aske_input['analyticSettings']['Nodes'].keys():
        if aske_input['analyticSettings']['Nodes'][node]['Type'].lower() == 'deterministic' and \
                (aske_input['analyticSettings']['Nodes'][node]['Children'] is None or not np.array(aske_input['analyticSettings']['Nodes'][node]['Children']).size):
            model_details['results'][node]['error'] = list(np.array(model_details['results'][node]['mean']) - np.array(aske_input['analyticSettings']['Nodes'][node]['actuals']))

            model_details['results']['modelAccuracy']['meanError'] = np.mean(model_details['results'][node]['error'])
            model_details['results']['modelAccuracy']['maxAbsError'] = np.max(np.abs(model_details['results'][node]['error']))

    return model_details, aske_input
#%%%%
def runDBN(aske_input,log_instance):
    """
    Initializes logging and runs the DBN execution. In case of hypothesis generation usecase,
    sets up the hypothesis and postprocessing.

    :param aske_input: Python dict object containing the DBN execution set up information
    :param log_instance: A logging instance to keep track of the current status
    :return: model_details: Python dict object containing the details and outputs from the DBN execution
            pfdbn: DBN object contains the whole network, prior, posterior samples etc
    """
    model_name = aske_input['modelName']
    task_name = aske_input['taskName']
    model_run_loc = aske_input['workDir']
    logFileName = model_name + '.log'
    if ('logFile' in aske_input.keys()):
        logFileName=aske_input['logFile']
    logFilePath = os.path.join(model_run_loc,logFileName)
    logging.basicConfig(format='%(message)s',filename=logFilePath,filemode='w',level=logging.INFO)

    line = '==============================================================================='

    log_instance.info(line)
    log_instance.info('Local time: %s'%(time.strftime("%a %b %d %H:%M:%S %Y")))
    log_instance.info('DBN risk assessment %s has started.'%task_name)


    sys.path.append(aske_input['workDir'])

    # Check if we are dealing with hypothesis generation
    hypothesisFlag = False
    if 'ObservationData' in aske_input['analyticSettings'] and aske_input['analyticSettings']['ObservationData'] is not None:
        # check if the file exists
        if 'File' in aske_input['analyticSettings']['ObservationData']:
            if os.path.isfile(aske_input['analyticSettings']['ObservationData']['File']):
                hypothesisData = pd.read_csv(aske_input['analyticSettings']['ObservationData']['File'])
                aske_input = updateNetworkHypothesis(aske_input, hypothesisData)
                hypothesisFlag = True
        elif 'Data' in aske_input['analyticSettings']['ObservationData']:
            hypothesisData = pd.DataFrame(aske_input['analyticSettings']['ObservationData']['Data'])
            aske_input = updateNetworkHypothesis(aske_input, hypothesisData)
            hypothesisFlag = True
        elif type(aske_input['analyticSettings']['ObservationData']) == 'dict':
            hypothesisData = pd.DataFrame(aske_input['analyticSettings']['ObservationData'])
            aske_input = updateNetworkHypothesis(aske_input, hypothesisData)
            hypothesisFlag = True

    
    print 'inside runDBN'
    
    model_details, pfdbn = RunDBNExecute(aske_input,log_instance)
    
    print 'Ran RunDBNExecute'

    if hypothesisFlag:
        model_details, aske_input = postProcessHypothesis(model_details,aske_input)
    
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
    """

    This function runs the DBN execution as a flask REST service that will called by the KApEESH execution manager.
    It sets up logging service, takes the POST request's json as the input to the DBN execution.

    :return: model_details as the response json
    """
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
        aske_input = json.loads(in_json,cls=Decoder)

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
        model_details, pfdbn = runDBN(aske_input,log_instance)
        #print output_dict
        # Return data in json format
        print 'Completed dbn run, extracted info'
        output_json = json.dumps(model_details)
        task_name = aske_input['taskName']
        print output_json
        output_response = Response(output_json, status = 200,
                                   mimetype = 'application/json')
        
        #log_instance.info("Successfully wrote %s"%(model_details_json_filename))
        log_instance.info('Local time: %s'%(time.strftime("%a %b %d %H:%M:%S %Y")))
        log_instance.info('DBN execution %s has finished.'%task_name)
        logging.shutdown()
        return output_response
    except Exception:
        error_message = traceback.format_exc()
        log_instance.error(error_message)
        work_dir = aske_input['workDir']
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

#%%%
if __name__=='__main__':
    
    try:
        line = '==============================================================================='
        
        # Adding capability to read json string from stdin
        if sys.argv[1][0] == '{':
            aske_input = json.loads(sys.argv[1])
            aske_input = aske_input['analyticConfig']
        else:
            if (len(sys.argv)<2):
                print line
                print "Please specify path to input json!"
                printUsage()
                exit(1)
            inputfile_json = sys.argv[1]
            if (not os.path.isfile(inputfile_json)):
                print line
                print "Specified input json file does not exist: " + inputfile_json
                exit(1)
            json_file = open(inputfile_json, 'r')
            aske_input = json.load(json_file,cls=Decoder)
            json_file.close()

        log_instance = logging.getLogger('root')
        
        model_name = aske_input['modelName']
        task_name = aske_input['taskName']
        model_run_loc = aske_input['workDir']
        logFileName = model_name + '.log'
        if ('logFile' in aske_input.keys()):
            logFileName=aske_input['logFile']
        logFilePath = os.path.join(model_run_loc,logFileName)
        logging.basicConfig(format='%(message)s',filename=logFilePath,filemode='w',level=logging.INFO)
        
        line = '==============================================================================='
    
        log_instance.info(line)
        log_instance.info('Local time: %s'%(time.strftime("%a %b %d %H:%M:%S %Y")))
        log_instance.info('DBN risk assessment %s has started.'%task_name)


        sys.path.append(aske_input['workDir'])
        
        #model_details, pfdbn = RunDBNExecute(aske_input,log_instance)

        model_details, pfdbn = runDBN(aske_input, log_instance)

        model_details_json_filename = 'ModelDetails.json'    
        model_details_json_file = open(os.path.join(model_run_loc,model_details_json_filename), 'w')
        json.dump(model_details, model_details_json_file, sort_keys=True)
        model_details_json_file.close()
        log_instance.info("Successfully wrote %s"%(model_details_json_filename))
        log_instance.info('Local time: %s'%(time.strftime("%a %b %d %H:%M:%S %Y")))
        log_instance.info('DBN execution %s has finished.'%task_name)
        
        #%%%

    except Exception:
        log_instance = logging.getLogger('root')
        error_message = traceback.format_exc()
        log_instance.error(error_message)
        inputfile_json = sys.argv[1]
        json_file = open(inputfile_json, 'r')
        aske_input = json.load(json_file)
        json_file.close()
        work_dir = aske_input['workDir']
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
        
        model_details['analyticSettings'] = aske_input['analyticSettings']
 
        model_details_json_file = open(model_details_json_filename, 'w')
        json.dump(model_details, model_details_json_file, sort_keys=True)
        model_details_json_file.close()

logging.shutdown()